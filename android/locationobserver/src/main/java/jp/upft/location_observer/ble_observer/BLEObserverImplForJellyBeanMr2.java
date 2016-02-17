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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
public final class BLEObserverImplForJellyBeanMr2 extends BLEObserver {

    private static final String PREFERENCES_NAME = "jp.upft.location_observer.ble_observer.BLEObserverImplForJellyBeanMr2";
    private static final String KEY_BLE_STATES = "bleStates";

    private Map<String, Long> mBLEStates = new HashMap<>();
    private BluetoothManager mBluetoothManager;
    private PersistenceManager mPersistenceManager;
    private Timer mTimer;
    private long mScanInterval = 2500;
    private long mExitSpendTime = 30000;

    protected BLEObserverImplForJellyBeanMr2(Context context, Intent performIntent,
            IntentType intentType, TaskListener taskListener) {
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
        mTimer.schedule(new ScanTask(), 0, mScanInterval);
    }

    @Override
    protected void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    protected void doTaskImpl() {
        final BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if (adapter != null) {
            boolean started = adapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    adapter.stopLeScan(this);

                    workInEntries(new WorkInEntriesTask() {
                        @Override
                        public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                            if (scanRecord != null) {
                                ByteBuffer byteBuffer = ByteBuffer.wrap(scanRecord, 5, 24);

                                int head = byteBuffer.getInt();

                                // Log.d("BLEObserver",
                                // "onLeScanInner:"+String.format("0x%08x", head));
                                if (head == 0x4c000215) {

                                    // result bytes to structure.
                                    byte[] uuidBytes = new byte[16];
                                    byteBuffer.get(uuidBytes);
                                    StringBuilder hexUUIDBuilder = new StringBuilder();
                                    for (int b : uuidBytes) {
                                        hexUUIDBuilder.append(Character.forDigit(b >> 4 & 0xf, 16));
                                        hexUUIDBuilder.append(Character.forDigit(b & 0xf, 16));
                                    }
                                    String resultUUID = hexUUIDBuilder.toString().toUpperCase();
                                    Integer resultMajorId = byteBuffer.getShort() & 0xffff;
                                    Integer resultMinorId = byteBuffer.getShort() & 0xffff;

                                    // Log.d("BLEObserver", "MapScan");

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

                                        if (rssiLevel <= rssi) {
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
                                                                Action.Enter);
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

                                if (currentTimeMillis > pastTime + mExitSpendTime) {
                                    i.remove();
                                    ConcurrentHashMap<String, String> entry = entriesRef.get(key);
                                    if (entry != null) {
                                        boolean exit = Boolean.parseBoolean(entry.get(
                                                LocationObserver.KEY_TRANSITION_EXIT));
                                        if (exit) {
                                            performIntent(key, Action.Exit);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            });

            if (!started) {
                mBluetoothManager = (BluetoothManager) getContext().getSystemService(
                        Context.BLUETOOTH_SERVICE);
            }

            mPersistenceManager.saveMemoryToPreferences();
        }
    }

    /**
     * BLEの監視処理 (前方サポート)
     */
    @Deprecated
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
