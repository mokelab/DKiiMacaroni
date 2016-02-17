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

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * トリガの開始、停止タイミングの Alarm のマネージャ
 * <p/>
 * Created by ryoga.kitagawa on 2015/02/18.
 */

final class ContentTriggerAlarmManager {

    private final Set<AlarmEntry> mAlarmEntries = new HashSet<>();
    private final Context mContext;
    private final Class<? extends Service> mServiceClass;
    private final String mIntentExtraKey;
    private final String mIntentAction;

    /**
     * コンストラクタ
     *
     * @param context
     * @param callbackServiceClass Alarm のコールバックを受けたい Service の型。
     * @param intentAction コールバック時の Intent に設定される Action。
     * @param intentExtraKey コールバック時に Intent に設定される Extra のキー。
     * @see AlarmEntry
     */
    public ContentTriggerAlarmManager(Context context,
            Class<? extends Service> callbackServiceClass, String intentAction,
            String intentExtraKey) {
        mContext = context;
        mServiceClass = callbackServiceClass;
        mIntentExtraKey = intentExtraKey;
        mIntentAction = intentAction;
    }

    /**
     * Alarm を追加する。
     *
     * @param alarmEntry
     */
    public void addAlarm(AlarmEntry alarmEntry) {
        mAlarmEntries.add(alarmEntry);

        Date date = alarmEntry.getDate();

        PendingIntent pendingIntent = pendingIntentFromParams(alarmEntry);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, date.getTime(), pendingIntent);
    }

    public void removeAllAlarm() {
        for (AlarmEntry entry : mAlarmEntries) {
            PendingIntent pendingIntent = pendingIntentFromParams(entry);
            AlarmManager alarmManager = (AlarmManager) mContext
                    .getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
    }

    private PendingIntent pendingIntentFromParams(AlarmEntry entry) {
        Intent intent = new Intent(mContext, mServiceClass);
        intent.setAction(mIntentAction);

        // Androidのバグにより、PendingIntent内のIntentにSerializableな
        // オブジェクトを直接含めると例外が投げられてしまう。
        // 対処としてBundleで包む。
        // https://code.google.com/p/android/issues/detail?id=6822
        Bundle bunde = new Bundle();

        bunde.putSerializable(mIntentExtraKey, entry);

        intent.putExtra(mIntentExtraKey, bunde);
        intent.setType(entry.getAlarmType() + "%" + entry.getEntryId());
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public AlarmEntry extractAlarmEntry(Intent intent) {
        return (AlarmEntry) intent.getBundleExtra(mIntentExtraKey).getSerializable(mIntentExtraKey);
    }

    /**
     * Alarm の Intent に付加される情報
     */
    public static class AlarmEntry implements Serializable {
        private final AlarmType mAlarmType;
        private final String mEntryId;
        private final Date mDate;
        public AlarmEntry(String entryId, AlarmType alarmType, Date date) {
            mEntryId = entryId;
            mAlarmType = alarmType;
            mDate = date;
        }

        public String getEntryId() {
            return mEntryId;
        }

        public AlarmType getAlarmType() {
            return mAlarmType;
        }

        public Date getDate() {
            return mDate;
        }

        public enum AlarmType {
            Start,
            End
        }
    }
}
