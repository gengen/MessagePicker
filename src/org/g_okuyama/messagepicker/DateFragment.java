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
    //���߂ɓǂݍ��ރ��b�Z�[�W��
    public static final int FIRST_MESSAGE_NUM = 10;
    //�ǉ��œǂݍ��܂�郁�b�Z�[�W��
    public static final int ADD_MESSAGE_NUM = 5;
    //���܂ŉ���ǂݍ��܂ꂽ��
    public static int mTimes = 0;
    //���b�Z�[�W��
    public static int mMsgNum = 0;
    //���݂̕\���ʒu
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
        
        //���݂̃��b�Z�[�W�̑������擾
        mMsgNum = getMessageNum();
        
        mMessageList = new ArrayList<MessageListData>();

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "";
        //���߂�FIRST_MESSAGE_NUM�������擾
        query = "select * from logtable order by time desc limit " + FIRST_MESSAGE_NUM;        	
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

                mMessageList.add(logitem);

                c.moveToPrevious();
            }
        }
        c.close();
        db.close();
        
        MessageArrayAdapter adapter = new MessageArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, mMessageList);
        OverScrollListView listview = (OverScrollListView)mView.findViewById(R.id.message_list_date);
        listview.setAdapter(adapter);
        //�\������ԍŌ�̃��b�Z�[�W�ɂ���
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
				//��ԏ�ɕ\������Ă��郁�b�Z�[�W�̃|�W�V�������o���Ă���
				mCurrentPos = firstVisibleItem;
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(scrollState == 0){
					mFlag = false;
				}
			}        	
        });

        //�X�V�{�^��
        Button button = (Button)mView.findViewById(R.id.refresh_tab1);
        button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
		    	//TODO:�X�V�_�C�A���O�\���B�X�V���Ȃ��Ă�1�b���x�\�������ق��������H
				
				int num = getMessageNum();
				//TODO:�����[�X���͂͂���
				//if(num >= mMsgNum){
					refreshMessage();
					mMsgNum = num;
				//}
			}
        });
    }
    
    //���b�Z�[�W�̍X�V
    void refreshMessage(){
        clearView();
        setMessageList();
    }
    
    void clearView(){
    	//adapter���N���A
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
			//��ԏオ�\������Ă���Ƃ��̂݃��b�Z�[�W��ǉ�����
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
	                //�����͕ϊ����Ă���i�[
	                String timeStr = c.getString(3);
	                long time = Long.parseLong(timeStr);
	                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
	                String date = sdf.format(time);
	                //TODO:���݂̎�������̍����ɕϊ�
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
