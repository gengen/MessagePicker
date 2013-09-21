package org.g_okuyama.messagepicker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class CategoryFragment extends Fragment {
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mNameList = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.category_fragment, container, false);

        setMessageList(v);

        return v;
    }

    void setMessageList(View v){
        if(mHelper == null){
            mHelper = new DatabaseHelper(getActivity());
        }
        
        mNameList = new ArrayList<MessageListData>();

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable order by name;";
        Cursor c = db.rawQuery(query, null);
        int rowcount = c.getCount();
        String prevName = "";

        if(rowcount != 0){
            c.moveToFirst();

            for(int i = 0; i < rowcount; i++){
                MessageListData logitem = new MessageListData();
                String name = c.getString(1);
                if(name.equals(prevName)){
                	c.moveToNext();
                	continue;
                }
                logitem.setName(name);
                int dbid = c.getInt(0);
                logitem.setDBID(dbid);
                prevName = name;
                logitem.setContents(c.getString(2));
                //ì˙éûÇÕïœä∑ÇµÇƒÇ©ÇÁäiî[
                String timeStr = c.getString(3);
                long time = Long.parseLong(timeStr);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                String date = sdf.format(time);
                //Log.d(TAG, "date = " + date);
                //TODO:åªç›ÇÃéûçèÇ©ÇÁÇÃç∑ï™Ç…ïœä∑
                logitem.setDate(date);

                mNameList.add(logitem);

                c.moveToNext();
            }
        }
        c.close();
        db.close();
        
        MessageArrayAdapter adapter = new MessageArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, mNameList);
        ListView listview = (ListView)v.findViewById(R.id.message_list_category);

        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new ClickAdapter());

        //listview.setOnItemLongClickListener(new LongClickAdapter());
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
		//TODO:é¿ëï
    }
    
    public void clearView(){
		//TODO:é¿ëï    	
    }
    
    void removeAll(){
		//TODO:é¿ëï
    }
    
    private class ClickAdapter implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			
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
						case 0://çÌèú
							//delete(position);
							break;
						case 1://ÉLÉÉÉìÉZÉã
							break;
						}
					}
				}).show();				
			
			return true;
		}
    }
}

