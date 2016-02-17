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

import jp.upft.location_observer.LocationObserver;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public abstract class BLEObserver extends LocationObserver {

    /**
     * ハッシュマップに格納される peripheralUUID のキー
     */
    public static final String KEY_UUID = "peripheralUUID";
    /**
     * ハッシュマップに格納される major のキー
     */
    public static final String KEY_MAJOR_ID = "major";
    /**
     * ハッシュマップに格納される minor のキー
     */
    public static final String KEY_MINOR_ID = "minor";
    /**
     * ハッシュマップに格納される rssi のキー
     */
    public static final String KEY_RSSI = "RSSI";

    protected BLEObserver(Context context, Intent callbackIntent, IntentType intentType,
            TaskListener taskListener) {
        super(context, callbackIntent, intentType, taskListener);
    }

    public static BLEObserver newInstance(Context context, Intent callbackIntent,
            IntentType intentType) {
        int ver = Build.VERSION.SDK_INT;
        if (BluetoothAdapter.getDefaultAdapter() == null){
            return new BLEObserverDummy(context, callbackIntent, intentType, null);
        } else if (ver >= Build.VERSION_CODES.LOLLIPOP) {
            return new BLEObserverImpl(context, callbackIntent, intentType, null);
        } else if (ver >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new BLEObserverImplForJellyBeanMr2(context, callbackIntent, intentType,
                    null);
        } else {
            return new BLEObserverDummy(context, callbackIntent, intentType, null);
        }
    }

    @Override
    protected void start() {

    }

    @Override
    protected void stop() {

    }
}
