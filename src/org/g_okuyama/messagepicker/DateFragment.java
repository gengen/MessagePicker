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
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DateFragment extends Fragment {
    public static final String TAG = "MessagePicker";
    //初めに読み込むメッセージ数
    public static final int FIRST_MESSAGE_NUM = 10;
    //追加で読み込まれるメッセージ数
    public static final int ADD_MESSAGE_NUM = 3;
    //今まで何回読み込まれたか
    public static int mTimes = 0;
    //メッセージ数
    public static int mMsgNum = 0;
    
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mMessageList = null;
    
    static boolean mFlag = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.date_fragment, container, false);

        setMessageList(v);

        return v;
    }

    void setMessageList(View v){
        if(mHelper == null){
            mHelper = new DatabaseHelper(getActivity());
        }
        
        //まずは現在のメッセージの総数を取得
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable";
        Cursor c = db.rawQuery(query, null);
        mMsgNum = c.getCount();
        c.close();
        
        //初めはFIRST_MESSAGE_NUM分だけ取得
        query = "select * from logtable order by time desc limit " + FIRST_MESSAGE_NUM;
        c = db.rawQuery(query, null);
        int rowcount = c.getCount();

        mMessageList = new ArrayList<MessageListData>();

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
        OverScrollListView listview = (OverScrollListView)v.findViewById(R.id.message_list_date);
        listview.setAdapter(adapter);
        //表示を一番最後のメッセージにする
        listview.setSelection(rowcount-1);
        listview.setOnScrollListener(new OnScrollListener(){
			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onScrollStateChanged(AbsListView arg0, int state) {
				if(state == 0){
					mFlag = false;
				}
			}        	
        });
    }
    
    public static class OverScrollListView extends ListView {
    	Context mContext;

		public OverScrollListView(Context context, AttributeSet attrs) {
			super(context, attrs);
			mContext = context;
		}
		


	    @Override
	    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
	    		int scrollY, int scrollRangeX, int scrollRangeY,
	    		int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

	        super.overScrollBy(0, deltaY, 0, scrollY, 0, scrollRangeY, 0, maxOverScrollY, isTouchEvent);
	        return true;
	    }


		@Override
		protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
			Log.d(TAG, "onOverScrolled");
			//TODO:このままだと上にスクロールしても更新されてしまうため、なんらかの設定を入れないといけない。とりあえず中断。
			Log.d(TAG, "x = " + scrollX + ", y = " + scrollY + ", xx = " + clampedX + ", yy =" + clampedY);
			if(mFlag == false){
				mFlag = true;
				if(mMsgNum > (FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes))){
					addList();
					mTimes++;
				}
			}
		}

	    void addList(){
	    	DatabaseHelper helper = new DatabaseHelper(mContext);
	        SQLiteDatabase db = helper.getWritableDatabase();
	        int offset = FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes);
	        String query = "select * from logtable order by time desc limit " + ADD_MESSAGE_NUM + " offset " + offset;
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

	    			MessageArrayAdapter adapter = (MessageArrayAdapter)getAdapter();
	                adapter.insert(logitem, 0);

	                c.moveToPrevious();
	            }
	        }
	        c.close();
	        db.close();
	    }	    
    }
}
