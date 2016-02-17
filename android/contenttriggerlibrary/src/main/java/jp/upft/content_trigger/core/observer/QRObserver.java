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


package jp.upft.content_trigger.core.observer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.content_trigger.ContentTriggerEntry;
import jp.upft.location_observer.LocationObserver;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

public final class QRObserver extends LocationObserver {

    private boolean mIsRunning = true;

    /**
     * コンストラクタ
     *
     * @param context
     * @param callbackIntent コールバック時に発行される Intent
     * @param intentType コールバック時に発行される Intent の 種別
     * @see IntentType
     */
    public QRObserver(Context context, Intent callbackIntent, IntentType intentType) {
        super(context, callbackIntent, intentType, null);
    }

    @NonNull
    @Override
    public List<ObserveResult> getResults(Intent callbackIntent) {
        if (getEnabled()) {
            String entryId = callbackIntent.getStringExtra(INTENT_EXTRA_ENTRY_ID);
            if (entryId == null){
                return Collections.emptyList();
            }

            ConcurrentHashMap<String, String> entry = getEntries().get(entryId);
            if (entry != null) {
                return Collections.singletonList(new ObserveResult(entry, Action.None));
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected void start() {
        mIsRunning = true;
    }

    @Override
    protected void stop() {
        mIsRunning = false;
    }

    @Override
    protected void doTaskImpl() {
        throw new UnsupportedOperationException();
    }

    public void queryQrString(final String qrStr) {
        if (mIsRunning) {
            workInEntries(new WorkInEntriesTask() {
                @Override
                public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                    for (final Map<String, String> entry : entriesRef.values()) {
                        if (entry.get(ContentTriggerEntry.KEY_QR_TARGET).equals(qrStr)) {
                            String entryId = entry.get(KEY_ENTRY_ID);
                            performIntent(entryId, Action.None);
                        }
                    }
                }
            });
        }
    }
}
