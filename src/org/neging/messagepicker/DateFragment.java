package org.neging.messagepicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.widget.ListView;

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

        initFields();
        setMessageList();

        return mView;
    }
    
    void initFields(){
    	mTimes = 0;
    	mMsgNum = 0;
    	mCurrentPos = -1;
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
                int dbid = c.getInt(0);
                logitem.setDBID(dbid);
                logitem.setName(c.getString(1));
                logitem.setContents(c.getString(2));
                //�����͕ϊ����Ă���i�[
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                String date = MessagePickerActivity.getDateString(time);
                //Log.d(TAG, "date = " + date);

                //�����̓��t�Ɣ�r
                long current = System.currentTimeMillis();
                String today = MessagePickerActivity.getDateString(current);
                //�����ł���Ύ��Ԃ̂ݕ\��
                if(date.equals(today)){
                	date = MessagePickerActivity.getTimeString(time);
                }
                else{
                	date = MessagePickerActivity.getDateAllString(time);
                }
                
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
        listview.setOnItemLongClickListener(new LongClickAdapter());
        
        setListener();
    }
    
    public int getMessageNum(){
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
    }
    
    //���b�Z�[�W�̍X�V
    public void refreshMessage(boolean forceFlag){
		int num = getMessageNum();
		if((num >= mMsgNum) || forceFlag){
			clearView();
			setMessageList();
			mMsgNum = num;
		}
    }
    
    public void clearView(){
    	//adapter���N���A
        OverScrollListView listview = (OverScrollListView)mView.findViewById(R.id.message_list_date);
        MessageArrayAdapter adapter = (MessageArrayAdapter)listview.getAdapter();
        adapter.clear();
        mMessageList = null;
        mCurrentPos = -1;
        mTimes = 0;
        mMsgNum = 0;
    }
    
    @Override
    public void onPause() {
        super.onPause();
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
			//Log.d(TAG, "onOverScrolled");
			
			if(isProcessed){
				return;
			}
			
			isProcessed = true;
			
			//��ԏオ�\������Ă���Ƃ��̂݃��b�Z�[�W��ǉ�����
			if(mFlag == false && mCurrentPos == 0){
				int currentNum = FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes);
				Log.d(TAG, "num = " + currentNum);
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
	        String query = "select * from logtable order by time desc limit " + ADD_MESSAGE_NUM + " offset " + offset;
	        Cursor c = db.rawQuery(query, null);
	        int rowcount = c.getCount();

	        if(rowcount != 0){
	            c.moveToFirst();

	            for(int i = 0; i < rowcount; i++){
	                MessageListData logitem = new MessageListData();
	                int dbid = c.getInt(0);
	                logitem.setDBID(dbid);
	                logitem.setName(c.getString(1));
	                logitem.setContents(c.getString(2));
	                //�����͕ϊ����Ă���i�[
	                String timeStr = c.getString(3);
	                long time = Long.parseLong(timeStr);
	                String date = MessagePickerActivity.getDateString(time);

	                //�����̓��t�Ɣ�r
	                long current = System.currentTimeMillis();
	                String today = MessagePickerActivity.getDateString(current);
	                //�����ł���Ύ��Ԃ̂ݕ\��
	                if(date.equals(today)){
	                	date = MessagePickerActivity.getTimeString(time);
	                }
	                else{
	                	date = MessagePickerActivity.getDateAllString(time);
	                }
	                
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
    
    private class LongClickAdapter implements OnItemLongClickListener{
    	int position = -1;
    	
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
			position = pos;

			new AlertDialog.Builder(getActivity())
				.setTitle(R.string.list_alert_select)
				.setItems(R.array.list_alert_array, new DialogInterface.OnClickListener() {
				
					public void onClick(DialogInterface dialog, int item) {
						switch(item){
						case 0://�폜
							delete(position);
							break;
						case 1://�L�����Z��
							break;
						}
					}
				}).show();				
			
			return true;
		}
    }
    
    private void delete(int position){
    	final int pos = position;
        	
    	new AlertDialog.Builder(getActivity())
        	.setTitle(R.string.dialog_confirm_title)
        	.setMessage(getString(R.string.dialog_delete_confirm))
        	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				//���O�폜
    				removeData(pos);
    				refreshMessage(true);
    			}
    		})
    		.setNegativeButton(R.string.ng, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				//�������Ȃ�
    			}
    		})
    		.show();    
    }
    
    private void removeData(int position){
    	SQLiteDatabase db = mHelper.getWritableDatabase();
        
    	//MessageListData logitem = mMessageList.get(position);
        OverScrollListView listview = (OverScrollListView)mView.findViewById(R.id.message_list_date);
        MessageArrayAdapter adapter = (MessageArrayAdapter)listview.getAdapter();
        MessageListData logitem = adapter.getItem(position);
        
    	int id = logitem.getDBID();

    	db.delete("logtable", "rowid = ?", new String[]{Integer.toString(id)});
    	db.close();
    }
    
    void removeAll(){
    	SQLiteDatabase db = mHelper.getWritableDatabase();
    	db.delete("logtable", null, null);
    	db.close();
    }
}
