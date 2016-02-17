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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.content_trigger.BuildConfig;
import jp.upft.content_trigger.ContentTriggerEntry;
import jp.upft.content_trigger.ContentTriggerEntry.TermParams;
import jp.upft.content_trigger.ObserveResult;
import jp.upft.location_observer.LocationObserver;
import jp.upft.location_observer.access_point_observer.AccessPointObserver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 通信や Alarm、トリガを監視するサービス
 */
public final class ContentTriggerService extends Service {

    /**
     * コールバックの Intent に設定される Action。
     *
     * @see android.content.Intent#setAction(String)
     * @see android.content.Intent#getAction()
     */
    public static final String ACTION_CONTENT_TRIGGER = "content_trigger_service.ACTION_TRIGGER";

    /**
     * コールバックの Intent に付加される検出結果情報のキー。
     *
     * @see android.content.Intent#putExtra(String, android.os.Parcelable)
     * @see android.content.Intent#getParcelableExtra(String)
     * @see jp.upft.location_observer.LocationObserver.ObserveResult
     */
    public static final String INTENT_EXTRA_TRIGGER = "content_trigger_service.extra_trigger";

    /**
     * 初期化時の Intent に付加される、KiiGroup の ID
     *
     * @see android.content.Intent#putExtra(String, String)
     * @see android.content.Intent#getStringExtra(String)
     */
    public static final String INTENT_EXTRA_GROUPID = "content_trigger_service.extra_groupid";

    // constants.
    private static final String INTENT_ACTION_ALARM = "content_trigger_service.action_alarm";
    private static final String INTENT_EXTRA_ALARM = "content_trigger_service.extra_alarm";
    private static final String ENTRIES_PREFERENCE_NAME = "jp.upft.content_trigger.core.ContentTriggerService#entries";
    private static final String KEY_ENTRIES = "entries";

    private final ConcurrentHashMap<String, ContentTriggerEntry> mEntries = new ConcurrentHashMap<>();
    private ContentTriggerObserver mContentTriggerObserver;
    private ContentTriggerAlarmManager mContentTriggerAlarmManager;
    private final IContentTriggerService.Stub mStub = new IContentTriggerService.Stub() {
        @Override
        public void queryQRString(String qrStr) throws RemoteException {
            ContentTriggerService.this.queryQRString(qrStr);
        }

        @Override
        public void setEnabled(String type, boolean enabled) throws RemoteException {
            ContentTriggerService.this.setEnabled(type, enabled);
        }

        @Override
        public void updateEntries(List<ContentTriggerEntry> entries) throws RemoteException {
            ContentTriggerService.this.updateEntries(entries);
        }
    };
    private BroadcastReceiver mBroadcastReceiver;

    // instance methods.
    private static List<ContentTriggerEntry> entriesFromSharedPreference(Context context) {
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mode = Context.MODE_MULTI_PROCESS;
        } else {
            mode = Context.MODE_PRIVATE;
        }

        String json = context.getSharedPreferences(ENTRIES_PREFERENCE_NAME, mode).getString(
                KEY_ENTRIES, "");
        if (json.length() > 2) {
            return new Gson().fromJson(json, new TypeToken<ArrayList<ContentTriggerEntry>>() {
            }.getType());
        } else {
            return new ArrayList<>();
        }
    }

    private static Date nextStartDateTime(TermParams params) {
        return searchDateTime(params, true, true);
    }

    private static Date prevStartDateTime(TermParams params) {
        return searchDateTime(params, false, true);
    }

    private static Date nextEndDateTime(TermParams params) {
        return searchDateTime(params, true, false);
    }

    private static Date prevEndDateTime(TermParams params) {
        return searchDateTime(params, false, false);
    }

    private static Date searchDateTime(TermParams params, boolean inFuture, boolean isStartTime) {
        Date startDateTime = params.getStartDateTime();
        Date endDateTime = params.getEndDateTime();
        String startTime = params.getStartTime();
        String endTime = params.getEndTime();
        int dayOfMonth = params.getDayOfMonth();
        int dayOfWeekFlags = params.getDayOfWeekFlags();

        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        now.set(Calendar.SECOND, 0);
        Calendar startDate;
        Calendar endDate;

        if (startDateTime != null) {
            startDate = Calendar.getInstance();
            startDate.setTime(startDateTime);

            if (endDateTime != null) {
                endDate = Calendar.getInstance();
                endDate.setTime(endDateTime);

                if (isSplittedTerm(params)) {
                    startDate.set(Calendar.HOUR, 0);
                    startDate.set(Calendar.MINUTE, 0);
                    startDate.set(Calendar.SECOND, 0);
                    endDate.set(Calendar.HOUR, 23);
                    endDate.set(Calendar.MINUTE, 59);
                    endDate.set(Calendar.SECOND, 59);

                    DateConverter converter = new DateConverter();
                    Calendar queryCalendar = converter.timeToCalendar(now, isStartTime ? startTime
                            : endTime, inFuture);
                    if (queryCalendar == null) {
                        return null;
                    }

                    final int SEARCH_LIMIT = 1000; // 一定日数以上のサーチは行わない
                    for (int i = 0; i < SEARCH_LIMIT; i++) {
                        boolean isSoFar = inFuture ?
                                queryCalendar.after(endDate) :
                                queryCalendar.before(startDate);
                        if (isSoFar) {
                            return null;
                        }

                        if (!queryCalendar.after(endDate) && !queryCalendar.before(startDate)
                                && isEqualDayOfMonth(queryCalendar, dayOfMonth)
                                && isEqualDayOfWeek(queryCalendar, dayOfWeekFlags)) {
                            return queryCalendar.getTime(); // 全ての条件に合致した日時を返却
                        }

                        // 条件に合致しなかったので前の日
                        queryCalendar.add(Calendar.DATE, inFuture ? 1 : -1);
                    }

                    return null;

                } else {
                    Calendar tmpDate = isStartTime ? startDate : endDate;
                    boolean isAlreadyOver = inFuture ? now.before(tmpDate) : now.after(tmpDate);
                    if (isAlreadyOver) {
                        return tmpDate.getTime();
                    } else {
                        return null;
                    }
                }
            }
        }

        return null; // startDateTime は設定されていなければならない
    }

    private static boolean isSplittedTerm(TermParams params) {
        String startTime = params.getStartTime();
        String endTime = params.getEndTime();
        return startTime != null && startTime.length() > 0 && endTime != null
                && endTime.length() > 0;
    }

    private static boolean isEqualDayOfMonth(Calendar calendar, int dayOfMonth) {
        // dayOfMonth の設定がされていないか、条件をクリア
        return dayOfMonth == 0 || calendar.get(Calendar.DAY_OF_MONTH) == dayOfMonth;
    }

    private static boolean isEqualDayOfWeek(Calendar calendar, int dayOfWeekFlags) {
        int nowDayOfWeek = 1 << (calendar.get(Calendar.DAY_OF_WEEK) - 1);
        // 曜日の条件をクリア
        return (nowDayOfWeek & dayOfWeekFlags) != 0;
    }

    @Override
    public void onCreate() {
        mContentTriggerObserver = new ContentTriggerObserver(this, getClass());
        mContentTriggerAlarmManager = new ContentTriggerAlarmManager(this, getClass(),
                INTENT_ACTION_ALARM, INTENT_EXTRA_ALARM);
        loadEntriesPreference(this);
        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "ContentTriggerサービス 起動", Toast.LENGTH_SHORT).show();
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                AccessPointObserver.postScanResultAvailableAction();
            }
        };

        registerReceiver(mBroadcastReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (BuildConfig.DEBUG) {
            //For develop.
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setLocalDispatchPeriod(0);
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            Tracker tracker = analytics.newTracker("UA-36307655-2");
            tracker.enableExceptionReporting(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            action:
            {
                List<LocationObserver.ObserveResult> observeResults = mContentTriggerObserver
                        .getObserveResult(intent);

                if (observeResults.size() > 0) {
                    synchronized (mEntries) {
                        for (LocationObserver.ObserveResult result : observeResults) {
                            ContentTriggerEntry entry = mEntries.get(result.getObserveEntry().get(
                                    LocationObserver.KEY_ENTRY_ID));
                            ObserveResult observeResult = new ObserveResult(result,
                                    entry.getMasterId(), entry.getTitle(), entry.getDescription());
                            sendBroadcast(observeResult);
                        }
                    }
                    break action;
                }

                if (intent.getAction() != null) {
                    switch (intent.getAction()) {
                        case Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED:
                        case Intent.ACTION_BOOT_COMPLETED:
                            break action;
                        case INTENT_ACTION_ALARM:
                            ContentTriggerAlarmManager.AlarmEntry alarmEntry = mContentTriggerAlarmManager.extractAlarmEntry(intent);
                            String entryId = alarmEntry.getEntryId();
                            switch (alarmEntry.getAlarmType()) {
                                case End: {
                                    mContentTriggerObserver.removeTriggers(Collections
                                            .singletonList(entryId));
                                    ContentTriggerEntry entry = mEntries.get(entryId);
                                    if (entry != null) {
                                        TermParams params = entry.getTermParams();
                                        Date nextEndTime = nextEndDateTime(params);
                                        if (nextEndTime != null) {
                                            mContentTriggerAlarmManager
                                                    .addAlarm(new ContentTriggerAlarmManager.AlarmEntry(
                                                            entryId,
                                                            ContentTriggerAlarmManager.AlarmEntry.AlarmType.End,
                                                            nextEndTime));
                                        }
                                    }
                                }
                                    break;
                                case Start: {
                                    mContentTriggerObserver.addTriggers(Collections
                                            .singletonList(mEntries
                                                    .get(entryId)));
                                    ContentTriggerEntry entry = mEntries.get(entryId);
                                    if (entry != null) {
                                        TermParams params = entry.getTermParams();
                                        Date nextStartTime = nextStartDateTime(params);
                                        if (nextStartTime != null) {
                                            mContentTriggerAlarmManager
                                                    .addAlarm(new ContentTriggerAlarmManager.AlarmEntry(
                                                            entryId,
                                                            ContentTriggerAlarmManager.AlarmEntry.AlarmType.Start,
                                                            nextStartTime));
                                        }
                                    }
                                }
                                    break;
                            }
                            break;
                    }
                }
            }
        }
        return START_STICKY;
    }

    // region Calculation for TermParams.

    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }

    private void sendBroadcast(ObserveResult result) {
        Intent broadcastIntent = new Intent(ACTION_CONTENT_TRIGGER);
        broadcastIntent.putExtra(INTENT_EXTRA_TRIGGER, (Serializable) result);
        broadcastIntent.addCategory(getPackageName());
        sendBroadcast(broadcastIntent);
    }

    public void queryQRString(String qrStr) {
        mContentTriggerObserver.queryQRString(qrStr);
    }

    public void setEnabled(String type, boolean enabled) {
        mContentTriggerObserver.setEnabled(type, enabled);
    }

    public void updateEntries(List<ContentTriggerEntry> entries) {
        updateEntries(ContentTriggerService.this, entries);
    }

    private synchronized void updateEntries(Context context, List<ContentTriggerEntry> entries) {
        synchronized (mEntries) {
            mEntries.clear();
            for (ContentTriggerEntry entry : entries) {
                mEntries.put(entry.getEntry().get(LocationObserver.KEY_ENTRY_ID), entry);
            }
        }

        // Reset entries.
        mContentTriggerAlarmManager.removeAllAlarm();
        mContentTriggerObserver.removeAllTriggersAndWaitStop();

        // Update preferences.
        saveEntriesPreference(context, entries);

        for (ContentTriggerEntry entry : entries) {
            String entryId = entry.getEntry().get(LocationObserver.KEY_ENTRY_ID);

            TermParams params = entry.getTermParams();
            Date nextStartDateTime = nextStartDateTime(params);
            Date nextEndDateTime = nextEndDateTime(params);
            Date prevStartDateTime = prevStartDateTime(params);
            Date prevEndDateTime = prevEndDateTime(params);

            if (prevStartDateTime != null) {
                if (prevEndDateTime != null && prevEndDateTime.after(prevStartDateTime)) {
                    // １つ前のエントリは完了している
                    if (nextStartDateTime != null) {
                        // 次のエントリが存在する
                        mContentTriggerAlarmManager
                                .addAlarm(new ContentTriggerAlarmManager.AlarmEntry(entryId,
                                        ContentTriggerAlarmManager.AlarmEntry.AlarmType.Start,
                                        nextStartDateTime));
                        if (nextEndDateTime != null) {
                            mContentTriggerAlarmManager
                                    .addAlarm(new ContentTriggerAlarmManager.AlarmEntry(entryId,
                                            ContentTriggerAlarmManager.AlarmEntry.AlarmType.End,
                                            nextEndDateTime));
                        }
                    }
                } else {
                    // １つ前のエントリが続いている
                    mContentTriggerAlarmManager.addAlarm(new ContentTriggerAlarmManager.AlarmEntry(
                            entryId, ContentTriggerAlarmManager.AlarmEntry.AlarmType.Start,
                            prevStartDateTime));
                    if (nextEndDateTime != null) {
                        mContentTriggerAlarmManager
                                .addAlarm(new ContentTriggerAlarmManager.AlarmEntry(entryId,
                                        ContentTriggerAlarmManager.AlarmEntry.AlarmType.End,
                                        nextEndDateTime));
                    }
                }
            } else {
                // まだ始まっていない
                if (nextStartDateTime != null) {
                    // 次のエントリが存在する
                    mContentTriggerAlarmManager.addAlarm(new ContentTriggerAlarmManager.AlarmEntry(
                            entryId, ContentTriggerAlarmManager.AlarmEntry.AlarmType.Start,
                            nextStartDateTime));
                    if (nextEndDateTime != null) {
                        mContentTriggerAlarmManager
                                .addAlarm(new ContentTriggerAlarmManager.AlarmEntry(entryId,
                                        ContentTriggerAlarmManager.AlarmEntry.AlarmType.End,
                                        nextEndDateTime));
                    }
                }
            }
        }
    }

    private void saveEntriesPreference(Context context, List<ContentTriggerEntry> entries) {
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mode = Context.MODE_MULTI_PROCESS;
        } else {
            mode = Context.MODE_PRIVATE;
        }

        context.getSharedPreferences(ENTRIES_PREFERENCE_NAME, mode).edit()
                .putString(KEY_ENTRIES, new Gson().toJson(entries)).commit();
    }

    private void loadEntriesPreference(Context context) {
        updateEntries(context, entriesFromSharedPreference(context));
    }

    private static class DateConverter {
        Calendar timeToCalendar(Calendar baseDate, String str, boolean inFuture) {
            try {
                Date date = new SimpleDateFormat("kk:mm", Locale.JAPAN).parse(str);

                Calendar parsedDateCalendar = Calendar.getInstance();
                parsedDateCalendar.setTime(date);

                Calendar todayCalendar = (Calendar) baseDate.clone();
                todayCalendar.set(Calendar.HOUR_OF_DAY,
                        parsedDateCalendar.get(Calendar.HOUR_OF_DAY));
                todayCalendar.set(Calendar.MINUTE, parsedDateCalendar.get(Calendar.MINUTE));
                todayCalendar.set(Calendar.SECOND, parsedDateCalendar.get(Calendar.SECOND));

                int aDay = 0;
                if (inFuture && baseDate.after(todayCalendar)) {
                    aDay = 1;
                } else if (!inFuture && baseDate.before(todayCalendar)) {
                    aDay = -1;
                }
                if (aDay != 0) {
                    todayCalendar.add(Calendar.DATE, aDay);
                }

                return todayCalendar;
            } catch (ParseException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
    // end region
}
