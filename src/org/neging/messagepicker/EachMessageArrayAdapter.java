package org.neging.messagepicker;

import java.util.List;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class EachMessageArrayAdapter extends ArrayAdapter<MessageListData> {
    private LayoutInflater mInflater;
    int mLoopCount = 1;
    Context mContext;

    public EachMessageArrayAdapter(Context context, int textViewResourceId, List<MessageListData> objects) {
        super(context, textViewResourceId, objects);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        //if(convertView == null){
        convertView = mInflater.inflate(R.layout.each_message_item, null);
        //}

        MessageListData data = (MessageListData)getItem(position);

        TextView contents = (TextView)convertView.findViewById(R.id.item_contents);
        contents.setAutoLinkMask(Linkify.ALL);
        contents.setText(data.getContents());
        
        TextView date = (TextView)convertView.findViewById(R.id.item_date);
        date.setText(data.getDate());

        return convertView;
    }
    
    /*
     * タップを無効にしたかったが下記だとすべて無効化されてしまうため削除
    @Override
	public boolean isEnabled(int position) {
		return false;
	}
	*/
}
