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

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jp.upft.content_trigger_sample.R;

public class LoginActivity extends ActionBarActivity {

    public static final String APP_ID = "";
    public static final String APP_KEY = "";

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Kii.initialize(APP_ID, APP_KEY, Kii.Site.JP); // AppId, AppKey

        if (KiiUser.isLoggedIn()){
            startActivity(new Intent(LoginActivity.this, MyActivity.class));
            finish();
        }

        mListView = (ListView) findViewById(R.id.listView);

        final ArrayList<Pair<String, String>> accounts = new ArrayList<>();
        accounts.add(new Pair<>("client001", "client001"));
        accounts.add(new Pair<>("client002", "client002"));
        accounts.add(new Pair<>("client003", "client003"));
        accounts.add(new Pair<>("client004", "client004"));
        accounts.add(new Pair<>("client005", "client005"));

        mListView.setAdapter(new MyAdapter(accounts));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pair<String, String > account = (Pair<String, String>) parent.getAdapter().getItem(position);

                Intent intent = new Intent(LoginActivity.this, MyActivity.class);
                intent.putExtra("userName", account.first);
                intent.putExtra("password", account.second);

                startActivity(intent);
                finish();
            }
        });

    }

    private class MyAdapter extends BaseAdapter{

        private ArrayList<Pair<String, String>> mAccounts;

        public MyAdapter(ArrayList<Pair<String, String>> accounts) {
            mAccounts = accounts;
        }

        @Override
        public int getCount() {
            return mAccounts.size();
        }

        @Override
        public Pair<String, String> getItem(int position) {
            return mAccounts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = View.inflate(LoginActivity.this, android.R.layout.simple_list_item_1, null);
            }

            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(getItem(position).first);

            return convertView;
        }
    }
}
