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


package jp.upft.content_trigger.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.content_trigger.ContentTriggerEntry;
import jp.upft.content_trigger.core.observer.QRObserver;
import jp.upft.content_trigger.core.observer.TimeObserver;
import jp.upft.location_observer.LocationObserver;
import jp.upft.location_observer.access_point_observer.AccessPointObserver;
import jp.upft.location_observer.ble_observer.BLEObserver;
import jp.upft.location_observer.geofence_observer.GeofenceObserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Observer と ContentTriggerLibrary の Adapter Created by ryoga.kitagawa on
 * 2015/02/18.
 */

final class ContentTriggerObserver {
    private static final String ENABLED_PREFERENCE_NAME = "jp.upft.content_trigger.core.ContentTriggerService#enabled";

    private final Context mContext;
    private final HashMap<String, LocationObserver> mObservers = new HashMap<>();
    private final QRObserver mQRObserverRef;

    /**
     * コンストラクタ
     *
     * @param context
     * @param serviceClass コールバックを受けたい ContentTriggerService の型。
     */
    public ContentTriggerObserver(Context context,
            Class<? extends ContentTriggerService> serviceClass) {
        mContext = context;

        Intent intent = new Intent(context, serviceClass);

        mObservers.put(ContentTriggerEntry.TRIGGER_TYPE_IBEACON,
                BLEObserver.newInstance(context, intent,
                        LocationObserver.IntentType.Service));
        mObservers.put(ContentTriggerEntry.TRIGGER_TYPE_GEOFENCE, new GeofenceObserver(context,
                intent,
                LocationObserver.IntentType.Service));
        mObservers.put(ContentTriggerEntry.TRIGGER_TYPE_ACCESSPOINT, new AccessPointObserver(
                context, intent,
                LocationObserver.IntentType.Service));
        mQRObserverRef = new QRObserver(context, intent,
                LocationObserver.IntentType.Service);
        mObservers.put(ContentTriggerEntry.TRIGGER_TYPE_QR, mQRObserverRef);
        mObservers.put(ContentTriggerEntry.TRIGGER_TYPE_TIME, new TimeObserver(context, intent,
                LocationObserver.IntentType.Service));

        loadTriggerEnabled(context);
    }

    public void queryQRString(String qrStr) {
        mQRObserverRef.queryQrString(qrStr);
    }

    public void setEnabled(String type, boolean enabled) {
        LocationObserver observer = mObservers.get(type);
        if (observer != null) {
            saveTriggerEnabled(mContext, type, enabled);
            observer.setEnabled(enabled);
        }
    }

    public void updateTriggers(List<ContentTriggerEntry> entries) {
        removeAllTriggersAndWaitStop();

        addTriggers(entries);
    }

    /**
     * トリガを追加する。
     *
     * @param entries
     */
    public void addTriggers(List<ContentTriggerEntry> entries) {
        List<HashMap<String, String>> observeEntries = new ArrayList<>();
        for (ContentTriggerEntry object : entries) {
            observeEntries.add(object.getEntry());
        }

        HashMap<String, Collection<ConcurrentHashMap<String, String>>> siftedEntries = siftEntries(observeEntries);

        for (String key : siftedEntries.keySet()) {
            mObservers.get(key).addAllEntries(siftedEntries.get(key));
        }
    }

    private HashMap<String, Collection<ConcurrentHashMap<String, String>>> siftEntries(
            Collection<HashMap<String, String>> observeEntries) {
        HashMap<String, Collection<ConcurrentHashMap<String, String>>> retTypedEntries = new HashMap<>();
        for (HashMap<String, String> entry : observeEntries) {
            String triggerType = entry.get(ContentTriggerEntry.KEY_TRIGGER_TYPE);
            Collection<ConcurrentHashMap<String, String>> entries = retTypedEntries.get(triggerType);
            if (entries == null) {
                entries = new ArrayList<>();
                retTypedEntries.put(triggerType, entries);
            }
            entries.add(new ConcurrentHashMap<>(entry));
        }
        return retTypedEntries;
    }

    /**
     * トリガを削除する。
     *
     * @param entryIds 削除するトリガ ID のリスト。
     */
    public void removeTriggers(List<String> entryIds) {
        for (LocationObserver observer : mObservers.values()) {
            observer.removeEntries(entryIds);
        }
    }

    /**
     * 全てのトリガを削除する。
     */
    public void removeAllTriggersAndWaitStop() {
        for (LocationObserver observer : mObservers.values()) {
            observer.removeAllEntries();
        }
    }

    /**
     * コールバック時に受け取る Intent から検出結果情報を生成する。
     *
     * @param callbackIntent コールバック時に受け取った Intent
     * @return コールバックのきっかけとなったトリガ情報
     */
    public List<LocationObserver.ObserveResult> getObserveResult(Intent callbackIntent) {
        List<LocationObserver.ObserveResult> results = new ArrayList<>();
        for (LocationObserver observer : mObservers.values()) {
            results.addAll(observer.getResults(callbackIntent));
        }
        return results;
    }

    // region persistence

    private void saveTriggerEnabled(Context context, String triggerType, boolean enabled) {
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mode = Context.MODE_MULTI_PROCESS;
        } else {
            mode = Context.MODE_PRIVATE;
        }

        context.getSharedPreferences(ENABLED_PREFERENCE_NAME, mode).edit()
                .putBoolean(triggerType, enabled).commit();
    }

    private void loadTriggerEnabled(Context context) {
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mode = Context.MODE_MULTI_PROCESS;
        } else {
            mode = Context.MODE_PRIVATE;
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(ENABLED_PREFERENCE_NAME,
                mode);
        Map<String, ?> all = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                LocationObserver observer = mObservers.get(entry.getKey());
                if (observer != null) {
                    observer.setEnabled((Boolean) entry.getValue());
                }
            }
        }
    }
    // endregion
}
