package org.g_okuyama.messagepicker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DateFragment extends Fragment {
    public static final String TAG = "MessagePicker";
    //初めに読み込むメッセージ数
    public static final int FIRST_MESSAGE_NUM = 10;
    //追加で読み込まれるメッセージ数
    public static final int ADD_MESSAGE_NUM = 5;
    //今まで何回読み込まれたか
    public static int mTimes = 0;
    //メッセージ数
    public static int mMsgNum = 0;
    //現在の表示位置
    public static int mCurrentPos = -1;
    
    View mView;
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mMessageList = null;
    
    static boolean mFlag = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.date_fragment, container, false);

        setMessageList();

        return mView;
    }

    void setMessageList(){
        if(mHelper == null){
            mHelper = new DatabaseHelper(getActivity());
        }
        
        //現在のメッセージの総数を取得
        mMsgNum = getMessageNum();
        
        mMessageList = new ArrayList<MessageListData>();

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "";
        //初めはFIRST_MESSAGE_NUM分だけ取得
        query = "select * from logtable order by time desc limit " + FIRST_MESSAGE_NUM;        	
        Cursor c = db.rawQuery(query, null);
        int rowcount = c.getCount();

        if(rowcount != 0){
            c.moveToLast();

            for(int i = 0; i < rowcount; i++){
                MessageListData logitem = new MessageListData();
                logitem.setName(c.getString(1));
                logitem.setContents(c.getString(2));
                //日時は変換してから格納
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String date = sdf.format(time);
                Log.d(TAG, "date = " + date);
                //TODO:現在の時刻からの差分に変換
                logitem.setDate(date);

                mMessageList.add(logitem);

                c.moveToPrevious();
            }
        }
        c.close();
        db.close();
        
        MessageArrayAdapter adapter = new MessageArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, mMessageList);
        OverScrollListView listview = (OverScrollListView)mView.findViewById(R.id.message_list_date);
        listview.setAdapter(adapter);
        //表示を一番最後のメッセージにする
        listview.setSelection(rowcount-1);
        
        setListener();
    }
    
    int getMessageNum(){
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable";
        Cursor c = db.rawQuery(query, null);
        int num = c.getCount();
        c.close();
        db.close();
        
        return num;
    }
    
    void setListener(){
        OverScrollListView listview = (OverScrollListView)mView.findViewById(R.id.message_list_date);
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

        //更新ボタン
        Button button = (Button)mView.findViewById(R.id.refresh_tab1);
        button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
		    	//TODO:更新ダイアログ表示。更新がなくても1秒程度表示したほうがいい？
				
				int num = getMessageNum();
				//TODO:リリース時ははずす
				//if(num >= mMsgNum){
					refreshMessage();
					mMsgNum = num;
				//}
			}
        });
    }
    
    //メッセージの更新
    void refreshMessage(){
        clearView();
        setMessageList();
    }
    
    void clearView(){
    	//adapterをクリア
        OverScrollListView listview = (OverScrollListView)mView.findViewById(R.id.message_list_date);
        MessageArrayAdapter adapter = (MessageArrayAdapter)listview.getAdapter();
        adapter.clear();
        mMessageList = null;
        mCurrentPos = -1;
        mTimes = 0;
    }
    
    public static class OverScrollListView extends ListView {
    	Context mContext;

		public OverScrollListView(Context context, AttributeSet attrs) {
			super(context, attrs);
			mContext = context;
		}
		
		@Override
		protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
			Log.d(TAG, "onOverScrolled");
			//一番上が表示されているときのみメッセージを追加する
			if(mFlag == false && mCurrentPos == 0){
				if(mMsgNum > (FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes))){
					mFlag = true;
					addList();
					mTimes++;
				}
			}
		}

	    void addList(){
	    	DatabaseHelper helper = new DatabaseHelper(mContext);
	        SQLiteDatabase db = helper.getWritableDatabase();
	        int offset = FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes);
	        Log.d(TAG, "offset = " + offset);
	        String query = "select * from logtable order by time desc limit " + ADD_MESSAGE_NUM + " offset " + offset;
	        Cursor c = db.rawQuery(query, null);
	        int rowcount = c.getCount();

	        if(rowcount != 0){
	            c.moveToFirst();

	            for(int i = 0; i < rowcount; i++){
	                MessageListData logitem = new MessageListData();
	                logitem.setName(c.getString(1));
	                logitem.setContents(c.getString(2));
	                //日時は変換してから格納
	                String timeStr = c.getString(3);
	                long time = Long.parseLong(timeStr);
	                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
	                String date = sdf.format(time);
	                //TODO:現在の時刻からの差分に変換
	                logitem.setDate(date);

	    			MessageArrayAdapter adapter = (MessageArrayAdapter)getAdapter();
	                adapter.insert(logitem, 0);

	                c.moveToNext();
	            }
	        }
	        c.close();
	        db.close();
	    }	    
    }
}
