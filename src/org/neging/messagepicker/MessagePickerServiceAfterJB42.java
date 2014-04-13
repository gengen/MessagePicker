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

        	//for test ��������
        	//�{�Ԃ̓t���O�O��
        	if(!testFlag){
        		getNotification(event);
        	}

        	testFlag = true;
            //for test�@�����܂�
        	
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
            
            //contents��16908358, 8359, 8360���w�肷��B
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
            	
            	//google analytics�𗘗p���ăL�[�𑗐M
            	EasyTracker easyTracker = EasyTracker.getInstance(this);
            	easyTracker.send(MapBuilder.createEvent(
            			Build.MODEL,		// Event category (required) <-	�@��
            			"9999",				// Event action (required)   <- �L�[
            			"" + length,		// Event label               <- �R���e���c�̒���
            			(long)9999)			// Event value               <- �C���f�b�N�X
            			.build()
            			);
            }

            if(contents == null){
            	//�e�L�X�g��null�̏ꍇ�̓G���[���b�Z�[�W��ݒ�
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
    	
    	//�ʏ�̃`���b�g
    	str = contents.split(getString(R.string.split_char_1));
        if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", str[1]);
        	return map;
        }

        //�X�^���v�p
        //���{�u�������X�^���v�𑗐M���܂����v�A�p��u���� sent a sticker.�v
        str = null;
        str = contents.split(getString(R.string.split_char_2));
        if(str.length == 2){
        	map.put("name", str[0]);
        	String text = getString(R.string.stamp_prefix) + str[1] + getString(R.string.stamp_suffix);
        	map.put("contents", text);
        	return map;
        }
        
        //���M�p
        //�^�C�g���F�uLINE ��������̒��M�ł��v�A�e�L�X�g�F�u�����Ƃ̖����ʘb�v
        if(name.length() != 4){
        	str = null;
        	str = contents.split("�Ƃ̖���");
        	if(str.length == 2){
            	map.put("name", str[0]);
            	map.put("contents", "���M�ł��B");
            	return map;
        	}
        	else{
        		return null;
        	}
        }
        
        //�ʘb�p
        //�u�����Ƃ̖����ʘb�v
    	str = null;
    	str = contents.split("�Ƃ̖���");
    	if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", "�����ʘb");
        	return map;
    	}
    	
    	//�s�ݒ��M�p
    	//�u����:�s�ݒ��M�v
    	str = null;
    	str = contents.split(":");
    	if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", "�s�ݒ��M");
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

        //LINE�𔻒�
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
        //�A�N�Z�V�r���e�B�ŗL���ɂ��ꂽ���Ƃ��o���Ă���
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
    	//�A�N�Z�V�r���e�B�Ŗ����ɂ��ꂽ���Ƃ��o���Ă���
        SharedPreferences pref = getSharedPreferences(MessagePickerActivity.PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(MessagePickerActivity.AVAILABLE_KEY, false);
        editor.commit();

        return true;
    }
}
