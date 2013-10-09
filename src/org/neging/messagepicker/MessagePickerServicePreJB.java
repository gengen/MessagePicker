package org.neging.messagepicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
                    	//for XPERIA AX etc.
                    	//��XPERIA AX��API16�Ȃ̂ŁAMessagePickerService.java�ň������A
                    	//�ی��̂��߁A�ȉ������Ă���
                    	contents = text.get(16908359);
                    	if(contents == null){
                    		contents = event.getText().toString();
                    		if(contents == null){
                    			//�e�L�X�g��null�̏ꍇ�̓G���[���b�Z�[�W��ݒ�
                    			contents = getString(R.string.error_msg);
                    		}
                    	}
                    }

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
        String pkg = event.getPackageName().toString();

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
