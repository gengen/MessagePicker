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
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable order by time desc limit 3;";
        Cursor c = db.rawQuery(query, null);
        int rowcount = c.getCount();

        mMessageList = new ArrayList<MessageListData>();

        if(rowcount != 0){
            c.moveToLast();

            for(int i = 0; i < rowcount; i++){
                MessageListData logitem = new MessageListData();
                logitem.setName(c.getString(1));
                logitem.setContents(c.getString(2));
                //“úŽž‚Í•ÏŠ·‚µ‚Ä‚©‚çŠi”[
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String date = sdf.format(time);
                Log.d(TAG, "date = " + date);
                //TODO:Œ»Ý‚ÌŽž‚©‚ç‚Ì·•ª‚É•ÏŠ·
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
        listview.setSelection(rowcount-1);
        listview.setOnScrollListener(new OnScrollListener(){

			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
				if(arg1 == 0){
					mFlag = false;
				}
			}
        	
        });
    }
    
    public static class OverScrollListView extends ListView {
    	Context mContext;
    	//boolean flag = false;

		public OverScrollListView(Context context, AttributeSet attrs) {
			super(context, attrs);
			mContext = context;
		}

		@Override
		protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
			Log.d(TAG, "onOverScrolled");
			if(mFlag == false){
				mFlag = true;
				addList();
			}
			/*
			MessageArrayAdapter adapter = (MessageArrayAdapter)getAdapter();
            MessageListData logitem = new MessageListData();
            logitem.setName("hoge");
            adapter.insert(logitem, 0);
            */
		}

	    void addList(){
	    	DatabaseHelper helper = new DatabaseHelper(mContext);
	        SQLiteDatabase db = helper.getWritableDatabase();
	        String query = "select * from logtable order by time desc limit 3 offset 2;";
	        Cursor c = db.rawQuery(query, null);
	        int rowcount = c.getCount();

	        if(rowcount != 0){
	            c.moveToLast();

	            for(int i = 0; i < rowcount; i++){
	                MessageListData logitem = new MessageListData();
	                logitem.setName(c.getString(1));
	                logitem.setContents(c.getString(2));
	                //“úŽž‚Í•ÏŠ·‚µ‚Ä‚©‚çŠi”[
	                String timeStr = c.getString(3);
	                long time = Long.parseLong(timeStr);
	                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
	                String date = sdf.format(time);
	                Log.d(TAG, "date = " + date);
	                //TODO:Œ»Ý‚ÌŽž‚©‚ç‚Ì·•ª‚É•ÏŠ·
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
