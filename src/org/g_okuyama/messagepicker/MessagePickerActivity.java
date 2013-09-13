package org.g_okuyama.messagepicker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;

import java.util.ArrayList;

public class MessagePickerActivity extends FragmentActivity implements TabHost.OnTabChangeListener{
    public static final String PREF_KEY = "pref";
    public static final String AVAILABLE_KEY = "available";

    // TabHost
    private TabHost mTabHost;
    // Last selected tabId
    private String mLastTabId;
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mMessageList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        TabHost.TabSpec tab1 = mTabHost.newTabSpec("tab1");
        tab1.setIndicator(getString(R.string.tab1));
        tab1.setContent(new DummyTabFactory(this));
        mTabHost.addTab(tab1);
        //mTabHost.setCurrentTab(0);

        TabHost.TabSpec tab2 = mTabHost.newTabSpec("tab2");
        tab2.setIndicator(getString(R.string.tab2));
        tab2.setContent(new DummyTabFactory(this));
        mTabHost.addTab(tab2);

        mTabHost.setOnTabChangedListener(this);

        onTabChanged("tab1");

        Intent intent = new Intent(this, MessagePickerService.class);
        startService(intent);

        //アクセシビリティ設定が有効でないときは、設定画面を起動する。
        SharedPreferences pref = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        if(!(pref.getBoolean(AVAILABLE_KEY, false))){
            startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onTabChanged(String tabId) {
        if(tabId != mLastTabId){
            FragmentTransaction fragmentTransaction
                    = getSupportFragmentManager().beginTransaction();

            if(tabId.equalsIgnoreCase("tab1")){
                fragmentTransaction.replace(R.id.content, new DateFragment());

            }else if(tabId.equalsIgnoreCase("tab2")){
                fragmentTransaction.replace(R.id.content, new CategoryFragment());
            }
            mLastTabId = tabId;
            fragmentTransaction.commit();
        }
    }

    private static class DummyTabFactory implements TabHost.TabContentFactory {

        /* Context */
        private final Context mContext;

        DummyTabFactory(Context context) {
            mContext = context;
        }

        @Override
        public View createTabContent(String tag) {
            View v = new View(mContext);
            return v;
        }
    }
}
