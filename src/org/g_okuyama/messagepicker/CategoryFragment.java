package org.g_okuyama.messagepicker;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

public class CategoryFragment extends Fragment {
    DatabaseHelper mHelper = null;
    ArrayList<MessageListData> mMessageList = null;

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
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String query = "select * from logtable order by name;";
        Cursor c = db.rawQuery(query, null);
        int rowcount = c.getCount();

        mMessageList = new ArrayList<MessageListData>();

        if(rowcount != 0){
            c.moveToFirst();

            for(int i = 0; i < rowcount; i++){
                MessageListData logitem = new MessageListData();
                logitem.setName(c.getString(1));
                logitem.setContents(c.getString(2));
                //logitem.setDate(c.getString(3));
                mMessageList.add(logitem);

                c.moveToNext();
            }
        }
        c.close();
        db.close();

        MessageArrayAdapter adapter = new MessageArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, mMessageList);
        ListView listview = (ListView)v.findViewById(R.id.message_list_category);
        listview.setAdapter(adapter);
    }
}

