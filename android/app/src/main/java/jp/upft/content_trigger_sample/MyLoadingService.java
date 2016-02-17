
package jp.upft.content_trigger_sample;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import jp.upft.content_trigger.ContentTriggerClient;
import jp.upft.content_trigger.ContentTriggerEntry;
import jp.upft.content_trigger.ObserveResult;

/**
 * コンテンツをロードするサービス
 */
public class MyLoadingService extends Service {
    private static final String EXTRA_RESULT
            = "jp.upft.content_trigger_sample.MyLoadingService.extra_result";
    private static final String EXTRA_MESSAGE
            = "jp.upft.content_trigger_sample.MyLoadingService.extra_message";

    /**
     * Content Triggerサービスへのクライアント
     */
    private ContentTriggerClient mContentTriggerClient;

    /**
     * Content Triggerサービスへのクライアントの初期化を待っているIntentのキュー
     * メインスレッドのみから触れる必要がある。
     */
    private Queue<Intent> mPendingIntents = new LinkedList<>();

    /**
     * ロード中のコンテンツの数。
     * メインスレッドのみから触れる必要がある。
     */
    private int mLoadingCount = 0;

    /**
     * Content Triggerサービスのコールバック
     */
    private ContentTriggerClient.ContentTriggerClientCallback mContentTriggerClientCallback = new ContentTriggerClient.ContentTriggerClientCallback() {
        @Override
        public void onClientInitializeFailed(Exception exception) {
            Toast.makeText(MyLoadingService.this,
                           "Failed init",
                           Toast.LENGTH_LONG).show();
        }

        @Override
        public void onClientInitializeSucceeded() {
            mMainHandler.post(new Runnable() {
                public void run() {
                    while (!mPendingIntents.isEmpty()) {
                        startLoading(mPendingIntents.poll());
                    }
                }
            });
        }

        @Override
        public void onFetchSucceeded(List<ContentTriggerEntry> triggers) {
        }

        @Override
        public void onFetchFailed(Exception exception) {
        }
    };

    /**
     * メインスレッドのハンドラ
     */
    private Handler mMainHandler = new Handler(Looper.getMainLooper());


    /**
     * このサービスでコンテンツをロードする。
     */
    public static void load(Context context,
                            String message,
                            ObserveResult result) {
        Intent intent = new Intent(context, MyLoadingService.class);

        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_RESULT, (Parcelable) result);

        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContentTriggerClient =
            new ContentTriggerClient(this,
                                     MyActivity.GROUP_ID,
                                     mContentTriggerClientCallback);
    }

    @Override
    public void onDestroy() {
        mContentTriggerClient.shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mContentTriggerClient.serviceConnected()) {
            startLoading(intent);

            return START_FLAG_REDELIVERY;
        } else {
            mPendingIntents.add(intent);

            return START_FLAG_REDELIVERY;
        }
    }

    private void startLoading(Intent intent) {
        if (intent == null) {
            return;
        }

        ObserveResult result = intent.getParcelableExtra(EXTRA_RESULT);
        String message = intent.getStringExtra(EXTRA_MESSAGE);

        startLoading(result, message);
    }

    private void startLoading(ObserveResult result, final String message) {
        mLoadingCount++;

        mContentTriggerClient.loadContent(
                result,
                new ContentTriggerClient.ContentTriggerClientCallback() {
                    @Override
                    public void onLoadContent(final ObserveResult result,
                                              final JSONObject object,
                                              Exception exception) {
                        super.onLoadContent(result, object, exception);

                        mMainHandler.post(new Runnable() {
                            public void run() {
                                MyLoadingService.this
                                    .onLoadContent(message, result, object);
                            }
                        });
                    }
                });
    }

    private void onLoadContent(String message,
                               ObserveResult result,
                               JSONObject object) {
        mLoadingCount--;

        if (mLoadingCount == 0) {
            stopSelf();
        }

        StringBuilder builder = new StringBuilder(message);

        if (object != null) {
            // Put information of got contents.
            Iterator<String> it = object.keys();
            while (it.hasNext()) {
                String key = it.next();

                try {
                    String value = object.getString(key);

                    builder
                        .append(", ")
                        .append(key)
                        .append(" : ")
                        .append(value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        MyContentTriggerBroadcastReceiver
            .showNotification(this, result.hashCode(), builder.toString());
    }
}
