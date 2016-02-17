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


package jp.upft.location_observer.ble_observer;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.location_observer.LocationObserver;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * BLE(iBeacon) の監視クラス Created by ryoga.kitagawa on 2015/01/20.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public final class BLEObserverImpl extends BLEObserver {

    private static final String PREFERENCES_NAME = "jp.upft.location_observer.ble_observer.BLEObserverImpl";
    private static final String KEY_BLE_STATES = "bleStates";

    private Map<String, Long> mBLEStates = new HashMap<>();
    private final BluetoothManager mBluetoothManager;
    private PersistenceManager mPersistenceManager;
    private Timer mTimer;
    private static final long SCAN_INTERVAL = 2500;
    private static final long EXIT_SPEND_TIME = 30000;

    protected BLEObserverImpl(Context context, Intent performIntent,
            LocationObserver.IntentType intentType, TaskListener taskListener) {
        super(context, performIntent, intentType, taskListener);
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mode = Context.MODE_MULTI_PROCESS;
        } else {
            mode = Context.MODE_PRIVATE;
        }
        mPersistenceManager = new PersistenceManager(context.getSharedPreferences(PREFERENCES_NAME, mode));
        mPersistenceManager.loadMemoryFromPreferences();
    }

    @Override
    protected void onFinishAddEntries() {
        if (getEnabled()) {
            start();
        }
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

        mTimer = new Timer();
        mTimer.schedule(new ScanTask(), 0, SCAN_INTERVAL);
    }

    @Override
    protected void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void doTaskImpl() {
        final BluetoothLeScanner scanner = mBluetoothManager.getAdapter().getBluetoothLeScanner();
        if (scanner != null) {
            scanner.startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    super.onScanResult(callbackType, result);
                    scanner.stopScan(this);
                    final ScanRecord record = result.getScanRecord();

                    workInEntries(new WorkInEntriesTask() {
                        @Override
                        public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                            if (record != null) {
                                byte[] bytes = record.getBytes();
                                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 5, 24);
                                if (byteBuffer.getInt() == 0x4c000215) {

                                    // result bytes to structure.
                                    byte[] uuidBytes = new byte[16];
                                    byteBuffer.get(uuidBytes);
                                    StringBuilder hexUUIDBuilder = new StringBuilder();
                                    for (int b : uuidBytes) {
                                        hexUUIDBuilder.append(Character.forDigit(b >> 4 & 0xf, 16));
                                        hexUUIDBuilder.append(Character.forDigit(b & 0xf, 16));
                                    }
                                    final String resultUUID = hexUUIDBuilder.toString().toUpperCase();
                                    final Integer resultMajorId = byteBuffer.getShort() & 0xffff;
                                    final Integer resultMinorId = byteBuffer.getShort() & 0xffff;

                                    for (ConcurrentHashMap<String, String> entry : entriesRef.values()) {
                                        String uuid = entry.get(KEY_UUID);
                                        Integer majorId = null;
                                        Integer minorId = null;
                                        Integer rssiLevel = null;

                                        try {
                                            majorId = Integer.valueOf(entry.get(KEY_MAJOR_ID));
                                        } catch (Exception e) {
                                            // e.printStackTrace();
                                        }
                                        try {
                                            minorId = Integer.valueOf(entry.get(KEY_MINOR_ID));
                                        } catch (Exception e) {
                                            // e.printStackTrace();
                                        }
                                        try {
                                            rssiLevel = Integer.valueOf(entry.get(KEY_RSSI));
                                        } catch (Exception e) {
                                            // e.printStackTrace();
                                        }

                                        if (rssiLevel == null) {
                                            rssiLevel = Integer.MIN_VALUE;
                                        }

                                        if (rssiLevel <= result.getRssi()) {
                                            int wrongCount = 0;

                                            if (uuid != null) {
                                                // uuid
                                                if (!resultUUID.equals(uuid)) {
                                                    ++wrongCount;
                                                }
                                            }

                                            if (majorId != null) {
                                                // major
                                                if (!resultMajorId.equals(majorId)) {
                                                    ++wrongCount;
                                                }
                                            }
                                            if (minorId != null) {
                                                // minor
                                                if (!resultMinorId.equals(minorId)) {
                                                    ++wrongCount;
                                                }
                                            }

                                            if (wrongCount == 0) {
                                                if (!mBLEStates.containsKey(entry
                                                        .get(LocationObserver.KEY_ENTRY_ID))) {
                                                    boolean enter = Boolean.parseBoolean(entry
                                                            .get(LocationObserver.KEY_TRANSITION_ENTER));
                                                    if (enter) {
                                                        performIntent(
                                                                entry.get(LocationObserver.KEY_ENTRY_ID),
                                                                LocationObserver.Action.Enter);
                                                    }
                                                }
                                                mBLEStates.put(entry.get(LocationObserver.KEY_ENTRY_ID),
                                                        System.currentTimeMillis());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            long currentTimeMillis = System.currentTimeMillis();
                            for (Iterator<Map.Entry<String, Long>> i = mBLEStates.entrySet().iterator(); i.hasNext();) {

                                Map.Entry<String, Long> statesEntry = i.next();
                                String key = statesEntry.getKey();
                                Long pastTime = statesEntry.getValue();

                                if (currentTimeMillis > pastTime + EXIT_SPEND_TIME) {
                                    i.remove();
                                    ConcurrentHashMap<String, String> entry = getEntries().get(key);
                                    if (entry != null) {
                                        boolean exit = Boolean.parseBoolean(entry.get(
                                                LocationObserver.KEY_TRANSITION_EXIT));
                                        if (exit) {
                                            performIntent(key, LocationObserver.Action.Exit);
                                        }
                                    }
                                }
                            }

                            mPersistenceManager.saveMemoryToPreferences();
                        }
                    });
                }
            });
        }
    }

    /**
     * BLEの監視クラス
     */
    private class ScanTask extends TimerTask {
        @Override
        public void run() {
            doTask();
        }
    }

    private class PersistenceManager extends LocationObserver.PersistenceManager {

        public PersistenceManager(SharedPreferences sharedPreferences) {
            super(sharedPreferences);
        }

        @Override
        protected void saveMemoryToPreferences() {
            SharedPreferences.Editor editor = getSharedPreferences().edit();

            Gson gson = new Gson();

            String bleStatesJson = gson.toJson(mBLEStates, new TypeToken<HashMap<String, Long>>(){}.getType());

            editor.putString(KEY_BLE_STATES, bleStatesJson);
            editor.apply();
        }

        @Override
        protected void loadMemoryFromPreferences() {
            Gson gson = new Gson();

            String bleStateJson = getSharedPreferences().getString(KEY_BLE_STATES, null);
            if (bleStateJson != null) {
                mBLEStates = gson.fromJson(bleStateJson, new TypeToken<HashMap<String, Long>>(){}.getType());
            }
        }
    }
}
