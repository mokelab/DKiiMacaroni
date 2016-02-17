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


package jp.upft.location_observer.access_point_observer;

import static jp.upft.location_observer.LocationObserver.KEY_ENTRY_ID;

import java.util.HashMap;

import jp.upft.location_observer.LocationObserver;

/**
 * トリガ情報 のビルダ Created by ryoga.kitagawa on 2015/01/21.
 */
public final class AccessPointObserveEntryBuilder extends LocationObserver.ObserveEntryBuilder {
    private String mSSID;
    private String mBSSID;
    private Integer mRSSILevel;

    public AccessPointObserveEntryBuilder(String entryId, boolean enter, boolean exit, String ssid) {
        super(entryId, enter, exit);
        mSSID = ssid;
    }

    public AccessPointObserveEntryBuilder setBSSID(String bssid) {
        mBSSID = bssid;
        return this;
    }

    public AccessPointObserveEntryBuilder setRSSILevel(Integer rssiLevel) {
        mRSSILevel = rssiLevel;
        return this;
    }

    @Override
    public HashMap<String, String> build() {
        HashMap<String, String> entry = super.build();
        entry.put(KEY_ENTRY_ID, mEntryId);
        entry.put(AccessPointObserver.KEY_SSID, mSSID);
        if (mBSSID != null) {
            entry.put(AccessPointObserver.KEY_BSSID, mBSSID);
        }
        if (mRSSILevel != null){
            entry.put(AccessPointObserver.KEY_RSSI, String.valueOf(mRSSILevel));
        }

        return entry;
    }
}
