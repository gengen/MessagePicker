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
                    		
                    		//google analytics�𗘗p���ăL�[�𑗐M
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
                            			Build.MODEL,		// Event category (required) <-	�@��
                            			"" + j,				// Event action (required)   <- �L�[
                            			"" + length,		// Event label               <- �R���e���c�̒���
                            			(long)idx++)		// Event value               <- �C���f�b�N�X
                            			.build()
                            			);
                        	}
                        	
                    		if(contents == null){
                    			//�e�L�X�g��null�̏ꍇ�̓G���[���b�Z�[�W��ݒ�
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
        
        //4.0�ȑO��manifest��meta-data�͔F�����Ȃ����߁A�����Őݒ肷��B
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
    	//�A�N�Z�V�r���e�B�Ŗ����ɂ��ꂽ���Ƃ��o���Ă���
        SharedPreferences pref = getSharedPreferences(MessagePickerActivity.PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(MessagePickerActivity.AVAILABLE_KEY, false);
        editor.commit();

        return true;
    }
}
