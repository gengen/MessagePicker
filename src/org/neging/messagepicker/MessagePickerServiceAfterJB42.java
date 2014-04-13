package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class MessagePickerServiceAfterJB42 extends AccessibilityService {
    public static final String TAG = "MessagePicker";
    Toast mToast;
    DatabaseHelper mHelper = null;
    
    boolean isInit = false;
    
    //for test
    boolean testFlag = false;

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
        		Log.d(TAG, "I'm After JB4.2");
        	}
        	
        	/*
            if(!(checkPackage(event))){
            	return;
            }
            */

        	//for test ここから
        	//本番はフラグ外す
        	if(!testFlag){
        		getNotification(event);
        	}

        	testFlag = true;
            //for test　ここまで
        	
        	//getNotification(event);  
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
        	Notification n = (Notification)event.getParcelableData();
        	if(n == null){
        		return;
        	}
        	
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup localView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
			n.contentView.reapply(getApplicationContext(), localView);
            
			View tv = localView.findViewById(android.R.id.title);
			String name = null;
			if (tv != null && tv instanceof TextView){
				name = ((TextView) tv).getText().toString();
			}
			tv = localView.findViewById(16909082);
			if (tv != null && tv instanceof TextView){
				name = ((TextView) tv).getText().toString();
			}
			
            if(name == null){
            	name = "LINE";
            }
            
            //contentsは16908358, 8359, 8360を指定する。
            String contents = null;
            tv = localView.findViewById(16908358);
            if (tv != null && tv instanceof TextView){
            	contents = ((TextView) tv).getText().toString();
            }
			
            if(contents == null){
    			tv = localView.findViewById(16908359);
    			if (tv != null && tv instanceof TextView){
    				contents = ((TextView) tv).getText().toString();
    			}
            }
            
            if(contents == null){
    			tv = localView.findViewById(16908360);
    			if (tv != null && tv instanceof TextView){
    				contents = ((TextView) tv).getText().toString();
    			}
            }

            if(contents == null){
            	contents = event.getText().toString();
            	
            	int length = 0;
            	if(contents != null){
            		length = contents.length();
            	}
            	
            	//google analyticsを利用してキーを送信
            	EasyTracker easyTracker = EasyTracker.getInstance(this);
            	easyTracker.send(MapBuilder.createEvent(
            			Build.MODEL,		// Event category (required) <-	機種
            			"9999",				// Event action (required)   <- キー
            			"" + length,		// Event label               <- コンテンツの長さ
            			(long)9999)			// Event value               <- インデックス
            			.build()
            			);
            }

            if(contents == null){
            	//テキストもnullの場合はエラーメッセージを設定
            	contents = getString(R.string.error_msg);
            }
            
            if(MessagePickerActivity.DEBUG){
            	Log.d(TAG, "name = " + name);
            	Log.d(TAG, "contents = " + contents);
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
