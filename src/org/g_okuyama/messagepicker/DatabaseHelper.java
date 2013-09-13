package org.g_okuyama.messagepicker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String CREATE_TABLE_SQL =
            "create table logtable"
                    + "(rowid integer primary key autoincrement,"
                    + "name text not null,"/*1*/
                    + "contents text not null,"/*2*/
                    + "time text not null)";/*3*/

    public DatabaseHelper(Context context) {
        super(context, "logdb", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
    }
}