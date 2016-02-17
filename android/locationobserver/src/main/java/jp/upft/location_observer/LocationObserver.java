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


package jp.upft.location_observer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * abstract 監視クラス Created by ryoga.kitagawa on 2015/01/20.
 */
public abstract class LocationObserver {

    public interface TaskListener {
        void OnStart();
        void OnEnd();
    }

    /**
     * ハッシュマップに格納される entryId のキー
     */
    public static final String KEY_ENTRY_ID = "entry_id";
    /**
     * ハッシュマップに格納される transitionEnter のキー
     */
    public static final String KEY_TRANSITION_ENTER = "transitionEnter";
    /**
     * ハッシュマップに格納される transitionExit のキー
     */
    public static final String KEY_TRANSITION_EXIT = "transitionExit";

    /**
     * コールバックの Intent に付加される entryId のキー
     */
    protected static final String INTENT_EXTRA_ENTRY_ID = "intent_extra:entry_id";

    /**
     * コールバックの Intent に付加される Action のキー
     *
     * @see jp.upft.location_observer.LocationObserver.Action
     */
    protected static final String INTENT_EXTRA_LOCATION_ACTION = "intent_extra:location_action";
    private Context mContext;
    private Intent mCallbackIntent;
    private IntentType mIntentType;
    protected TaskListener mTaskListener;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> mEntries = new ConcurrentHashMap<>();
    private boolean mEnabled = true;

    /**
     * コンストラクタ
     *
     * @param context
     * @param callbackIntent コールバック時に発行される Intent
     * @param intentType コールバック時に発行される Intent の 種別
     * @see jp.upft.location_observer.LocationObserver.IntentType
     */
    public LocationObserver(Context context, Intent callbackIntent, IntentType intentType,
            TaskListener taskListener) {
        mContext = context;
        mCallbackIntent = callbackIntent;
        mIntentType = intentType;
        mTaskListener = taskListener;
    }

    public final Context getContext() {
        return mContext;
    }

    /**
     * コールバック時に受け取る Intent から検出結果情報を生成する。
     *
     * @param callbackIntent コールバック時に受け取った Intent
     * @return コールバックのきっかけとなったトリガ情報
     */
    @NonNull
    public List<ObserveResult> getResults(Intent callbackIntent){
        String entryId = callbackIntent.getStringExtra(LocationObserver.INTENT_EXTRA_ENTRY_ID);
        if (entryId == null){
            return Collections.emptyList();
        }

        ConcurrentHashMap<String, String> entry = getEntries().get(entryId);
        if (entry == null) {
            return Collections.emptyList();
        }

        LocationObserver.Action action = (LocationObserver.Action) callbackIntent
                .getSerializableExtra(LocationObserver.INTENT_EXTRA_LOCATION_ACTION);

        return Collections.singletonList(new LocationObserver.ObserveResult(entry, action));
    }

    protected interface WorkInEntriesTask{
        void task(ConcurrentHashMap<String , ConcurrentHashMap<String, String>> entriesRef);
    }

    protected void workInEntries(WorkInEntriesTask task){
        synchronized (mEntries){
            task.task(mEntries);
        }
    }

    protected ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getEntries() {
        return mEntries;
    }

    /**
     * トリガを追加する。
     *
     * @param entries
     */
    public void addAllEntries(Collection<ConcurrentHashMap<String, String>> entries) {
        List<ConcurrentHashMap<String, String>> checkedEntries = new ArrayList<>();
        for (Map<String, String> entry : entries) {
            checkedEntries.add(new ConcurrentHashMap<>(entry));
        }

        if (checkedEntries.size() > 0) {
            onStartAddEntries();
            for (ConcurrentHashMap<String, String> entry : checkedEntries) {
                mEntries.put(entry.get(KEY_ENTRY_ID), entry);
            }
            onFinishAddEntries();
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.mEnabled ^ enabled) {
            this.mEnabled = enabled;
            if (this.mEnabled) {
                start();
            } else {
                stop();
            }
        }
    }

    public boolean getEnabled() {
        return mEnabled;
    }

    protected abstract void start();

    protected abstract void stop();

    /**
     * トリガを削除する。
     *
     * @param entryIds 削除するトリガの entryId リスト。
     */
    public void removeEntries(final Collection<String> entryIds) {
        onStartRemoveEntries();
        for (String entryId : entryIds) {
            mEntries.remove(entryId);
        }
        onFinishRemoveEntries(entryIds);
    }

    /**
     * 全てのトリガを削除する。
     */
    public final void removeAllEntries() {
        Collection<String> entryIds = mEntries.keySet();
        removeEntries(new ArrayList<>(entryIds));
    }

    protected abstract void doTaskImpl();

    synchronized protected final void doTask() {
        if (mTaskListener != null) {
            mTaskListener.OnStart();
        }
        doTaskImpl();
        if (mTaskListener != null) {
            mTaskListener.OnEnd();
        }
    }

    /**
     * トリガを追加する直前に呼び出される。必要に応じて監視の一時停止などを行う。
     */
    protected void onStartAddEntries() {
    }

    /**
     * トリガの追加が完了した直後に呼び出される。必要に応じて監視の再開などを行う。
     */
    protected void onFinishAddEntries() {
    }

    /**
     * トリガを削除する直前に呼び出される。必要に応じて監視の一時停止などを行う。
     */
    protected void onStartRemoveEntries() {
    }

    /**
     * トリガの削除が完了した直後に呼び出される。必要に応じて監視の再開などを行う。
     */
    protected void onFinishRemoveEntries(Collection<String> entryIds) {
    }

    /**
     * トリガを検知した際に呼び出し、コールバックの Intent を発行する。
     *
     * @param entryId
     * @param action
     */
    protected final void performIntent(String entryId, Action action) {
        Intent intent = cloneCallbackIntent();

        intent.putExtra(INTENT_EXTRA_ENTRY_ID, entryId);
        intent.putExtra(INTENT_EXTRA_LOCATION_ACTION, action);

        switch (mIntentType) {
            case Activity:
                mContext.startActivity(intent);
                break;
            case Service:
                mContext.startService(intent);
                break;
            case BroadcastReceiver:
                mContext.sendBroadcast(intent);
                break;
        }
    }

    private final Intent cloneCallbackIntent() {
        return (Intent) mCallbackIntent.clone();
    }

    /**
     * トリガ範囲に対するアクションの種類
     */
    public enum Action {
        Enter,
        Exit,
        None
    }

    /**
     * コールバック時の Intent の種別
     */
    public static enum IntentType {
        Activity,
        Service,
        BroadcastReceiver
    }

    /**
     * トリガ情報ビルダ
     */
    public static abstract class ObserveEntryBuilder {
        protected String mEntryId;
        protected Boolean mTransitionEnter = false;
        protected Boolean mTransitionExit = false;

        /**
         * コンストラクタ
         *
         * @param entryId
         */
        public ObserveEntryBuilder(String entryId, boolean enter, boolean exit) {
            mEntryId = entryId;
            mTransitionEnter = enter;
            mTransitionExit = exit;
        }

        public HashMap<String, String> build() {
            HashMap<String, String> entry = new HashMap<>();
            entry.put(KEY_ENTRY_ID, mEntryId);
            entry.put(KEY_TRANSITION_ENTER, mTransitionEnter.toString());
            entry.put(KEY_TRANSITION_EXIT, mTransitionExit.toString());
            return entry;
        }
    }

    /**
     * 検出結果
     */
    public static class ObserveResult implements Serializable, Parcelable {
        protected ConcurrentHashMap<String, String> mObserveEntry;
        protected Action mAction;

        public ObserveResult(ConcurrentHashMap<String, String> observeEntry, Action action) {
            mObserveEntry = observeEntry;
            mAction = action;
        }

        /**
         * 検出のきっかけとなったトリガ情報
         *
         * @return
         */
        public final ConcurrentHashMap<String, String> getObserveEntry() {
            return mObserveEntry;
        }

        /**
         * 検出のきっかけとなった Action
         *
         * @return
         * @see jp.upft.location_observer.LocationObserver.Action
         */
        public final Action getAction() {
            return mAction;
        }

        public static final Creator<ObserveResult> CREATOR = new Creator<ObserveResult>() {
            @Override
            public ObserveResult createFromParcel(Parcel source) {
                return new ObserveResult(source);
            }

            @Override
            public ObserveResult[] newArray(int size) {
                return new ObserveResult[size];
            }
        };

        public ObserveResult(Parcel in) {
            mAction = (Action) in.readSerializable();
            mObserveEntry = (ConcurrentHashMap<String, String>) in.readSerializable();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeSerializable(mAction);
            dest.writeSerializable(mObserveEntry);
        }
    }

    protected static abstract class PersistenceManager{
        private SharedPreferences mSharedPreferences;

        public PersistenceManager(SharedPreferences sharedPreferences) {
            mSharedPreferences = sharedPreferences;
        }

        protected SharedPreferences getSharedPreferences(){
            return mSharedPreferences;
        }

        protected abstract void saveMemoryToPreferences();
        protected abstract void loadMemoryFromPreferences();
    }
}
