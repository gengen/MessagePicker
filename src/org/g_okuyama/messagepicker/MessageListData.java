package org.g_okuyama.messagepicker;

public class MessageListData {
    private String mName = null;
    private String mContents = null;
    private String mDate = null;

    public void setName(String name){
        mName = name;
    }

    public String getName(){
        return mName;
    }

    public void setContents(String contents){
        mContents = contents;
    }

    public String getContents(){
        return mContents;
    }

    public void setDate(String date){
        mDate = date;
    }

    public String getDate(){
        return mDate;
    }
}
