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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.upft.content_trigger.ContentTriggerClient;
import jp.upft.content_trigger.ContentTriggerEntry;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiPushSubscription;
import com.kii.cloud.storage.KiiTopic;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiUserCallBack;
import com.kii.cloud.storage.exception.app.BadRequestException;
import com.kii.cloud.storage.exception.app.ConflictException;
import com.kii.cloud.storage.exception.app.ForbiddenException;
import com.kii.cloud.storage.exception.app.NotFoundException;
import com.kii.cloud.storage.exception.app.UnauthorizedException;
import com.kii.cloud.storage.exception.app.UndefinedException;

public class MyActivity extends ActionBarActivity {
    public static final String GROUP_ID = "0000000000000000000000000";
    private static final String SENDER_ID = "00000000000";

    private ContentTriggerClient mContentTriggerClient;

    private String mUserName;
    private String mPassword;

    private Button mUpdateButton;
    private Button mQRButton;
    private Switch mIsMale;
    private Switch mIsFemale;

    private boolean mPushLoaded = false;

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    ContentTriggerClient.ContentTriggerClientCallback mContentTriggerClientCallback = new ContentTriggerClient.ContentTriggerClientCallback() {
        @Override
        public void onClientInitializeFailed(Exception exception) {
            Toast.makeText(MyActivity.this, "Failed init", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onClientInitializeSucceeded() {
            // Settings.
            mContentTriggerClient.setEnabled(ContentTriggerEntry.TRIGGER_TYPE_GEOFENCE, true);
            mContentTriggerClient.setEnabled(ContentTriggerEntry.TRIGGER_TYPE_ACCESSPOINT, true);
            mContentTriggerClient.setEnabled(ContentTriggerEntry.TRIGGER_TYPE_IBEACON, true);
            mContentTriggerClient.setEnabled(ContentTriggerEntry.TRIGGER_TYPE_TIME, true);
            Toast.makeText(MyActivity.this, "Succeed init", Toast.LENGTH_SHORT).show();

            registerGCMIfNeed(new Runnable() {
                @Override
                public void run() {
                    activateUi();
                }
            });
        }

        @Override
        public void onFetchSucceeded(List<ContentTriggerEntry> triggers) {
            mContentTriggerClient.update(triggers);

            // Succeed update.
            Toast.makeText(getApplicationContext(), "Succeed update", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFetchFailed(Exception exception) {
            // Failed update.
            Toast.makeText(getApplicationContext(), "Failed update\n" + exception.toString(),
                    Toast.LENGTH_LONG).show();
        }
    };

    private void registerGCMIfNeed(final Runnable runnable) {
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());

        final SharedPreferences sharedPreferences = getSharedPreferences("GCM", Context.MODE_PRIVATE);
        String regId = sharedPreferences.getString("id", "");
        if (regId.isEmpty()){
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        String regId = gcm.register(SENDER_ID);
                        KiiUser.pushInstallation().install(regId);
                        sharedPreferences.edit().putString("id", regId).apply();
                    } catch (IOException | BadRequestException | ForbiddenException | UnauthorizedException | NotFoundException | ConflictException | UndefinedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    runOnUiThread(runnable);
                }
            }.execute();
        }else{
            runOnUiThread(runnable);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserName = getIntent().getStringExtra("userName");
        mPassword = getIntent().getStringExtra("password");

        setContentView(R.layout.activity_main);

        mUpdateButton = (Button) findViewById(R.id.button);
        mQRButton = (Button) findViewById(R.id.button2);
        mIsMale = (Switch) findViewById(R.id.switch1);
        mIsFemale = (Switch) findViewById(R.id.switch2);

        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update();
            }
        });
        mQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mContentTriggerClient.queryQRString("http://www.up-frontier.jp/");
                } catch (RemoteException e) {
                    Toast.makeText(MyActivity.this, "Service is not connected.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        mIsMale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        changeSubscribeSwitch(buttonView, isChecked, "male");
                    }
                });
            }
        });
        mIsFemale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        changeSubscribeSwitch(buttonView, isChecked, "female");
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Login if need, and update triggers.
        if (KiiUser.isLoggedIn()) {
            Toast.makeText(getApplicationContext(),
                           "Kii ログイン済み",
                           Toast.LENGTH_SHORT).show();

            initializeContentTriggerClient();
        } else {
            KiiUser.logIn(new KiiUserCallBack() {
                @Override
                public void onLoginCompleted(int token,
                                             KiiUser user,
                                             Exception exception) {
                    MyActivity.this.onLoginCompleted(exception);
                }
            }, mUserName, mPassword); // UserID, Password
        }
    }

    private void onLoginCompleted(Exception exception) {
        if (exception == null) {
            Toast.makeText(getApplicationContext(),
                           "Kii ログイン成功",
                           Toast.LENGTH_SHORT).show();

            initializeContentTriggerClient();
        } else {
            Toast.makeText(getApplicationContext(),
                           "Kii ログイン失敗\n" + exception.toString(),
                           Toast.LENGTH_LONG).show();
        }
    }

    private void initializeContentTriggerClient() {
        mContentTriggerClient
            = new ContentTriggerClient(MyActivity.this,
                                       GROUP_ID,
                                       mContentTriggerClientCallback);
    }

    @Override
    protected void onStop() {
        mContentTriggerClient.shutdown();
        mContentTriggerClient = null;

        deactivateUi();

        super.onStop();
    }

    private void update() {
        // Update triggers.
        mContentTriggerClient.fetch(mContentTriggerClientCallback);
    }

    private void checkPushLoaded(){
        boolean maleIsSubscribed = false;
        boolean femaleIsSubscribed = false;

        try {
            maleIsSubscribed = checkPushLoaded("male");
            femaleIsSubscribed = checkPushLoaded("female");

            final boolean finalMaleIsSubscribed = maleIsSubscribed;
            final boolean finalFemaleIsSubscribed = femaleIsSubscribed;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIsMale.setChecked(finalMaleIsSubscribed);
                    mIsFemale.setChecked(finalFemaleIsSubscribed);

                    mIsMale.setEnabled(true);
                    mIsFemale.setEnabled(true);

                    mPushLoaded = true;
                }
            });
        } catch (UnauthorizedException | ForbiddenException | IOException | ConflictException | NotFoundException | UndefinedException | BadRequestException ignored){
        }
    }

    private boolean checkPushLoaded(String topicName) throws ForbiddenException, BadRequestException, UndefinedException, IOException, ConflictException, NotFoundException, UnauthorizedException {
        KiiTopic topic = Kii.topic(topicName);
        return KiiUser.getCurrentUser().pushSubscription().isSubscribed(topic);
    }

    private void changeSubscribeSwitch(final CompoundButton compoundButton, boolean isChecked, String topicName){
        KiiTopic topic = Kii.topic(topicName);
        KiiUser user = KiiUser.getCurrentUser();
        KiiPushSubscription subscription = user.pushSubscription();
        if (isChecked) {
            try {
                subscription.subscribe(topic);
            } catch (final BadRequestException | UnauthorizedException | ForbiddenException | IOException | NotFoundException | UndefinedException ignored) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyActivity.this, ignored.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (ConflictException e){

            }
        }
        else {
            try {
                subscription.unsubscribe(topic);
            } catch (BadRequestException | UnauthorizedException | ForbiddenException | IOException | NotFoundException | UndefinedException ignored) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyActivity.this, ignored.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (ConflictException e){

            }
        }
    }

    private void activateUi(){
        mUpdateButton.setEnabled(true);
        mQRButton.setEnabled(true);

        if (mPushLoaded) {
            mIsMale.setEnabled(true);
            mIsFemale.setEnabled(true);
        } else {
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    checkPushLoaded();
                }
            });
        }
    }

    private void deactivateUi() {
        mUpdateButton.setEnabled(false);
        mQRButton.setEnabled(false);
        mIsMale.setEnabled(false);
        mIsFemale.setEnabled(false);
    }
}
