package org.neging.messagepicker;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
        contents.setText(data.getContents());
        
        TextView date = (TextView)convertView.findViewById(R.id.item_date);
        date.setText(data.getDate());
        
        /*
        //îwåiêFê›íË
        LinearLayout linearLayout = (LinearLayout)convertView.findViewById(R.id.list_container);
        
        //- îwåiêFÇåå›Ç…ì¸ë÷Ç¶ÇÈ
        if( mLoopCount%2 == 0 ) {
             linearLayout.setBackgroundColor(mContext.getResources().getColor(R.color.listFirst));
        } else {
             linearLayout.setBackgroundColor(mContext.getResources().getColor(R.color.listSecond));
        }
        mLoopCount++;
        */

        return convertView;
    }
}
