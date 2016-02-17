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


package jp.upft.location_observer.geofence_observer;

import java.util.HashMap;

import jp.upft.location_observer.LocationObserver;

/**
 * トリガ情報 のビルダ Created by ryoga.kitagawa on 2015/01/21.
 */
public final class GeofenceObserveEntryBuilder extends LocationObserver.ObserveEntryBuilder {
    private double mLatitude;
    private double mLongitude;
    private float mRadius;

    public GeofenceObserveEntryBuilder(String entryId, boolean enter, boolean exit) {
        super(entryId, enter, exit);
    }

    public GeofenceObserveEntryBuilder setLatitude(double latitude) {
        mLatitude = latitude;
        return this;
    }

    public GeofenceObserveEntryBuilder setLongitude(double longitude) {
        mLongitude = longitude;
        return this;
    }

    public GeofenceObserveEntryBuilder setRadius(float radius) {
        mRadius = radius;
        return this;
    }

    @Override
    public HashMap<String, String> build() {
        HashMap<String, String> entry = super.build();

        entry.put(GeofenceObserver.KEY_LATITUDE, String.valueOf(mLatitude));
        entry.put(GeofenceObserver.KEY_LONGITUDE, String.valueOf(mLongitude));
        entry.put(GeofenceObserver.KEY_RADIUS, String.valueOf(mRadius));

        return entry;
    }
}
