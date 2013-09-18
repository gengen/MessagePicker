package org.g_okuyama.messagepicker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

public class MessagePickerActivity extends ActionBarActivity{
    public static final String PREF_KEY = "pref";
    public static final String AVAILABLE_KEY = "available";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //actionBar.setDisplayShowTitleEnabled(false); 

        actionBar.addTab(actionBar.newTab().setText(R.string.tab1).setTabListener(
        		new MyTabListener<DateFragment>(this, "tab1", DateFragment.class)));

        actionBar.addTab(actionBar.newTab().setText(R.string.tab2).setTabListener(
        		new MyTabListener<CategoryFragment>(this, "tab2", CategoryFragment.class)));
        
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

	//デベロッパーページのサンプルのままだとタブ切り替え時に表示が重なる現象が発生したため、いくつか修正
    public static class MyTabListener<T extends Fragment> implements ActionBar.TabListener{
    	private Fragment mFragment;
    	private final ActionBarActivity mActivity;
    	private final String mTag;
    	private final Class<T> mClass;

    	/** Constructor used each time a new tab is created.
    	 * @param activity  The host Activity, used to instantiate the fragment
    	 * @param tag  The identifier tag for the fragment
    	 * @param clz  The fragment's Class, used to instantiate the fragment
    	 * @return 
    	 */
    	public MyTabListener(ActionBarActivity activity, String tag, Class<T> clz) {
    		mActivity = activity;
    		mTag = tag;
    		mClass = clz;
    		
    		mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
    	}
    	
    	/* The following are each of the ActionBar.TabListener callbacks */
    	public void onTabSelected(Tab tab, FragmentTransaction ft) {
    		// Check if the fragment is already initialized
    		if (mFragment == null) {
    			// If not, instantiate and add it to the activity
    			mFragment = Fragment.instantiate(mActivity, mClass.getName());
    			//ft.add(android.R.id.content, mFragment, mTag);
    			mActivity.getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFragment, mTag).commit();
    		} else {
    			// If it exists, simply attach it in order to show it
				//ft.attach(mFragment);
    			if (mFragment.isDetached()) { 
    				mActivity.getSupportFragmentManager().beginTransaction().attach(mFragment).commit();
    			}
    		}
    	}
    	
    	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    		if (mFragment != null) {
    			// Detach the fragment, because another one is being attached
    			//ft.detach(mFragment);
				mActivity.getSupportFragmentManager().beginTransaction().detach(mFragment).commit();
    		}
    	}
    	
    	public void onTabReselected(Tab tab, FragmentTransaction ft) {
    		// User selected the already selected tab. Usually do nothing.
    	}
    }
}
