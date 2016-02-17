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

import jp.upft.content_trigger.core.ContentTriggerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.kii.cloud.storage.PushMessageBundleHelper;
import com.kii.cloud.storage.ReceivedMessage;

/**
 * トリガに対するコールバックの処理、兼端末の起動時やアプリの再インストール時にサービスを立ち上げるReceiver
 */
public abstract class ContentTriggerBroadcastReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            Bundle extras = intent.getExtras();
            ReceivedMessage message = PushMessageBundleHelper.parse(extras);
            onReceive(context, message);
        } else {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED:
                case Intent.ACTION_BOOT_COMPLETED:
                    Intent serviceIntent = new Intent(context, ContentTriggerService.class);
                    context.startService(serviceIntent);
                    break;
                case ContentTriggerService.ACTION_CONTENT_TRIGGER:
                    ObserveResult trigger = (ObserveResult) intent
                            .getSerializableExtra(ContentTriggerService.INTENT_EXTRA_TRIGGER);
                    onReceive(context, trigger);
                    break;
            }
        }
    }

    /**
     * Push通知のコールバックを処理する
     *
     * @param context
     * @param message 受信情報
     */
    protected abstract void onReceive(Context context, ReceivedMessage message);

    /**
     * ランタイムサービスのコールバックを処理する
     *
     * @param context
     * @param trigger 検出結果
     */
    public abstract void onReceive(Context context, ObserveResult trigger);
}
