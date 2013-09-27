package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagePickerService extends AccessibilityService {
    public static final String TAG = "MessagePicker";
    Toast mToast;
    DatabaseHelper mHelper = null;

    @Override
    public void onCreate(){
        if(mHelper == null){
            mHelper = new DatabaseHelper(this.getApplicationContext());
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int et = event.getEventType();

        if(et == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){
            if(!(checkPackage(event))){
            	return;
            }

            getNotification(event);
        }
        else{
            return;
        }
    }

    private void getNotification(AccessibilityEvent event){
        Parcelable parcel = event.getParcelableData();
        if(!(parcel instanceof Notification)){
            return;
        }
        else{
            Notification notification = (Notification)parcel;
            RemoteViews rv = notification.contentView;
            Class secretClass = rv.getClass();

            try {
                Map<Integer, String> text = new HashMap<Integer, String>();
                Field outerFields[] = secretClass.getDeclaredFields();
                for (int i = 0; i < outerFields.length; i++) {
                    if (!outerFields[i].getName().equals("mActions")) continue;

                    outerFields[i].setAccessible(true);
                    ArrayList<Object> actions = (ArrayList<Object>)outerFields[i].get(rv);
                    for (Object action : actions) {
                        Field innerFields[] = action.getClass().getDeclaredFields();                        
                        Object value = null;
                        Integer type = null;
                        Integer viewId = null;
                        for (Field field : innerFields) {
                            field.setAccessible(true);
                            if (field.getName().equals("value")) {
                                value = field.get(action);
                            } else if (field.getName().equals("type")) {
                                type = field.getInt(action);
                            } else if (field.getName().equals("viewId")) {
                                viewId = field.getInt(action);
                            }
                        }
                        
                        //TODO:リリース時ははずす
                        //TODO：下のnameやcontents以外にはどんな表示があるのか見てみたいが。
                        //Log.d(TAG, "viewId = " + viewId);

                        if (type == 9 || type == 10) {
                            text.put(viewId, value.toString());
                        }
                    }

                    //LINEのときは使わないのでコメントアウト
                    //String name = text.get(16908310);
                    String name = null;
                    String contents = text.get(16908358);
                    
                    //if(name == null){
                    //name = getString(R.string.error_msg);
                    //}

                    if(contents == null){
                    	//for XPERIA AX
                    	contents = text.get(16908359);
                    	if(contents == null){
                    		contents = event.getText().toString();
                    		if(contents == null){
                    			//テキストもnullの場合はエラーメッセージを設定
                    			contents = getString(R.string.error_msg);
                    			//TODO:スタンプのときなどはどうなるか、検証必要
                    		}
                    	}
                    }
                    
                    //getEventTimeだと起動時からの時間しか取れないため使用しない
                    //long time = event.getEventTime();
                    long time = System.currentTimeMillis();
                    
                    //for LINE
                    String[] str = contents.split("：");
                    if(str.length == 2){
                    	name = str[0];
                    	contents = str[1];
                    }
                    else{
                    	//海外用。「:」が半角。
                    	str = null;
                    	str = contents.split(":");
                        if(str.length == 2){
                        	name = str[0];
                        	contents = str[1];
                        }
                    }
                    
                    Log.d(TAG, "name = " + name);
                    Log.d(TAG, "contents = " + contents);
                    
                    //DBに格納
                    ContentValues values = new ContentValues();
                    values.put("name", name);
                    values.put("contents", contents);
                    values.put("time", time);
                    SQLiteDatabase db = mHelper.getWritableDatabase();
                    db.insert("logtable", null, values);
                    db.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkPackage(AccessibilityEvent event){
        //String cls = event.getClassName().toString();
        String pkg = event.getPackageName().toString();

        //Log.d(TAG, "cls = " + cls + "," + "pkg = " + pkg);

        //LINEを判定
        if(pkg.equalsIgnoreCase("jp.naver.line.android")){
            return true;
        }

        return false;
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected(){
        //アクセシビリティで有効にされたことを覚えておく
        SharedPreferences pref = getSharedPreferences(MessagePickerActivity.PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(MessagePickerActivity.AVAILABLE_KEY, true);
        editor.commit();
    }

    @Override
    public boolean onUnbind(Intent intent){
    	//アクセシビリティで無効にされたことを覚えておく
        SharedPreferences pref = getSharedPreferences(MessagePickerActivity.PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(MessagePickerActivity.AVAILABLE_KEY, false);
        editor.commit();

        return true;
    }
}
