package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessagePickerService extends AccessibilityService {
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

	@SuppressLint("NewApi")
	@Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    	Log.d(TAG, "onAccessibilityEvent");
    	
        int et = event.getEventType();

        if(et == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){
        	Log.d(TAG, "TYPE_NOTIFICATION");
            if(!(checkPackage(event))){
            	return;
            }
        	
        	//for test
        	String s = event.getText().toString();
        	if(s != null){
        		Log.d(TAG, "str = " + s);
        		//return;
        	}
        	
        	/*
        	AccessibilityNodeInfo info = event.getSource();
        	if(info != null){
        		Log.d(TAG, "info = " + info.getText());
        	}
        	*/
        	
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
                //Field outerFields[] = secretClass.getSuperclass().getDeclaredFields();
                for (int i = 0; i < outerFields.length; i++) {
                    if (!outerFields[i].getName().equals("mActions")) continue;

                    outerFields[i].setAccessible(true);
                    ArrayList<Object> actions = (ArrayList<Object>)outerFields[i].get(rv);
                    for (Object action : actions) {
                    	//Field innerFields[] = action.getClass().getDeclaredFields();
                    	Field innerFields[] = action.getClass().getSuperclass().getDeclaredFields();
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
                        Log.d(TAG, "viewId = " + viewId);
                        Log.d(TAG, "value = " + value);

                        if (type == 9 || type == 10) {
                            text.put(viewId, value.toString());
                        }
                    }
                    
                    //for test
                    Log.d(TAG, "text = " + text.get(16908294));

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
                    
                    //Log.d(TAG, "name = " + name);
                    //Log.d(TAG, "contents = " + contents);
                    
                    //getEventTime���ƋN��������̎��Ԃ������Ȃ����ߎg�p���Ȃ�
                    //long time = event.getEventTime();
                    long time = System.currentTimeMillis();
                    
                    String[] str;
                	str = contents.split(getString(R.string.split_char_1));
                    if(str.length == 2){
                    	name = str[0];
                    	contents = str[1];
                    }
                    else{
                    	//�X�^���v�p�B�z��́A
                    	//���{�u�������X�^���v�𑗐M���܂����v�A�p��u���� sent a sticker.�v
                    	str = null;
                    	str = contents.split(getString(R.string.split_char_2));
                    	if(str.length == 2){
                    		name = str[0];
                    		contents = getString(R.string.stamp_prefix) + str[1];
                    		contents += getString(R.string.stamp_suffix);
                    	}
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
    	Log.d(TAG, "onServiceConnected");

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
