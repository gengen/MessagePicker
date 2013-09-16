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
    //���߂ɓǂݍ��ރ��b�Z�[�W��
    public static final int FIRST_MESSAGE_NUM = 10;
    //�ǉ��œǂݍ��܂�郁�b�Z�[�W��
    public static final int ADD_MESSAGE_NUM = 3;
    //���܂ŉ���ǂݍ��܂ꂽ��
    public static int mTimes = 0;
    //���b�Z�[�W��
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
        
        //�܂��͌��݂̃��b�Z�[�W�̑������擾
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable";
        Cursor c = db.rawQuery(query, null);
        mMsgNum = c.getCount();
        c.close();
        
        //���߂�FIRST_MESSAGE_NUM�������擾
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
                //�����͕ϊ����Ă���i�[
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String date = sdf.format(time);
                Log.d(TAG, "date = " + date);
                //TODO:���݂̎�������̍����ɕϊ�
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
        //�\������ԍŌ�̃��b�Z�[�W�ɂ���
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
			//TODO:���̂܂܂��Ə�ɃX�N���[�����Ă��X�V����Ă��܂����߁A�Ȃ�炩�̐ݒ�����Ȃ��Ƃ����Ȃ��B�Ƃ肠�������f�B
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
	                //�����͕ϊ����Ă���i�[
	                String timeStr = c.getString(3);
	                long time = Long.parseLong(timeStr);
	                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
	                String date = sdf.format(time);
	                Log.d(TAG, "date = " + date);
	                //TODO:���݂̎�������̍����ɕϊ�
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
