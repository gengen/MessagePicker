package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessagePickerServiceJB42 extends AccessibilityService {
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
        	Notification n = (Notification)event.getParcelableData();
        	if(n == null){
        		return;
        	}

        	//String text = n.tickerText.toString();
        	//Log.d(TAG, "text = " + text);
        	
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup localView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
			n.contentView.reapply(getApplicationContext(), localView);

			View tv = localView.findViewById(16908358);
			String contents = null;
			if (tv != null && tv instanceof TextView){
				contents = ((TextView) tv).getText().toString();
			}
			
			tv = localView.findViewById(android.R.id.title);
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

            if(contents == null){
            	//for XPERIA AX
    			tv = localView.findViewById(16908358);
    			if (tv != null && tv instanceof TextView){
    				contents = ((TextView) tv).getText().toString();
    			}

    			if(contents == null){
            		contents = event.getText().toString();
            		if(contents == null){
            			//�e�L�X�g��null�̏ꍇ�̓G���[���b�Z�[�W��ݒ�
            			contents = getString(R.string.error_msg);
            		}
            	}
            }
            
            //Log.d(TAG, "name = " + name);
            //Log.d(TAG, "contents = " + contents);
            
            //getEventTime���ƋN��������̎��Ԃ������Ȃ����ߎg�p���Ȃ�
            //long time = event.getEventTime();
            long time = System.currentTimeMillis();
            
            HashMap<String, String> map = analyzeContents(name, contents);
            if(map != null){
            	name = map.get("name");
            	contents = map.get("contents");
            }
            
            //Log.d(TAG, "name = " + name);
            //Log.d(TAG, "contents = " + contents);
            
            //DB�Ɋi�[
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("contents", contents);
            values.put("time", time);
            SQLiteDatabase db = mHelper.getWritableDatabase();
            db.insert("logtable", null, values);
            db.close();
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
        	//Log.d(TAG, "length = " + name.length());
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

    private boolean checkPackage(AccessibilityEvent event){
        //String cls = event.getClassName().toString();
        String pkg = event.getPackageName().toString();

        //Log.d(TAG, "cls = " + cls + "," + "pkg = " + pkg);

        //LINE�𔻒�
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
    	//Log.d(TAG, "onServiceConnected");

    	/*
    	if (isInit) {
            return;
        }
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        //info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        setServiceInfo(info);
        isInit = true;
        */
        
        //�A�N�Z�V�r���e�B�ŗL���ɂ��ꂽ���Ƃ��o���Ă���
        SharedPreferences pref = getSharedPreferences(MessagePickerActivity.PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(MessagePickerActivity.AVAILABLE_KEY, true);
        editor.commit();
        
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
