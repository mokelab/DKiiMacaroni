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

import static jp.upft.location_observer.LocationObserver.KEY_ENTRY_ID;
import static jp.upft.location_observer.ble_observer.BLEObserverImpl.KEY_MAJOR_ID;
import static jp.upft.location_observer.ble_observer.BLEObserverImpl.KEY_MINOR_ID;
import static jp.upft.location_observer.ble_observer.BLEObserverImpl.KEY_RSSI;
import static jp.upft.location_observer.ble_observer.BLEObserverImpl.KEY_UUID;

import java.util.HashMap;

import jp.upft.location_observer.LocationObserver;

/**
 * トリガ情報 のビルダ Created by ryoga.kitagawa on 2015/01/21.
 */
public final class BLEObserveEntryBuilder extends LocationObserver.ObserveEntryBuilder {
    private String mUUID;
    private Integer mMajorId;
    private Integer mMinorId;
    private Integer mRSSILevel;

    public BLEObserveEntryBuilder(String entryId, boolean enter, boolean exit, String uuid) {
        super(entryId, enter, exit);
        mUUID = uuid;
    }

    public BLEObserveEntryBuilder setMajorId(Integer majorId) {
        mMajorId = majorId;
        return this;
    }

    public BLEObserveEntryBuilder setMinorId(Integer minorId) {
        mMinorId = minorId;
        return this;
    }

    public BLEObserveEntryBuilder setRSSILevel(Integer rssiLevel) {
        mRSSILevel = rssiLevel;
        return this;
    }

    @Override
    public HashMap<String, String> build() {
        HashMap<String, String> entry = super.build();
        entry.put(KEY_ENTRY_ID, mEntryId);
        entry.put(KEY_UUID, mUUID.replaceAll("-", "").toUpperCase());
        if (mMajorId != null) {
            entry.put(KEY_MAJOR_ID, String.valueOf(mMajorId));
        }
        if (mMinorId != null) {
            entry.put(KEY_MINOR_ID, String.valueOf(mMinorId));
        }
        if (mRSSILevel != null){
            entry.put(KEY_RSSI, String.valueOf(mRSSILevel));
        }

        return entry;
    }
}
