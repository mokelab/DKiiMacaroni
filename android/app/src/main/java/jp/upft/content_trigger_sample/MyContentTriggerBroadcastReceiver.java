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


package jp.upft.content_trigger_sample;

import jp.upft.content_trigger.ContentTriggerBroadcastReceiver;
import jp.upft.content_trigger.ObserveResult;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.kii.cloud.storage.ReceivedMessage;

public class MyContentTriggerBroadcastReceiver extends ContentTriggerBroadcastReceiver {

    @Override
    protected void onReceive(Context context, ReceivedMessage message) {
        StringBuilder builder = new StringBuilder();

        Bundle extras = message.getMessage();
        for (String key : extras.keySet()){
            if (builder.length() == 0){
                builder.append("triggerType: push");
            }
            builder.append(", ").append(key).append(": ").append(extras.get(key));
        }

        showNotification(context, 0, builder.toString());
    }

    @Override
    public void onReceive(final Context context, ObserveResult result) {
        // Make toast text.
        final StringBuilder builder = new StringBuilder();
        builder.append("description : ")
                .append(result.getDescription()).append(", action : ")
                .append(result.getAction().name()); // Put information of
                                                    // observe.

        MyLoadingService.load(context, builder.toString(), result);
    }

    public static void showNotification(Context context,int entryId, String bigText) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setTicker("トリガ検知");
        notificationBuilder.setContentTitle("ContentTriggerLibrary");
        notificationBuilder.setContentText(bigText);
        notificationBuilder.setWhen(System.currentTimeMillis());

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(bigText);
        bigTextStyle.setBigContentTitle("ContentTriggerLibrary");
        bigTextStyle.setSummaryText("トリガを検知しました");

        notificationBuilder.setStyle(bigTextStyle);

        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender().setStartScrollBottom(true);

        notificationBuilder.extend(extender);

        manager.notify(entryId, notificationBuilder.build());
    }
}
