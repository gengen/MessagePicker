package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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

public class MessagePickerServiceJB extends AccessibilityService {
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
        		Log.d(TAG, "I'm JB");
        	}

        	//for test
        	/*
            if(!(checkPackage(event))){
            	return;
            }
            */
        	
        	//for test
        	//�{�Ԃ̓t���O�O��
        	if(!testFlag){
        		getNotification(event);
        	}

            //for test
        	testFlag = true;
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
                        
                        //TODO:�����[�X���͂͂���
                        //����name��contents�ȊO�ɂ͂ǂ�ȕ\��������̂����Ă݂������B
                        //Log.d(TAG, "viewId = " + viewId);

                        if (type == 9 || type == 10) {
                            text.put(viewId, value.toString());
                        }
                    }

                    String name = text.get(16908310);
                    //String name = null;
                    String contents = text.get(16908358);
                    
                    if(name == null){
                    	name = "LINE";
                    }

                    if(contents == null){
                    	//for XPERIA AX
                    	contents = text.get(16908359);
                    	if(contents == null){
                    		contents = event.getText().toString();
                    		if(contents == null){
                    			//�e�L�X�g��null�̏ꍇ�̓G���[���b�Z�[�W��ݒ�
                    			contents = getString(R.string.error_msg);
                    		}
                    	}
                    }
                    
                    if(MessagePickerActivity.DEBUG){
                    	Log.d(TAG, "name = " + name);
                    	Log.d(TAG, "contents = " + contents);
                    }
                    
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
                    
                    displayNotificationArea();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    	
    	//�摜���M�p
    	//���� ���摜�𑗐M���܂����B
    	str = null;
    	str = contents.split(" ���摜�𑗐M");
    	if(str.length == 2){
        	map.put("name", str[0]);
        	map.put("contents", "�摜�𑗐M���܂����B�摜������ɂ�LINE���N�����Ă��������B");
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
    	//Log.d(TAG, "onServiceConnected");
        
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
