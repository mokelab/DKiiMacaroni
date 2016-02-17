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

import static jp.upft.content_trigger.ContentTriggerEntry.TRIGGER_TYPE_ACCESSPOINT;
import static jp.upft.content_trigger.ContentTriggerEntry.TRIGGER_TYPE_GEOFENCE;
import static jp.upft.content_trigger.ContentTriggerEntry.TRIGGER_TYPE_IBEACON;
import static jp.upft.content_trigger.ContentTriggerEntry.TRIGGER_TYPE_QR;
import static jp.upft.content_trigger.ContentTriggerEntry.TRIGGER_TYPE_TIME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.upft.content_trigger.core.ContentTriggerService;
import jp.upft.content_trigger.core.IContentTriggerService;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiGroup;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiServerCodeEntry;
import com.kii.cloud.storage.KiiServerCodeEntryArgument;
import com.kii.cloud.storage.KiiServerCodeExecResult;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiGroupCallBack;
import com.kii.cloud.storage.callback.KiiQueryCallBack;
import com.kii.cloud.storage.exception.app.BadRequestException;
import com.kii.cloud.storage.exception.app.ConflictException;
import com.kii.cloud.storage.exception.app.ForbiddenException;
import com.kii.cloud.storage.exception.app.NotFoundException;
import com.kii.cloud.storage.exception.app.UnauthorizedException;
import com.kii.cloud.storage.exception.app.UndefinedException;
import com.kii.cloud.storage.query.KiiClause;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;

/**
 * ContentTriggerLibrary の機能を Application 側から利用するためのインタフェース
 * <p/>
 * Created by ryoga.kitagawa on 2015/02/10.
 */

public final class ContentTriggerClient {

    public static final String TYPE_GEOFENCE;
    public static final String TYPE_BEACON;
    public static final String TYPE_ACCESSPOINT;
    public static final String TYPE_QR;
    public static final String TYPE_TIME;
    private static final String BUCKET_NAME = "trigger";
    final static private RuntimeException mServiceNotConnectedException = new RuntimeException(
            "Service is not connect.");

    static {
        TYPE_GEOFENCE = TRIGGER_TYPE_GEOFENCE;
        TYPE_BEACON = TRIGGER_TYPE_IBEACON;
        TYPE_ACCESSPOINT = TRIGGER_TYPE_ACCESSPOINT;
        TYPE_QR = TRIGGER_TYPE_QR;
        TYPE_TIME = TRIGGER_TYPE_TIME;
    }

    private final Context mContext;
    private final ContentTriggerClientCallback mContentTriggerClientCallback;
    private final String mGroupId;
    // instance
    private IContentTriggerService mContentTriggerService;
    private boolean mServiceConnected = false;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mContentTriggerService = IContentTriggerService.Stub.asInterface(service);
            mServiceConnected = true;
            mContentTriggerClientCallback.onClientInitializeSucceeded();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceConnected = false;
            mContentTriggerService = null;
        }
    };

    public ContentTriggerClient(Context context, String groupId,
            ContentTriggerClientCallback callback) {
        mContext = context;
        mGroupId = groupId;
        mContentTriggerClientCallback = callback;

        if (!rebindService(groupId)) {
            mContentTriggerClientCallback.onClientInitializeFailed(new Exception(
                    "Connection can not made so you will not receive the service object."));
        }
    }

    public void shutdown() {
        if (mServiceConnected) {
            mContext.unbindService(mServiceConnection);
        }
    }

    private boolean rebindService(String groupId) {
        Intent intent = new Intent(mContext, ContentTriggerService.class);
        intent.putExtra(ContentTriggerService.INTENT_EXTRA_GROUPID, groupId);
        mContext.getApplicationContext().startService(intent);

        int bindFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            bindFlag = Service.BIND_ABOVE_CLIENT;
        } else {
            bindFlag = 0;
        }

        return mContext.bindService(intent, mServiceConnection, bindFlag);
    }

    /**
     * ランタイムサービスに接続している
     *
     * @return
     */
    public boolean serviceConnected() {
        return mServiceConnected;
    }

    /**
     * トリガの種類ごとに、その有効無効を設定する
     *
     * @param type
     * @param enabled
     */
    public void setEnabled(String type, boolean enabled) {
        if (mContentTriggerService == null) {
            throw mServiceNotConnectedException;
        }
        try {
            mContentTriggerService.setEnabled(type, enabled);
        } catch (RemoteException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * トリガの情報を更新する (登録済みの項目は全て破棄される)
     */
    public void update(List<ContentTriggerEntry> triggerObjects) {
        if (mContentTriggerService == null) {
            throw mServiceNotConnectedException;
        }
        try {
            mContentTriggerService.updateEntries(triggerObjects);
        } catch (RemoteException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * トリガの情報を取得する
     *
     * @param callback 取得に関するコールバック
     */
    public void fetch(final ContentTriggerClientCallback callback) {
        if (mContentTriggerService == null) {
            callback.onFetchFailed(mServiceNotConnectedException);
        }

        final List<KiiObject> results = new ArrayList<>();

        final KiiQueryCallBack<KiiObject> kiiQueryCallBack = new KiiQueryCallBack<KiiObject>() {
            @Override
            public void onQueryCompleted(int token, KiiQueryResult<KiiObject> result,
                    Exception exception) {
                super.onQueryCompleted(token, result, exception);

                if (exception != null) {
                    callback.onFetchFailed(exception);
                    return;
                }

                if (result != null) {
                    results.addAll(result.getResult());
                    if (result.hasNext()) {
                        result.getNextQueryResult(this);
                        return;
                    }
                }

                List<ContentTriggerEntry> triggers = new ArrayList<>();

                for (KiiObject obj : results) {
                    try {
                        ContentTriggerEntry entry = new ContentTriggerEntry(obj);
                        triggers.add(entry);
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }

                callback.onFetchSucceeded(triggers);
            }
        };

        KiiGroupCallBack kiiGroupCallBack = new KiiGroupCallBack() {
            @Override
            public void onRefreshCompleted(int token, KiiGroup group, Exception exception) {
                super.onRefreshCompleted(token, group, exception);

                if (exception != null) {
                    callback.onFetchFailed(exception);
                    return;
                }

                KiiQuery query = new KiiQuery(KiiClause.equals("enabled", true));
                group.bucket(BUCKET_NAME).query(kiiQueryCallBack, query);
            }
        };

        KiiGroup group = KiiGroup.groupWithID(mGroupId);
        group.refresh(kiiGroupCallBack);
    }

    /**
     * 指定する文字列が、QRコードのトリガとして登録されているか問い合わせる
     *
     * @param qrStr 問い合わせるテキストデータ
     */
    public void queryQRString(String qrStr) throws RemoteException {
        if (mContentTriggerService == null) {
            throw mServiceNotConnectedException;
        }
        mContentTriggerService.queryQRString(qrStr);
    }

    /**
     * トリガの検出結果を用いて、そのコンテンツの情報を取得する
     *
     * @param result コンテンツを取得する検出結果情報
     * @param callback 取得に関するコールバック
     */
    public void loadContent(final ObserveResult result, final ContentTriggerClientCallback callback) {
        if (mContentTriggerService == null) {
            throw mServiceNotConnectedException;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final String masterId = result.getMasterId();

                KiiServerCodeEntry entry = Kii.serverCodeEntry("master");

                try {
                    JSONObject rawArgs = new JSONObject();

                    rawArgs.put("userID", KiiUser.getCurrentUser().getID());
                    rawArgs.put("contentID", masterId);

                    KiiServerCodeEntryArgument args = KiiServerCodeEntryArgument
                            .newArgument(rawArgs);
                    KiiServerCodeExecResult res = entry.execute(args);

                    JSONObject returnedValue = res.getReturnedValue()
                            .getJSONObject("returnedValue");

                    callback.onLoadContent(result, returnedValue, null);
                } catch (JSONException | ForbiddenException | BadRequestException | IOException
                        | UndefinedException | UnauthorizedException | NotFoundException
                        | ConflictException e) {
                    callback.onLoadContent(result, null, e);
                }

                return null;
            }
        }.execute();
    }

    /**
     * クライアントの各操作に対するコールバック
     */
    public static abstract class ContentTriggerClientCallback {
        /**
         * 初期化が完了した際によばれる。
         *
         * @see jp.upft.content_trigger.ContentTriggerClient#initialize(Context,
         *      String, ContentTriggerClientCallback)
         */
        public void onClientInitializeSucceeded() {
        }

        /**
         * 初期化が失敗した際に呼ばれる。
         *
         * @param exception
         * @see jp.upft.content_trigger.ContentTriggerClient#initialize(Context,
         *      String, ContentTriggerClientCallback)
         */
        public void onClientInitializeFailed(Exception exception) {
        }

        /**
         * トリガ情報の取得が成功した際に呼ばれる。
         */
        public void onFetchSucceeded(List<ContentTriggerEntry> triggers) {
        }

        /**
         * トリガ情報の取得が失敗した際に呼ばれる。
         *
         * @param exception
         */
        public void onFetchFailed(Exception exception) {

        }

        /**
         * トリガのコンテンツ情報を取得した際に呼ばれる。
         *
         * @param result
         * @param object
         */
        public void onLoadContent(ObserveResult result, JSONObject object, Exception exception) {
        }
    }
}
