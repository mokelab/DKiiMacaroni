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

import static jp.upft.location_observer.LocationObserver.Action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.location_observer.LocationObserver;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 検出結果
 */
public class ObserveResult implements Serializable, Parcelable {
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
    protected final ConcurrentHashMap<String, String> mObserveEntry;
    protected final String mMasterId;
    protected final String mTitle;
    protected final String mDescription;
    protected final Action mAction;

    public ObserveResult(HashMap<String, String> observeEntry, String masterId, String title,
            String description, Action action) {
        mObserveEntry = new ConcurrentHashMap<>(observeEntry);
        mMasterId = masterId;
        mTitle = title;
        mDescription = description;
        mAction = action;
    }

    public ObserveResult(LocationObserver.ObserveResult result, String masterId, String title,
            String description) {
        mObserveEntry = result.getObserveEntry();
        mMasterId = masterId;
        mTitle = title;
        mDescription = description;
        mAction = result.getAction();
    }

    public ObserveResult(Parcel in) {
        mAction = (Action) in.readSerializable();
        mObserveEntry = (ConcurrentHashMap<String, String>) in.readSerializable();
        mMasterId = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
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
     * @see Action
     */
    public final Action getAction() {
        return mAction;
    }

    public final String getMasterId() {
        return mMasterId;
    }

    public final String getTitle() {
        return mTitle;
    }

    public final String getDescription() {
        return mDescription;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mAction);
        dest.writeSerializable(mObserveEntry);
        dest.writeString(mMasterId);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
    }
}
