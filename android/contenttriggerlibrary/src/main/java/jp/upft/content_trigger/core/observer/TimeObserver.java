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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.location_observer.LocationObserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class TimeObserver extends LocationObserver {

    private static final String PREFERENCES_NAME = "jp.upft.content_trigger.core.observer.TimeObserver";
    private static final String KEY_DETECTED_IDS = "mDetectedIds";

    private boolean mIsRunning = true;
    private ArrayList<String> mDetectedIds = new ArrayList<>();

    private PersistenceManager mPersistenceManager;

    /**
     * コンストラクタ
     *
     * @param context
     * @param callbackIntent コールバック時に発行される Intent
     * @param intentType コールバック時に発行される Intent の 種別
     * @see IntentType
     */
    public TimeObserver(Context context, Intent callbackIntent, IntentType intentType) {
        super(context, callbackIntent, intentType, null);

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
    protected void start() {
        mIsRunning = true;
    }

    @Override
    protected void stop() {
        mIsRunning = false;
    }

    @Override
    protected void doTaskImpl() {

    }

    @Override
    protected void onFinishAddEntries() {
        super.onFinishAddEntries();

        workInEntries(new WorkInEntriesTask() {
            @Override
            public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                for (String entryId : entriesRef.keySet()) {
                    if (!mDetectedIds.contains(entryId)) {
                        mDetectedIds.add(entryId);
                        if (mIsRunning) {
                            performIntent(entryId, Action.None);
                        }
                    }
                }

                mPersistenceManager.saveMemoryToPreferences();
            }
        });
    }

    @Override
    protected void onFinishRemoveEntries(final Collection<String> entryIds) {
        super.onFinishRemoveEntries(entryIds);

        workInEntries(new WorkInEntriesTask() {
            @Override
            public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                for (String entryId : entryIds) {
                    mDetectedIds.remove(entryId);
                }

                mPersistenceManager.saveMemoryToPreferences();
            }
        });
    }

    private class PersistenceManager extends LocationObserver.PersistenceManager{
        public PersistenceManager(SharedPreferences sharedPreferences) {
            super(sharedPreferences);
        }

        @Override
        protected void saveMemoryToPreferences(){
            SharedPreferences.Editor editor = getSharedPreferences().edit();

            Gson gson = new Gson();

            String pastInAccessJson = gson.toJson(mDetectedIds);

            editor.putString(KEY_DETECTED_IDS, pastInAccessJson);
            editor.apply();
        }

        @Override
        protected void loadMemoryFromPreferences(){
            Gson gson = new Gson();

            String pastInAccessJson = getSharedPreferences().getString(KEY_DETECTED_IDS, null);
            if (pastInAccessJson != null){
                mDetectedIds = gson.fromJson(pastInAccessJson, new TypeToken<ArrayList<String>>(){}.getType());
            }
        }
    }
}
