package com.beanie.stepsensors;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {

	private static final String TAG = "DBAdapter"; //used for logging database version changes
			
	// Field Names:
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TASK = "task";
	public static final String KEY_CALORIE = "calorie";
	public static final String KEY_DATE = "date";
	
	public static final String[] ALL_KEYS = new String[] {KEY_ROWID, KEY_TASK,KEY_CALORIE, KEY_DATE};
	
	// Column Numbers for each Field Name:
	public static final int COL_ROWID = 0;
	public static final int COL_TASK = 1;
	public static final int COL_CALORIE = 1;
	public static final int COL_DATE = 3;

	// DataBase info:
	public static final String DATABASE_NAME = "dbToDo";
	public static final String DATABASE_TABLE = "mainToDo";
	public static final int DATABASE_VERSION = 3; // The version number must be incremented each time a change to DB structure occurs.
		
	//SQL statement to create database
	private static final String DATABASE_CREATE_SQL = 
			"CREATE TABLE " + DATABASE_TABLE 
			+ " (" + KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ KEY_TASK + " TEXT NOT NULL, "
			+ KEY_CALORIE + " TEXT NOT NULL, "
			+ KEY_DATE + " TEXT"
			+ ");";
	
	private final Context context;
	private DatabaseHelper myDBHelper;
	private SQLiteDatabase db;


	public DBAdapter(Context ctx) {
		this.context = ctx;
		myDBHelper = new DatabaseHelper(context);
	}
	
	// Open the database connection.
	public DBAdapter open() {
		db = myDBHelper.getWritableDatabase();
		return this;
	}
	
	// Close the database connection.
	public void close() {
		myDBHelper.close();
	}
	
	// Add a new set of values to be inserted into the database.
	public long insertRow(String task,String calorie, String date) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TASK, task);
		initialValues.put(KEY_CALORIE, calorie);
		initialValues.put(KEY_DATE, date);
				
		// Insert the data into the database.
		return db.insert(DATABASE_TABLE, null, initialValues);
	}
	
	// Delete a row from the database, by rowId (primary key)
	public boolean deleteRow(long rowId) {
		String where = KEY_ROWID + "=" + rowId;
		return db.delete(DATABASE_TABLE, where, null) != 0;
	}
	
	public void deleteAll() {
		Cursor c = getAllRows();
		long rowId = c.getColumnIndexOrThrow(KEY_ROWID);
		if (c.moveToFirst()) {
			do {
				deleteRow(c.getLong((int) rowId));				
			} while (c.moveToNext());
		}
		c.close();
	}
	
	// Return all data in the database.
	public Cursor getAllRows() {
		String where = null;
		Cursor c = 	db.query(true, DATABASE_TABLE, ALL_KEYS, where, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	// Get a specific row (by rowId)
	public Cursor getRow(long rowId) {
		String where = KEY_ROWID + "=" + rowId;
		Cursor c = 	db.query(true, DATABASE_TABLE, ALL_KEYS, 
						where, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}
	
	// Change an existing row to be equal to new data.
	public boolean updateRow(long rowId, String task, String calorie, String date) {
		String where = KEY_ROWID + "=" + rowId;
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_TASK, task);
		newValues.put(KEY_CALORIE, calorie);
		newValues.put(KEY_DATE, date);
		// Insert it into the database.
		return db.update(DATABASE_TABLE, newValues, where, null) != 0;
	}

	
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_CREATE_SQL);			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}

	
	}


}

