## Android版 ContentTriggerLibrary 利用に際する設定項目一覧

---

### ビルド環境

* Android Studio 1.2
* Android SDK 21 (min 10)
* KiiCloud SDK v2.1.36

---

### 設定項目

#### dependencies

* contenttrigger.aar
* locationobserver.aar

aar をモジュールとしてインポートし、以下のように記述

<pre>
dependencies {
    compile project(':contenttriggerlibrary')

    //Dependencies on LocationObserver.
    compile project(":locationobserver")
    compile 'com.google.code.gson:gson:2.3.1'
    compile 'com.google.android.gms:play-services:7.0.0'

    //Dependencies on KiiCloud.
	~ KiiCloud SDK ~
}
</pre>

#### manifest
以下の記述が必要になります。

	<!-- 共通 -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

    <!-- AccessPoint 関連 -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>

    <!-- Geofence 関連 -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

    <!-- BLEObserver -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

	<application ~~>

        <!-- Google Play Service -->
        <meta-data android:name="com.google.android.gms.version"
                   android:value="@integer/google_play_services_version"/>
		
		<!-- ContentTriggerService -->
        <service android:name="jp.upft.content_trigger.core.ContentTriggerService" android:process=":service" />

		<!-- ContentTriggerBroadcastReceiver を継承したクラス -->
        <receiver
            android:name="com.myproject.MyContentTriggerBroadcastReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.APPLICATION_RESTRICTIONS_CHANGED" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="jp.upft.locationobserver.ACTION_LOCATION_OBSERVE" />
                <action android:name="content_trigger_service.ACTION_TRIGGER" />
            </intent-filter>
        </receiver>
    </application>

### 実装手引き

0． KiiSDK を初期化し、KiiUser にログインする。  
詳細は KiiSDK 公式を参照。

1．ContentTrigger を初期化する。

<pre>
ContentTriggerClient.initialize(MyActivity.this, GROUP_ID, mContentTriggerClientCallback);
</pre>

2．ContentTriggerBroadcastReceiver を実装する。

<pre>
public class MyContentTriggerBroadcastReceiver extends ContentTriggerBroadcastReceiver {
    @Override
    public void onReceive(Context context, LocationObserver.ObserveResult trigger) {
    	//TODO: トリガに対する任意の処理を行う。
    }
}
</pre>

3．任意のタイミングでトリガ情報を更新する。

<pre>
ContentTriggerClient.getInstance().fetch(new ContentTriggerClient.ContentTriggerClientCallback() {
    @Override
    public void onFetchSucceeded(List<ContentTriggerEntry> triggers) {
        ContentTriggerClient.getInstance().update(triggers);
    }
});
</pre>