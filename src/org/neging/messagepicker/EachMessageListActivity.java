package org.neging.messagepicker;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class EachMessageListActivity extends ActionBarActivity{
    public static final String TAG = "MessagePicker";
    public static final int RESPONSE_DELETE = 1;

    //初めに読み込むメッセージ数
    public static final int FIRST_MESSAGE_NUM = 10;
    //追加で読み込まれるメッセージ数
    public static final int ADD_MESSAGE_NUM = 2;
    //今まで何回読み込まれたか
    public static int mTimes = 0;
    //メッセージ数
    public static int mMsgNum = 0;
    //現在の表示位置
    public static int mCurrentPos = -1;
    static String mName;
    static boolean mFlag = false;

    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mEachList = null;
    
    ProgressDialog mProgressDialog = null;
    Handler mHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.each_message_list);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        Bundle extras = getIntent().getExtras();
        mName = extras.getString("name");
        setTitle(mName);
        
        initFields();
        setLayout();
        setEachList();
        initProgressDialog();
    }
    
    void initFields(){
    	mTimes = 0;
    	mMsgNum = 0;
    	mCurrentPos = -1;
    }
    
    void setLayout(){
    	LinearLayout layout = (LinearLayout)findViewById(R.id.container);
    	layout.setBackgroundResource(R.drawable.background);
    }
    
    void initProgressDialog(){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.dialog_progress_refresh));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }
    
    void setEachList(){
        if(mHelper == null){
            mHelper = new DatabaseHelper(this);
        }
        
        //現在のメッセージの総数を取得
        mMsgNum = getMessageNum();

        mEachList = new ArrayList<MessageListData>();
    	
        SQLiteDatabase db = mHelper.getWritableDatabase();
    	String query = "select * from logtable where name = ? order by time desc limit " + FIRST_MESSAGE_NUM;
    	Cursor c = db.rawQuery(query, new String[]{mName});

        int rowcount = c.getCount();
        
        //Log.d(TAG, "num = " + mMsgNum);
        //Log.d(TAG, "count = " + rowcount);
        
        if(rowcount != 0){
            c.moveToLast();

            for(int i = 0; i < rowcount; i++){
                MessageListData logitem = new MessageListData();
                int dbid = c.getInt(0);
                logitem.setDBID(dbid);
                logitem.setName(c.getString(1));
                logitem.setContents(c.getString(2));
                //日時は変換してから格納
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                String date = MessagePickerActivity.getDateString(time);

                //今日の日付と比較
                long current = System.currentTimeMillis();
                String today = MessagePickerActivity.getDateString(current);
                //今日であれば時間のみ表示
                if(date.equals(today)){
                	date = MessagePickerActivity.getTimeString(time);
                }
                else{
                	date = MessagePickerActivity.getDateAllString(time);
                }
                
                logitem.setDate(date);

                mEachList.add(logitem);

                c.moveToPrevious();
            }
        }
        c.close();
        db.close();
        
        EachMessageArrayAdapter adapter = new EachMessageArrayAdapter(this, android.R.layout.simple_list_item_1, mEachList);
        OverScrollListView listview = (OverScrollListView)findViewById(R.id.each_list);
        listview.setAdapter(adapter);
        //表示を一番最後のメッセージにする
        listview.setSelection(rowcount-1);
        listview.setOnItemLongClickListener(new LongClickAdapter());
        //これがないとスクロール時にちらつく
        listview.setScrollingCacheEnabled(false); 
        
        listview.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				//一番上に表示されているメッセージのポジションを覚えておく
				mCurrentPos = firstVisibleItem;
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(scrollState == 0){
					mFlag = false;
				}
			}        	
        });
    }
    
    private class LongClickAdapter implements OnItemLongClickListener{
    	int position = -1;
    	
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
			position = pos;

			new AlertDialog.Builder(EachMessageListActivity.this)
				.setTitle(R.string.list_alert_select)
				.setItems(R.array.list_alert_array, new DialogInterface.OnClickListener() {
				
					public void onClick(DialogInterface dialog, int item) {
						switch(item){
						case 0://削除
							delete(position);
							break;
						case 1://キャンセル
							break;
						}
					}
				}).show();				
			
			return true;
		}
    }
    
    private void delete(int position){
    	final int pos = position;
        	
    	new AlertDialog.Builder(EachMessageListActivity.this)
        	.setTitle(R.string.dialog_confirm_title)
        	.setMessage(getString(R.string.dialog_delete_confirm))
        	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				//ログ削除
    				removeData(pos);
    				refreshMessage(true);
    			}
    		})
    		.setNegativeButton(R.string.ng, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				//何もしない
    			}
    		})
    		.show();    
    }
    
    public void refreshMessage(boolean forceFlag){
		int num = getMessageNum();
		if((num >= mMsgNum) || forceFlag){
			clearView();
			setEachList();
			mMsgNum = num;
		}
    }
    
    public int getMessageNum(){
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable where name = ?;";
        Cursor c = db.rawQuery(query, new String[]{mName});
        int num = c.getCount();
        c.close();
        db.close();
        
        return num;
    }
    
    public void clearView(){
    	//adapterをクリア
        OverScrollListView listview = (OverScrollListView)findViewById(R.id.each_list);
        EachMessageArrayAdapter adapter = (EachMessageArrayAdapter)listview.getAdapter();
        adapter.clear();
        mEachList = null;
        mCurrentPos = -1;
        mTimes = 0;
        mMsgNum = 0;
    }
    
    private void removeData(int position){
    	SQLiteDatabase db = mHelper.getWritableDatabase();
        
    	//MessageListData logitem = mMessageList.get(position);
        OverScrollListView listview = (OverScrollListView)findViewById(R.id.each_list);
        EachMessageArrayAdapter adapter = (EachMessageArrayAdapter)listview.getAdapter();
        MessageListData logitem = adapter.getItem(position);
        
    	int id = logitem.getDBID();

    	db.delete("logtable", "rowid = ?", new String[]{Integer.toString(id)});
    	db.close();
    	
    	//メッセージが0のときは前の画面に戻る
    	if(getMessageNum() == 0){
            Intent intent = new Intent();
            setResult(RESPONSE_DELETE, intent);
        	finish();
    	}
    }
    
    @Override
    public void onPause() {
        super.onPause();
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
    		return true;
    		
    	case R.id.action_deleteAll:
    		deleteCategory();
    		return true;
        	
    	case R.id.action_help:
    		displayHelp();
    		return true;
    		
        default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    void refresh(){
    	//プログレスダイアログ表示
    	mProgressDialog.show();
    	
    	refreshMessage(false);
    	
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
    
    void deleteCategory(){
    	int num = getMessageNum();
    	if(num == 0){
    		return;
    	}
    	
    	new AlertDialog.Builder(this)
    	.setTitle(mName)
    	.setMessage(getString(R.string.dialog_delete_all_confirm))
    	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				removeCategoryDB();
			}
		})
		.setNegativeButton(R.string.ng, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//何もしない
			}
		})
		.show();
    }
    
    void removeCategoryDB(){
    	SQLiteDatabase db = mHelper.getWritableDatabase();
    	db.delete("logtable", "name = ?", new String[]{mName});
    	db.close();

    	//clearView();
    	
    	//前画面のActivityに戻る
        Intent intent = new Intent();
        setResult(RESPONSE_DELETE, intent);
    	finish();
    }
    
    void displayHelp(){
    	String url = "http://neging01.web.fc2.com/android/keepunread/top.html";
    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    	startActivity(intent);
    }
    
    @Override
    public boolean onSupportNavigateUp(){
    	//Log.d(TAG, "onSupportNavigateUp");
    	
		return super.onSupportNavigateUp();
    }
    
    public static class OverScrollListView extends ListView {
    	Context mContext;
    	boolean isProcessed = false;

		public OverScrollListView(Context context, AttributeSet attrs) {
			super(context, attrs);
			mContext = context;
		}
		
		@Override
		protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
			//Log.d(TAG, "OverScroll");
			
			if(isProcessed){
				return;
			}
			
			isProcessed = true;
			
			//一番上が表示されているときのみメッセージを追加する
			if(mFlag == false && mCurrentPos == 0){
				int currentNum = FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes);
				if(mMsgNum > currentNum){
					mFlag = true;
					addMessageList();
					mTimes++;
				}
			}
			
			isProcessed = false;
		}

	    void addMessageList(){
	    	DatabaseHelper helper = new DatabaseHelper(mContext);
	        SQLiteDatabase db = helper.getWritableDatabase();
	        int offset = FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes);
	        //Log.d(TAG, "offset = " + offset);
	        String query = "select * from logtable desc where name = ? order by time desc limit " + ADD_MESSAGE_NUM + " offset " + offset;
	        Cursor c = db.rawQuery(query, new String[]{mName});
	        int rowcount = c.getCount();

	        if(rowcount != 0){
	            c.moveToFirst();

	            for(int i = 0; i < rowcount; i++){
	                MessageListData logitem = new MessageListData();
	                int dbid = c.getInt(0);
	                logitem.setDBID(dbid);
	                logitem.setName(c.getString(1));
	                logitem.setContents(c.getString(2));
	                //日時は変換してから格納
	                String timeStr = c.getString(3);
	                long time = Long.parseLong(timeStr);
	                String date = MessagePickerActivity.getDateString(time);

	                //今日の日付と比較
	                long current = System.currentTimeMillis();
	                String today = MessagePickerActivity.getDateString(current);
	                //今日であれば時間のみ表示
	                if(date.equals(today)){
	                	date = MessagePickerActivity.getTimeString(time);
	                }
	                else{
	                	date = MessagePickerActivity.getDateAllString(time);
	                }
	                
	                
	                logitem.setDate(date);

	    			EachMessageArrayAdapter adapter = (EachMessageArrayAdapter)getAdapter();
	                adapter.insert(logitem, 0);

	                c.moveToNext();
	            }
	        }
	        c.close();
	        db.close();
	    }	    
    }
}
