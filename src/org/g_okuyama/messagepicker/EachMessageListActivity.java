package org.g_okuyama.messagepicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class EachMessageListActivity extends Activity {
    public static final String TAG = "MessagePicker";

    //���߂ɓǂݍ��ރ��b�Z�[�W��
    public static final int FIRST_MESSAGE_NUM = 5;
    //�ǉ��œǂݍ��܂�郁�b�Z�[�W��
    public static final int ADD_MESSAGE_NUM = 3;
    //���܂ŉ���ǂݍ��܂ꂽ��
    public static int mTimes = 0;
    //���b�Z�[�W��
    public static int mMsgNum = 0;
    //���݂̕\���ʒu
    public static int mCurrentPos = -1;
    static boolean mFlag = false;

    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mEachList = null;
    
    static String mName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.each_message_list);
        
        Bundle extras = getIntent().getExtras();
        mName = extras.getString("name");
        Log.d(TAG, "name = " + mName);
        
        setEachList();
    }
    
    void setEachList(){
        if(mHelper == null){
            mHelper = new DatabaseHelper(this);
        }
        
        //���݂̃��b�Z�[�W�̑������擾
        mMsgNum = getMessageNum();

        mEachList = new ArrayList<MessageListData>();
    	
        SQLiteDatabase db = mHelper.getWritableDatabase();
    	String query = "select * from logtable where name = ? order by time desc limit " + FIRST_MESSAGE_NUM;
    	Cursor c = db.rawQuery(query, new String[]{mName});

        int rowcount = c.getCount();
        
        Log.d(TAG, "num = " + mMsgNum);
        Log.d(TAG, "count = " + rowcount);
        
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
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String date = sdf.format(time);
                //Log.d(TAG, "date = " + date);
                //TODO:���݂̎�������̍����ɕϊ�
                logitem.setDate(date);

                mEachList.add(logitem);

                c.moveToPrevious();
            }
        }
        c.close();
        db.close();
        
        MessageArrayAdapter adapter = new MessageArrayAdapter(this, android.R.layout.simple_list_item_1, mEachList);
        OverScrollListView listview = (OverScrollListView)findViewById(R.id.each_list);
        listview.setAdapter(adapter);
        //�\������ԍŌ�̃��b�Z�[�W�ɂ���
        listview.setSelection(rowcount-1);
        listview.setOnItemLongClickListener(new LongClickAdapter());
        
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
    
    private class LongClickAdapter implements OnItemLongClickListener{
    	int position = -1;
    	
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
			position = pos;

			new AlertDialog.Builder(EachMessageListActivity.this)
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
        	
    	new AlertDialog.Builder(EachMessageListActivity.this)
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
    	//adapter���N���A
        OverScrollListView listview = (OverScrollListView)findViewById(R.id.each_list);
        MessageArrayAdapter adapter = (MessageArrayAdapter)listview.getAdapter();
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
        MessageArrayAdapter adapter = (MessageArrayAdapter)listview.getAdapter();
        MessageListData logitem = adapter.getItem(position);
        
    	int id = logitem.getDBID();

    	db.delete("logtable", "rowid = ?", new String[]{Integer.toString(id)});
    	db.close();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        clearView();
    }
    
    public static class OverScrollListView extends ListView {
    	Context mContext;

		public OverScrollListView(Context context, AttributeSet attrs) {
			super(context, attrs);
			mContext = context;
		}
		
		@Override
		protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
			Log.d(TAG, "OverScroll");
			//��ԏオ�\������Ă���Ƃ��̂݃��b�Z�[�W��ǉ�����
			if(mFlag == false && mCurrentPos == 0){
				Log.d(TAG, "add");
				if(mMsgNum > (FIRST_MESSAGE_NUM + (ADD_MESSAGE_NUM * mTimes))){
					Log.d(TAG, "add2");
					mFlag = true;
					addMessageList();
					mTimes++;
				}
			}
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
