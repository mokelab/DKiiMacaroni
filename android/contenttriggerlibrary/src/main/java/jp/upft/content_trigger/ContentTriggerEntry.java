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

package jp.upft.content_trigger;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import jp.upft.location_observer.LocationObserver;
import jp.upft.location_observer.access_point_observer.AccessPointObserveEntryBuilder;
import jp.upft.location_observer.access_point_observer.AccessPointObserver;
import jp.upft.location_observer.ble_observer.BLEObserveEntryBuilder;
import jp.upft.location_observer.ble_observer.BLEObserver;
import jp.upft.location_observer.geofence_observer.GeofenceObserveEntryBuilder;
import jp.upft.location_observer.geofence_observer.GeofenceObserver;

import android.os.Parcel;
import android.os.Parcelable;

import com.kii.cloud.storage.GeoPoint;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.exception.IllegalKiiBaseObjectFormatException;

/**
 * トリガ情報
 * <p/>
 * Created by ryoga.kitagawa on 2015/02/10.
 */
public class ContentTriggerEntry implements Parcelable {
    public static final Creator<ContentTriggerEntry> CREATOR = new Creator<ContentTriggerEntry>() {
        @Override
        public ContentTriggerEntry createFromParcel(Parcel source) {
            return new ContentTriggerEntry(source);
        }

        @Override
        public ContentTriggerEntry[] newArray(int size) {
            return new ContentTriggerEntry[size];
        }
    };
    public static final String KEY_TRIGGER_TYPE;
    public static final String KEY_QR_TARGET;
    public static final String KEY_UUID;
    public static final String KEY_MAJOR_ID;
    public static final String KEY_MINOR_ID;
    public static final String KEY_BLE_RSSI;
    public static final String KEY_LATITUDE;
    public static final String KEY_LONGITUDE;
    public static final String KEY_RADIUS;
    public static final String KEY_SSID;
    public static final String KEY_BSSID;
    public static final String KEY_AP_RSSI;

    public static final String TRIGGER_TYPE_GEOFENCE;
    public static final String TRIGGER_TYPE_IBEACON;
    public static final String TRIGGER_TYPE_ACCESSPOINT;
    public static final String TRIGGER_TYPE_QR;
    public static final String TRIGGER_TYPE_TIME;

    private static final String ID = "_id";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String MASTER_ID = "relation";
    private static final String START_DATE_TIME = "startDateTime";
    private static final String END_DATE_TIME = "endDateTime";
    private static final String MONDAY = "monday";
    private static final String TUESDAY = "tuesday";
    private static final String WEDNESDAY = "wednesday";
    private static final String THURSDAY = "thursday";
    private static final String FRIDAY = "friday";
    private static final String SATURDAY = "saturday";
    private static final String SUNDAY = "sunday";
    private static final String DAY_OF_MONTH = "dayOfMonth";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String TRANSITION_ENTER = "transitionEnter";
    private static final String TRANSITION_EXIT = "transitionExit";

    static {
        KEY_TRIGGER_TYPE = "triggerType";
        KEY_QR_TARGET = "qr_target";
        KEY_UUID = BLEObserver.KEY_UUID;
        KEY_MAJOR_ID = BLEObserver.KEY_MAJOR_ID;
        KEY_MINOR_ID = BLEObserver.KEY_MINOR_ID;
        KEY_BLE_RSSI = BLEObserver.KEY_RSSI;

        KEY_LATITUDE = GeofenceObserver.KEY_LATITUDE;
        KEY_LONGITUDE = GeofenceObserver.KEY_LONGITUDE;
        KEY_RADIUS = GeofenceObserver.KEY_RADIUS;

        KEY_SSID = AccessPointObserver.KEY_SSID;
        KEY_BSSID = AccessPointObserver.KEY_BSSID;
        KEY_AP_RSSI = AccessPointObserver.KEY_RSSI;

        TRIGGER_TYPE_GEOFENCE = "geo";
        TRIGGER_TYPE_IBEACON = "beacon";
        TRIGGER_TYPE_ACCESSPOINT = "wifi";
        TRIGGER_TYPE_QR = "qr";
        TRIGGER_TYPE_TIME = "time";
    }

    private final TermParams mTermParams;
    private HashMap<String, String> mEntry;
    private String mTriggerType = "";
    private String mTitle = "";
    private String mDescription = "";
    private String mMasterId = "";

    public ContentTriggerEntry(Parcel in) {
        mEntry = (HashMap<String, String>) in.readSerializable();
        mTermParams = (TermParams) in.readSerializable();
        mTriggerType = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mMasterId = in.readString();
    }

    /**
     * コンストラクタ
     *
     * @param object トリガ情報となる KiiObject。
     */
    public ContentTriggerEntry(KiiObject object) throws Exception {
        String entryId = object.getString(ID);
        mTriggerType = object.getString(KEY_TRIGGER_TYPE);
        mTitle = object.getString(TITLE);
        mDescription = object.getString(DESCRIPTION, "");
        mMasterId = object.getString(MASTER_ID);
        long startDateTime = object.getLong(START_DATE_TIME, 0L);
        long endDateTime = object.getLong(END_DATE_TIME, 0L);
        String startTime = object.getString(START_TIME, "");
        String endTime = object.getString(END_TIME, "");
        boolean transitionEnter = object.getBoolean(TRANSITION_ENTER, false);
        boolean transitionExit = object.getBoolean(TRANSITION_EXIT, false);
        int dayOfMonth = object.getInt(DAY_OF_MONTH, 0);
        int dayOfWeekFlags = 0;
        {
            HashMap<String, Integer> dayOfWeeks = new HashMap<>();
            dayOfWeeks.put(SUNDAY, TermParams.SUNDAY);
            dayOfWeeks.put(MONDAY, TermParams.MONDAY);
            dayOfWeeks.put(TUESDAY, TermParams.TUESDAY);
            dayOfWeeks.put(WEDNESDAY, TermParams.WEDNESDAY);
            dayOfWeeks.put(THURSDAY, TermParams.THURSDAY);
            dayOfWeeks.put(FRIDAY, TermParams.FRIDAY);
            dayOfWeeks.put(SATURDAY, TermParams.SATURDAY);
            for (String dayOfWeek : dayOfWeeks.keySet()) {
                if (object.getBoolean(dayOfWeek, true)) {
                    dayOfWeekFlags |= dayOfWeeks.get(dayOfWeek);
                }
            }
        }

        mTermParams = new TermParams();
        mTermParams.mDayOfWeekFlags = dayOfWeekFlags;
        mTermParams.mDayOfMonth = dayOfMonth;
        mTermParams.mStartTime = startTime;
        mTermParams.mEndTime = endTime;
        if (startDateTime != 0) {
            mTermParams.mStartDateTime = new Date(startDateTime);
        }
        if (endDateTime != 0) {
            mTermParams.mEndDateTime = new Date(endDateTime);
        }

        if (mTriggerType.equals(TRIGGER_TYPE_IBEACON)) {
            String uuid = object.getString("peripheralUUID");
            BLEObserveEntryBuilder builder = new BLEObserveEntryBuilder(entryId,
                    transitionEnter, transitionExit, uuid);
            try {
                int majorId = object.getInt("major");
                builder.setMajorId(majorId);
            } catch (IllegalKiiBaseObjectFormatException ignored) {
            }
            try {
                int minorId = object.getInt("minor");
                builder.setMinorId(minorId);
            } catch (IllegalKiiBaseObjectFormatException ignored) {
            }
            try {
                int rssi = object.getInt("RSSI");
                builder.setRSSILevel(rssi);
            } catch (IllegalKiiBaseObjectFormatException ignored) {
            }
            mEntry = builder.build();

        } else if (mTriggerType.equals(TRIGGER_TYPE_ACCESSPOINT)) {
            String ssid = object.getString(KEY_SSID);
            AccessPointObserveEntryBuilder builder = new AccessPointObserveEntryBuilder(
                    entryId, transitionEnter, transitionExit, ssid);
            try {
                String bssid = object.getString(KEY_BSSID);
                builder.setBSSID(bssid);
            } catch (IllegalKiiBaseObjectFormatException ignored) {
            }
            mEntry = builder.build();

        } else if (mTriggerType.equals(TRIGGER_TYPE_GEOFENCE)) {
            GeofenceObserveEntryBuilder builder = new GeofenceObserveEntryBuilder(entryId,
                    transitionEnter, transitionExit);
            GeoPoint geoPoint = object.getGeoPoint("geoPoint");
            builder.setLatitude(geoPoint.getLatitude());
            builder.setLongitude(geoPoint.getLongitude());
            builder.setRadius(object.getLong("range"));
            mEntry = builder.build();

        } else if (mTriggerType.equals(TRIGGER_TYPE_QR)) {
            mEntry = new HashMap<>();
            mEntry.put(LocationObserver.KEY_ENTRY_ID, entryId);
            mEntry.put(KEY_QR_TARGET, object.getString("target"));

        } else if (mTriggerType.equals(TRIGGER_TYPE_TIME)) {
            mEntry = new HashMap<>();
            mEntry.put(LocationObserver.KEY_ENTRY_ID, entryId);

        } else {
            throw new Exception("Unknown Entry.");
        }
        mEntry.put(KEY_TRIGGER_TYPE, mTriggerType);
    }

    /**
     * トリガ情報を取得する。
     *
     * @return トリガ情報。各種 ObserveEntry に継承されているため型チェックの後 キャストして詳細を取得する。
     */
    public HashMap<String, String> getEntry() {
        return mEntry;
    }

    /**
     * トリガを監視するタイミングを取得する。
     *
     * @return キーはタイミングの種類、値は設定されている日時。
     */
    public TermParams getTermParams() {
        return mTermParams;
    }

    /**
     * トリガのタイトルを取得する。
     *
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * トリガの説明を取得する。
     *
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * トリガのコンテンツを格納する KiiObject の ID を取得する。
     *
     * @return
     */
    public String getMasterId() {
        return mMasterId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mEntry);
        dest.writeSerializable(mTermParams);
        dest.writeString(mTriggerType);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeString(mMasterId);
    }

    public static class TermParams implements Serializable {
        private static final int SUNDAY = 0x0001;
        private static final int MONDAY = 0x0002;
        private static final int TUESDAY = 0x0004;
        private static final int WEDNESDAY = 0x0008;
        private static final int THURSDAY = 0x0010;
        private static final int FRIDAY = 0x0020;
        private static final int SATURDAY = 0x0040;

        private int mDayOfWeekFlags;
        private int mDayOfMonth;
        private Date mStartDateTime;
        private Date mEndDateTime;
        private String mStartTime;
        private String mEndTime;

        public String getStartTime() {
            return mStartTime;
        }

        public String getEndTime() {
            return mEndTime;
        }

        public Date getStartDateTime() {
            return mStartDateTime;
        }

        public Date getEndDateTime() {
            return mEndDateTime;
        }

        public int getDayOfMonth() {
            return mDayOfMonth;
        }

        public int getDayOfWeekFlags() {
            return mDayOfWeekFlags;
        }
    }
}
