package com.example.ble;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class MyDatabaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_TABLE = "create table PULSE(" +
            "id integer primary key autoincrement," +
            "SPO2 int," +
            "PR   int)";
    private Context mContext;
    public MyDatabaseHelper(Context context, String name,
                            SQLiteDatabase.CursorFactory factory,int version){
        super(context,name,factory,version);
        mContext = context;
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(CREATE_TABLE);
        Toast.makeText(mContext,"Create Success",Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        db.execSQL("DROP TABLE IF EXISTS PULSE");
        onCreate(db);
    }
}
