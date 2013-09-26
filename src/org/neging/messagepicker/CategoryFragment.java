package org.neging.messagepicker;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CategoryFragment extends Fragment {
	public static final int REQUEST_CODE = 1;
	
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mNameList = null;
    
    View mView;
    //メッセージ数
    public static int mMsgNum = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mView = inflater.inflate(R.layout.category_fragment, container, false);

        setMessageList();

        return mView;
    }

    void setMessageList(){
        if(mHelper == null){
            mHelper = new DatabaseHelper(getActivity());
        }
        
        //現在のメッセージの総数を取得
        mMsgNum = getMessageNum();
        
        mNameList = new ArrayList<MessageListData>();

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable order by name;";
        Cursor c = db.rawQuery(query, null);
        int rowcount = c.getCount();
        String prevName = "";

        if(rowcount != 0){
            c.moveToLast();

            for(int i = 0; i < rowcount; i++){
                MessageListData logitem = new MessageListData();
                String name = c.getString(1);
                if(name.equals(prevName)){
                	c.moveToPrevious();
                	continue;
                }
                logitem.setName(name);
                int dbid = c.getInt(0);
                logitem.setDBID(dbid);
                prevName = name;
                logitem.setContents(c.getString(2));
                //日時は変換してから格納
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                String date = MessagePickerActivity.getDateString(time);

                //今日の日付と比較
                long current = System.currentTimeMillis();
                String today = MessagePickerActivity.getDateString(current);
                if(date.equals(today)){
                	date = MessagePickerActivity.getTimeString(time);
                }
                
                logitem.setDate(date);

                mNameList.add(logitem);

                c.moveToPrevious();
            }
        }
        c.close();
        db.close();
        
        CategoryArrayAdapter adapter = new CategoryArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, mNameList);
        ListView listview = (ListView)mView.findViewById(R.id.message_list_category);

        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new ClickAdapter());
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
    
    public void refreshMessage(boolean forceFlag){
		int num = getMessageNum();
		if((num >= mMsgNum) || forceFlag){
			clearView();
			setMessageList();
			mMsgNum = num;
		}
    }
    
    public void clearView(){
    	//adapterをクリア
        ListView listview = (ListView)mView.findViewById(R.id.message_list_category);
        CategoryArrayAdapter adapter = (CategoryArrayAdapter)listview.getAdapter();
        adapter.clear();
        mNameList = null;
        //mCurrentPos = -1;
        //mTimes = 0;
        mMsgNum = 0;
    }
    
    void removeAll(){
    	SQLiteDatabase db = mHelper.getWritableDatabase();
    	db.delete("logtable", null, null);
    	db.close();
    }
    
    private class ClickAdapter implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
			//リスト詳細を表示するアクティビティを起動
        	Intent intent = new Intent(getActivity(), EachMessageListActivity.class);
        	MessageListData item = mNameList.get(pos);
        	String name = item.getName();
        	intent.putExtra("name", name);
        	startActivityForResult(intent, REQUEST_CODE);
		}
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	if(requestCode == REQUEST_CODE){
    		if(resultCode == EachMessageListActivity.RESPONSE_DELETE){
    			//カテゴリが削除されたため、表示をクリアする
    			refreshMessage(true);
    		}
    	}
    }
}

