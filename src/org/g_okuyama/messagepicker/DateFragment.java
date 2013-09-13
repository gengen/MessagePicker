package org.g_okuyama.messagepicker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;

public class DateFragment extends Fragment {
    public static final String TAG = "MessagePicker";
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mMessageList = null;
    PullToRefreshListView mListView = null;

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
        String query = "select * from logtable order by time;";
        Cursor c = db.rawQuery(query, null);
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
        //ListView listview = (ListView)v.findViewById(R.id.message_list_date);
        mListView = (PullToRefreshListView)v.findViewById(R.id.message_list_date);
        mListView.setAdapter(adapter);
        mListView.setOnRefreshListener(new OnRefreshListener() {
        	@Override
        	public void onRefresh() {
        		// pull to refresh時に呼ばれる
        		new GetDataTask().execute();
       		} 
        });
    }
    
    private class GetDataTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... params) {
        	//TODO:更新処理を入れる
            // Simulates a background job.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                ;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
        	//TODO:更新処理を入れる
            //mListItems.addFirst("Added after refresh...");

            // Call onRefreshComplete when the list has been refreshed.
            mListView.onRefreshComplete();

            super.onPostExecute(result);
        }
    }
}
