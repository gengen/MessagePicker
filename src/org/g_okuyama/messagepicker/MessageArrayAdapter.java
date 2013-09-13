package org.g_okuyama.messagepicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MessageArrayAdapter extends ArrayAdapter<MessageListData> {
    private LayoutInflater mInflater;

    public MessageArrayAdapter(Context context, int textViewResourceId, List<MessageListData> objects) {
        super(context, textViewResourceId, objects);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        //if(convertView == null){
        convertView = mInflater.inflate(R.layout.message_item, null);
        //}

        MessageListData data = (MessageListData)getItem(position);

        TextView name = (TextView)convertView.findViewById(R.id.item_name);
        name.setText(data.getName());

        TextView contents = (TextView)convertView.findViewById(R.id.item_contents);
        contents.setText(data.getContents());
        
        TextView date = (TextView)convertView.findViewById(R.id.item_date);
        date.setText(data.getDate());

        return convertView;
    }
}
