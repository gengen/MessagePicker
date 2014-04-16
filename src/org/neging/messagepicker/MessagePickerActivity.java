package org.neging.messagepicker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jp.beyond.sdk.Bead;
import jp.beyond.sdk.Bead.ContentsOrientation;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class MessagePickerActivity extends ActionBarActivity{
	public static final String TAG = "MessagePicker";
    public static final String PREF_KEY = "pref";
    public static final String AVAILABLE_KEY = "available";
    
    public static final boolean DEBUG = false;
    
    ProgressDialog mProgressDialog = null;
    Handler mHandler = new Handler();
    int mTabId = 0;
    
    //BEAD ad
    private Bead mBeadExit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //actionBar.setDisplayShowTitleEnabled(false); 

        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_menu_recent_history).setText(R.string.tab1).setTabListener(
        		new MyTabListener<DateFragment>(this, "tab1", DateFragment.class)));

        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_menu_cc).setText(R.string.tab2).setTabListener(
        		new MyTabListener<CategoryFragment>(this, "tab2", CategoryFragment.class)));

        //アクセシビリティ設定が有効でないときは、設定画面を起動する。
        SharedPreferences pref = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        if(!(pref.getBoolean(AVAILABLE_KEY, false))){
        	startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        initProgressDialog();
        
        //BEAD ad
        mBeadExit = Bead. createExitInstance("df90e2a0ddfc86512087815ead362a0d1cee1734df709cb4", ContentsOrientation.Auto);
        mBeadExit.requestAd(this);
    }
    
    void initProgressDialog(){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.dialog_progress_refresh));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }
    
    protected void onResume(){
        super.onResume();

        //UPナビゲーションで戻ってきたときは、元のタブを表示する
        SharedPreferences pref = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        if((pref.getBoolean("navigation", false))){
        	ActionBar actionBar = getSupportActionBar();
            actionBar.setSelectedNavigationItem(1);
            
            //設定を元に戻す
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("navigation", false);
            editor.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle presses on the action bar items
    	switch (item.getItemId()) {
    	case R.id.action_refresh:
    		refresh();

    		if(DEBUG){
    			displayNotificationArea();
    		}

    		return true;
    		
    	case R.id.action_deleteAll:
    		deleteAll();
    		return true;
    		
    	case R.id.action_help:
    		displayHelp();
    		return true;
    		
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    //for test
    private void displayNotificationArea(){
        Intent intent = new Intent(this, MessagePickerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
        		this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(contentIntent);
        builder.setTicker("テストティッカー");
        builder.setSmallIcon(R.drawable.icon24);
        builder.setContentTitle("テストタイトル");
        builder.setContentText("テストテキスト");
        builder.setAutoCancel(true);
        
        NotificationManager manager = (NotificationManager)getSystemService("notification");
        manager.notify(R.string.app_name, builder.build());
    }
    
    void refresh(){
    	//プログレスダイアログ表示
    	mProgressDialog.show();

		ActionBar bar = getSupportActionBar();
		int id = bar.getSelectedNavigationIndex();
		
		if(id == 0){
    		Fragment fragment = this.getSupportFragmentManager().findFragmentByTag("tab1");
    		if(fragment instanceof DateFragment){
    			((DateFragment)fragment).refreshMessage(false);
    		}    			
		}
		else if(id == 1){
    		Fragment fragment = this.getSupportFragmentManager().findFragmentByTag("tab2");
    		if(fragment instanceof CategoryFragment){
    			((CategoryFragment)fragment).refreshMessage(false);
    		}
		}
		else{
			//nothing to do
		}
		
		//プログレスダイアログ表示のためのウェイト用スレッド
        Thread thread = new Thread(runnable);
        thread.start();
    }
    
    Runnable runnable = new Runnable() {
        public void run() {
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
        	handler.sendMessage(new Message());
        }
    };
    
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            mProgressDialog.dismiss();
        };
    };


    void deleteAll(){
    	//メッセージが0個の場合は何もしない
		Fragment fragment = this.getSupportFragmentManager().findFragmentByTag("tab1");
		if(fragment instanceof DateFragment){
			int num = ((DateFragment)fragment).getMessageNum();
			if(num == 0){
				return;
			}
		}
		
    	new AlertDialog.Builder(this)
    	.setTitle(R.string.dialog_confirm_title)
    	.setMessage(getString(R.string.dialog_delete_all_confirm))
    	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				removeDB();
			}
		})
		.setNegativeButton(R.string.ng, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//何もしない
			}
		})
		.show();
    }
    
    private void removeDB(){
		ActionBar bar = getSupportActionBar();
		int id = bar.getSelectedNavigationIndex();
		
		if(id == 0){
    		Fragment fragment = this.getSupportFragmentManager().findFragmentByTag("tab1");
    		if(fragment instanceof DateFragment){
    			((DateFragment)fragment).removeAll();
    			((DateFragment)fragment).clearView();
    		}
		}
		else if(id == 1){
    		Fragment fragment = this.getSupportFragmentManager().findFragmentByTag("tab2");
    		if(fragment instanceof CategoryFragment){
    			((CategoryFragment)fragment).removeAll();
    			((CategoryFragment)fragment).clearView();
    		}    			
		}
    }
    
    void displayHelp(){
    	String url = "http://neging01.web.fc2.com/android/keepunread/top.html";
    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    	startActivity(intent);
    }
    
    @Override
    protected void onPause(){
    	super.onPause();

    	//タブを覚えておく
    	ActionBar bar = getSupportActionBar();
    	mTabId = bar.getSelectedNavigationIndex();
    	//Log.d(TAG, "mTabId = " + mTabId);
    }
    
    void release(){
		Fragment tab1 = this.getSupportFragmentManager().findFragmentByTag("tab1");
		if(tab1 instanceof DateFragment){
			((DateFragment)tab1).clearView();
		}
		
		Fragment tab2 = this.getSupportFragmentManager().findFragmentByTag("tab2");
		if(tab2 instanceof CategoryFragment){
			((CategoryFragment)tab2).clearView();
		}
    }
    
    public static String getDateAllString(long time){
        SimpleDateFormat sdf;
        if(Locale.JAPAN.equals(Locale.getDefault())) {
            sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");	                	
        }
        else{
        	sdf = new SimpleDateFormat("MMM d, yyyy, HH:mm");
        }

        return sdf.format(time);
    }
    
    public static String getDateString(long time){
        SimpleDateFormat sdf;
        if(Locale.JAPAN.equals(Locale.getDefault())) {
        	sdf = new SimpleDateFormat("MM/dd");	                	
        }
        else{
        	sdf = new SimpleDateFormat("MMM d");
        }

        return sdf.format(time);
    }

    public static String getTimeString(long time){
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("HH:mm");	                	

        return sdf.format(time);
    }
    
    @Override
	public void onDestroy(){
    	super.onDestroy();
    	
    	// 広告終了
    	if(mBeadExit != null){
    		mBeadExit.endAd();
    	}
    	
    	deleteCache(getCacheDir());
    }
    
    public static boolean deleteCache(File dir) {
        if(dir==null) {
            return false;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteCache(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    //for 2.3
    //これがないと2.3でFragmentの内容がActionBarにかぶるため追加
    public static int getContentViewCompat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ?
                   android.R.id.content : R.id.action_bar_activity_content;
    }
    
    @Override
    public void onStart() {
      super.onStart();
      EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
      super.onStop();
      EasyTracker.getInstance(this).activityStop(this);
    }
    
    @Override
    public void onBackPressed() {
    	// 広告ダイアログ表示
    	mBeadExit.showAd(this);
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
    			mActivity.getSupportFragmentManager().beginTransaction().add(getContentViewCompat(), mFragment, mTag).commit();
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
