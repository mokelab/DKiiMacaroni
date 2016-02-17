/* Copyright 2016 Kii Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package jp.upft.location_observer.access_point_observer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.location_observer.LocationObserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * AP の監視クラス Created by ryoga.kitagawa on 2015/01/20.
 */
public final class AccessPointObserver extends LocationObserver {

    private static ArrayList<AccessPointObserver> sAccessPointObservers = new ArrayList<>();

    public static final String KEY_SSID = "SSID";
    public static final String KEY_BSSID = "BSSID";
    public static final String KEY_RSSI = "RSSI";

    private static final String PREFERENCES_NAME = "jp.upft.location_observer.access_point_observer.AccessPointObserver";
    private static final String KEY_PAST_IN_ACCESSES = "PastInAccesses";

    private Set<String> mPastInAccessPoints = new HashSet<>();
    private Set<String> mNowInAccessPoints = new HashSet<>();
    private final WifiManager mWifiManager;
    private Timer mTimer;
    private InterruptibleTimerTask mTimerTask;
    private long mScanInterval = 10000;

    private final List<ScanResult> mScannedAccessPointList = new ArrayList<>();

    private PersistenceManager mPersistenceManager;

    public static void postScanResultAvailableAction(){
        for (AccessPointObserver observer : sAccessPointObservers){
            observer.onScanResultAvailable();
        }
    }

    /**
     * @see jp.upft.location_observer.LocationObserver#LocationObserver(Context, Intent, IntentType, TaskListener)
     */
    public AccessPointObserver(Context context, Intent performIntent, IntentType intentType) {
        super(context, performIntent, intentType, null);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mode = Context.MODE_MULTI_PROCESS;
        } else {
            mode = Context.MODE_PRIVATE;
        }
        mPersistenceManager = new PersistenceManager(context.getSharedPreferences(PREFERENCES_NAME, mode));
        mPersistenceManager.loadMemoryFromPreferences();
    }

    /**
     * @see jp.upft.location_observer.LocationObserver#getResults(android.content.Intent)
     */
    @Override
    @NonNull
    public List<ObserveResult> getResults(Intent callbackIntent) {
        String entryId = callbackIntent.getStringExtra(LocationObserver.INTENT_EXTRA_ENTRY_ID);
        if (entryId == null){
            return Collections.emptyList();
        }

        ConcurrentHashMap<String, String> entry = getEntries().get(entryId);
        if (entry == null) {
            return Collections.emptyList();
        }

        Action action = (Action) callbackIntent
                .getSerializableExtra(LocationObserver.INTENT_EXTRA_LOCATION_ACTION);

        ObserveResult result = new ObserveResult(entry, action);

        return Collections.singletonList(result);
    }

    public void setScanInterval(long milliseconds) {
        mScanInterval = milliseconds;
    }

    @Override
    protected void onStartAddEntries() {

    }

    @Override
    protected void onFinishAddEntries() {
        if (getEnabled()) {
            start();
        }
    }

    @Override
    protected void onStartRemoveEntries() {

    }

    @Override
    protected void onFinishRemoveEntries(Collection<String> entryIds) {
        if (getEntries().isEmpty()) {
            stop();
        }
    }

    @Override
    protected void start() {
        if (mTimer != null) {
            return;
        }

        mTimerTask = new InterruptibleTimerTask() {
            @Override
            public void run() {
                currentThread = Thread.currentThread();
                doTask();
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, 0, mScanInterval);
    }

    @Override
    protected void stop() {
        if (mTimerTask != null){
            mTimerTask.interrupt();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    protected void doTaskImpl() {
        // AP検出
        mNowInAccessPoints.clear();

        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            register(this);
            mWifiManager.startScan();
            try {
                synchronized (mScannedAccessPointList) {
                    mScannedAccessPointList.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } finally {
                unregister(this);
            }

            workInEntries(new WorkInEntriesTask() {
                @Override
                public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                    for (ScanResult result : mScannedAccessPointList) {
                        for (ConcurrentHashMap<String, String> entry : getEntries().values()) {
                            String ssid = entry.get(KEY_SSID);
                            String bssid = entry.get(KEY_BSSID);
                            String rssiStr = entry.get(KEY_RSSI);
                            Integer rssiLevel = null;
                            if (rssiStr != null) {
                                rssiLevel = Integer.parseInt(entry.get(KEY_RSSI));
                            }

                            if (rssiLevel == null || rssiLevel <= result.level) {
                                int wrongCount = 0;
                                if (!result.SSID.equals(ssid)) {
                                    ++wrongCount;
                                }
                                if (bssid != null && bssid.length() > 0) {
                                    if (!result.BSSID.equals(bssid)) {
                                        ++wrongCount;
                                    }
                                }

                                if (wrongCount == 0
                                        && !mNowInAccessPoints.contains(entry.get(KEY_ENTRY_ID))) {
                                    mNowInAccessPoints.add(entry.get(KEY_ENTRY_ID));
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }

        for (String nowInEntryId : mNowInAccessPoints) {
            if (!mPastInAccessPoints.contains(nowInEntryId)) {
                if (Boolean.parseBoolean(getEntries().get(nowInEntryId).get(LocationObserver.KEY_TRANSITION_ENTER))) {
                    performIntent(nowInEntryId, Action.Enter);
                }
            }
        }
        for (String pastInEntryId : mPastInAccessPoints) {
            if (!mNowInAccessPoints.contains(pastInEntryId)) {
                if (Boolean.parseBoolean(getEntries().get(pastInEntryId).get(LocationObserver.KEY_TRANSITION_EXIT))) {
                    performIntent(pastInEntryId, Action.Exit);
                }
            }
        }

        Set<String> temp = mPastInAccessPoints;
        mPastInAccessPoints = mNowInAccessPoints;
        mNowInAccessPoints = temp;

        mPersistenceManager.saveMemoryToPreferences();
    }

    public void onScanResultAvailable(){
        synchronized (mScannedAccessPointList) {
            mScannedAccessPointList.clear();
            mScannedAccessPointList.addAll(mWifiManager.getScanResults());
            mScannedAccessPointList.notifyAll();
        }
    }

    private static synchronized void register(AccessPointObserver observer){
        if (!sAccessPointObservers.contains(observer)){
            sAccessPointObservers.add(observer);
        }
    }

    private static synchronized void unregister(AccessPointObserver observer){
        sAccessPointObservers.remove(observer);
    }

    private abstract static class InterruptibleTimerTask extends TimerTask{

        public Thread currentThread;

        public void interrupt(){
            if (currentThread != null){
                currentThread.interrupt();
            }
        }
    }

    private class PersistenceManager extends LocationObserver.PersistenceManager{
        public PersistenceManager(SharedPreferences sharedPreferences) {
            super(sharedPreferences);
        }

        @Override
        protected void saveMemoryToPreferences(){
            SharedPreferences.Editor editor = getSharedPreferences().edit();

            Gson gson = new Gson();

            String pastInAccessJson = gson.toJson(mPastInAccessPoints);

            editor.putString(KEY_PAST_IN_ACCESSES, pastInAccessJson);
            editor.apply();
        }

        @Override
        protected void loadMemoryFromPreferences(){
            Gson gson = new Gson();

            String pastInAccessJson = getSharedPreferences().getString(KEY_PAST_IN_ACCESSES, null);
            if (pastInAccessJson != null){
                mPastInAccessPoints = gson.fromJson(pastInAccessJson, new TypeToken<HashSet<String>>(){}.getType());
            }
        }
    }
}
