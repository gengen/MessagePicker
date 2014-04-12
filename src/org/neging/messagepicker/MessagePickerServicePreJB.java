package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class MessagePickerServicePreJB extends AccessibilityService {
    public static final String TAG = "MessagePicker";
    Toast mToast;
    DatabaseHelper mHelper = null;
    
    boolean isInit = false;
    
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
        	if(MessagePickerActivity.DEBUG){
        		Log.d(TAG, "I'm Pre JB");
        	}

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

                        if (type == 9 || type == 10) {
                            text.put(viewId, value.toString());
                        }
                    }
                    
                    if(MessagePickerActivity.DEBUG){
                    	Set<Integer> s = text.keySet();
                    	for(int j: s){
                    		String item = text.get(j);
                    		Log.d(TAG, "item " + j + " = " + item);
                    	}
                    }

                    String name = text.get(16908310);
                    String contents = text.get(16908358);
                    
                    if(name == null){
                    	name = "LINE";
                    }

                    if(contents == null){
                    	int key = getContentsID();
                    	contents = text.get(key);
                    	if(contents == null){
                    		contents = event.getText().toString();
                    		
                    		//google analyticsを利用してキーを送信
                        	EasyTracker easyTracker = EasyTracker.getInstance(this);
                        	Set<Integer> s = text.keySet();
                        	int idx = 1;
                        	for(int j: s){
                        		String item = text.get(j);
                        		//Log.d(TAG, "item " + j + " = " + item);
                        		
                        		int length = 0;
                        		if(item != null){
                        			length = item.length();
                        		}
                            	easyTracker.send(MapBuilder.createEvent(
                            			Build.MODEL,		// Event category (required) <-	機種
                            			"" + j,				// Event action (required)   <- キー
                            			"" + length,		// Event label               <- コンテンツの長さ
                            			(long)idx++)		// Event value               <- インデックス
                            			.build()
                            			);
                        	}
                        	
                    		if(contents == null){
                    			//テキストもnullの場合はエラーメッセージを設定
                    			contents = getString(R.string.error_msg);
                    		}
                    	}
                    }
                    
                    HashMap<String, String> map = analyzeContents(name, contents);
                    if(map != null){
                    	name = map.get("name");
                    	contents = map.get("contents");
                    }
                    
                    //Log.d(TAG, "name = " + name);
                    //Log.d(TAG, "contents = " + contents);
                    
                    insertDB(name, contents);
                    displayNotificationArea();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private int getContentsID(){
    	String device = Build.MODEL;
    	if(device.equalsIgnoreCase("SO-02C")){
    		return 16908352;
    	}
    	else{
    		return 16908359;
    	}
    }
    
    HashMap<String, String> analyzeContents(String name, String contents){
        String[] str;
    	
    	HashMap<String, String> map = new HashMap<String, String>();
    	
    	//通常のチャット
    	str = contents.split(getString(R.string.split_char_1));
        if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", str[1]);
        	return map;
        }

        //スタンプ用
        //日本「○○がスタンプを送信しました」、英語「○○ sent a sticker.」
        str = null;
        str = contents.split(getString(R.string.split_char_2));
        if(str.length == 2){
        	map.put("name", str[0]);
        	String text = getString(R.string.stamp_prefix) + str[1] + getString(R.string.stamp_suffix);
        	map.put("contents", text);
        	return map;
        }
        
        //着信用
        //タイトル：「LINE ○○からの着信です」、テキスト：「○○との無料通話」
        if(name.length() != 4){
        	//Log.d(TAG, "length = " + name.length());
        	str = null;
        	str = contents.split("との無料");
        	if(str.length == 2){
            	map.put("name", str[0]);
            	map.put("contents", "着信です。");
            	return map;
        	}
        	else{
        		return null;
        	}
        }
        
        //通話用
        //「○○との無料通話」
    	str = null;
    	str = contents.split("との無料");
    	if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", "無料通話");
        	return map;
    	}
    	
    	//不在着信用
    	//「○○:不在着信」
    	str = null;
    	str = contents.split(":");
    	if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", "不在着信");
        	return map;
    	}

		return null;
    }
    
    private void insertDB(String name, String contents){
        long time = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("contents", contents);
        values.put("time", time);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.insert("logtable", null, values);
        db.close();
    }

    private boolean checkPackage(AccessibilityEvent event){
        String pkg = event.getPackageName().toString();

        //LINEを判定
        if(pkg.equalsIgnoreCase("jp.naver.line.android")){
            return true;
        }

        return false;
    }
    
    private void displayNotificationArea(){
        Intent intent = new Intent(getApplicationContext(), MessagePickerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
        		getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext());
        builder.setContentIntent(contentIntent);
        builder.setTicker(getString(R.string.app_name));
        builder.setSmallIcon(R.drawable.icon24);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.notification_text));
        builder.setAutoCancel(true);
        
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(R.string.app_name, builder.build());
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
        
        //4.0以前はmanifestのmeta-dataは認識しないため、ここで設定する。
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
        	AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        	info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        	info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        	info.notificationTimeout = 100;
        	setServiceInfo(info);
        }
        
        Intent intent = new Intent(getApplicationContext(), MessagePickerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
