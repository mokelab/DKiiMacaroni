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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jp.upft.location_observer.LocationObserver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Geofence の監視クラス Created by ryoga.kitagawa on 2015/01/20.
 */
public final class GeofenceObserver extends LocationObserver {

    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_RADIUS = "range";

    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mPendingIntent;

    private Map<String, ConcurrentHashMap<String, String>> mPendingAddEntries = new HashMap<>();
    private Set<String> mPendingRemoveEntryIds = new HashSet<>();

    public GeofenceObserver(Context context, Intent callbackIntent, IntentType intentType) {
        super(context, callbackIntent, intentType, null);

        GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {

            @Override
            public void onConnected(Bundle bundle) {
                synchronized (GeofenceObserver.this) {
                    if (!mPendingRemoveEntryIds.isEmpty()) {
                        GeofenceObserver.super.removeEntries(mPendingRemoveEntryIds);
                        mPendingRemoveEntryIds.clear();
                    }
                    if (!mPendingAddEntries.isEmpty()) {
                        GeofenceObserver.super.addAllEntries(mPendingAddEntries.values());
                        mPendingAddEntries.clear();
                    }
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                // noop
            }
        };
        mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API)
                .addConnectionCallbacks(connectionCallbacks).build();

        switch (intentType) {
            case Activity:
                mPendingIntent = PendingIntent.getActivity(context, 0, callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                break;
            case Service:
                mPendingIntent = PendingIntent.getService(context, 0, callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                break;
            case BroadcastReceiver:
                mPendingIntent = PendingIntent.getBroadcast(context, 0, callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                break;
        }
    }

    /**
     * @see jp.upft.location_observer.LocationObserver#getResults(android.content.Intent)
     */
    @Override
    @NonNull
    public List<ObserveResult> getResults(Intent callbackIntent) {
        // Check geofence event.
        final GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(callbackIntent);

        if (geofencingEvent != null) {
            if (!geofencingEvent.hasError()) {
                int transition = geofencingEvent.getGeofenceTransition();
                final Action action;
                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        action = Action.Exit;
                        break;
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        action = Action.Enter;
                        break;
                    default:
                        return Collections.emptyList();
                }

                final List<ObserveResult> results = new ArrayList<>();

                workInEntries(new WorkInEntriesTask() {
                    @Override
                    public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                        for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                            ConcurrentHashMap<String, String> entry = getEntries().get(geofence.getRequestId());

                            ObserveResult result = new ObserveResult(entry, action);

                            if (entry != null) {
                                results.add(result);
                            }
                        }
                    }
                });

                if (!results.isEmpty()) {
                    return results;
                } else {
                    return Collections.emptyList();
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public synchronized void removeEntries(Collection<String> entryIds) {
        if (entryIds.isEmpty()) {
            return;
        }

        if (!mGoogleApiClient.isConnected()) {
            for (String entryId : entryIds) {
                mPendingAddEntries.remove(entryId);
                mPendingRemoveEntryIds.add(entryId);
            }
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            super.removeEntries(entryIds);
        }
    }

    @Override
    protected void doTaskImpl() {
        // noop
    }

    /**
     * @see jp.upft.location_observer.LocationObserver#addAllEntries(java.util.Collection)
     */
    @Override
    public synchronized void addAllEntries(Collection<ConcurrentHashMap<String, String>> entries) {
        List<ConcurrentHashMap<String, String>> checkedEntries = new ArrayList<>();
        for (ConcurrentHashMap<String, String> entry : entries) {
            if (entry.get(KEY_RADIUS) != null
                    || entry.get(KEY_LONGITUDE) != null
                    || entry.get(KEY_LATITUDE) != null) {
                checkedEntries.add(new ConcurrentHashMap<>(entry));
            }
        }

        if (checkedEntries.isEmpty()) {
            return;
        }

        if (!mGoogleApiClient.isConnected()) {
            for (ConcurrentHashMap<String, String> entry : checkedEntries) {
                mPendingRemoveEntryIds.remove(entry.get(KEY_ENTRY_ID));
                mPendingAddEntries.put(entry.get(KEY_ENTRY_ID), entry);
            }
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            super.addAllEntries(checkedEntries);
        }
    }

    @Override
    protected void start() {
        if (mTaskListener != null) {
            mTaskListener.OnStart();
        }


        workInEntries(new WorkInEntriesTask() {
            @Override
            public void task(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> entriesRef) {
                GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                for (ConcurrentHashMap<String, String> entry : entriesRef.values()) {
                    builder.addGeofence(createGeofence(entry));
                }

                //Geofencing service should not trigger notification at the moment when the geofence is added.
                builder.setInitialTrigger(0);

                GeofencingRequest request = builder.build();

                if (!request.getGeofences().isEmpty()){
                    LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, request, mPendingIntent);
                }
            }
        });
    }

    @Override
    protected synchronized void stop() {
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()){
                mGoogleApiClient.connect();
            }
        }else {
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, mPendingIntent);
        }
    }

    @Override
    protected void onFinishAddEntries() {
        if (getEnabled()) {
            start();
        }
    }

    @Override
    protected void onFinishRemoveEntries(Collection<String> entryIds) {
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, new ArrayList<>(entryIds));
        if (getEntries().isEmpty()){
            if (mTaskListener != null) {
                mTaskListener.OnEnd();
            }
        }
    }

    private Geofence createGeofence(Map<String, String> entry) {
        boolean enter = Boolean.parseBoolean(entry.get(KEY_TRANSITION_ENTER));
        boolean exit = Boolean.parseBoolean(entry.get(KEY_TRANSITION_EXIT));

        int transitionTypes = 0;
        if (enter)
            transitionTypes |= Geofence.GEOFENCE_TRANSITION_ENTER;
        if (exit)
            transitionTypes |= Geofence.GEOFENCE_TRANSITION_EXIT;

        if (transitionTypes == 0) {
            transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
                    | Geofence.GEOFENCE_TRANSITION_EXIT;
        }

        return new Geofence.Builder()
                .setRequestId(entry.get(KEY_ENTRY_ID))
                .setTransitionTypes(transitionTypes)
                .setCircularRegion(Double.parseDouble(entry.get(KEY_LATITUDE)),
                        Double.parseDouble(entry.get(KEY_LONGITUDE)),
                        Float.parseFloat(entry.get(KEY_RADIUS)))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }
}
