package com.blacksmithlabs.networkrecorder.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Brian on 7/20/13.
 */
public class LogEntryDatabase {
	private final LogEntryDbHelper dbHelper;

	public LogEntryDatabase(Context context) {
		dbHelper = new LogEntryDbHelper(context);
	}

	/**
	 * Create a new log entry in the database
	 * @param appUID the UID of the app who's traffic was recorded
	 * @param entryID a unique identifier for the log file
	 * @param name the name of the log file
	 * @param path the absolute path of the log file
	 * @return the id of the log entry
	 */
	public long createLogEntry(int appUID, String entryID, String name, String path) throws SQLException {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		try {
			final ContentValues values = new ContentValues();
			values.put(LogEntryTable.COLUMN_APP_UID, appUID);
			values.put(LogEntryTable.COLUMN_ENTRY_ID, entryID);
			values.put(LogEntryTable.COLUMN_LOG_NAME, name);
			values.put(LogEntryTable.COLUMN_LOG_PATH, path);

			return db.insert(LogEntryTable.TABLE_NAME, "null", values);
		} finally {
			db.close();
		}
	}

	/**
	 * Update the log length for a given log file
	 * @param appUID the UID of the app
	 * @param entryID the unique identifier of the log file
	 * @param length the new length (in bytes)
	 * @return the number of rows updated (should only be one)
	 */
	public int updateLogLength(int appUID, String entryID, long length) throws SQLException {
		// Too huge...
		if (length < 0) {
			length = -1;
		}

		if (entryID == null || entryID.isEmpty()) {
			throw new IllegalArgumentException("EntryID cannot be NULL or empty");
		}

		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		try {
			final ContentValues values = new ContentValues();
			values.put(LogEntryTable.COLUMN_LOG_LENGTH, length);

			final String where = LogEntryTable.COLUMN_APP_UID + " = ?"
					+ " AND " + LogEntryTable.COLUMN_ENTRY_ID + " = ?";

			final String[] args = new String[] {
				Integer.toString(appUID),
				entryID,
			};

			return db.update(LogEntryTable.TABLE_NAME, values, where, args);
		} finally {
			db.close();
		}
	}

	/**
	 * Get the logs, optionally filtering by appUID, entryID, or both
	 * @param appUID
	 * @param entryID
	 * @return the cursor for the logs, null on error or none
	 */
	public Cursor getLogs(Integer appUID, String entryID) throws SQLException {
		ContentValues args = new ContentValues();
		if (appUID != null) {
			args.put(LogEntryTable.COLUMN_APP_UID, appUID);
		}
		if (entryID != null && !entryID.isEmpty()) {
			args.put(LogEntryTable.COLUMN_ENTRY_ID, entryID);
		}

		return query(args, LogEntryTable.ALL_COLUMNS);
	}

	/**
	 * Delete an entry based on it's unique ID in the database
	 * @param ID
	 * @return the number of rows affected
	 */
	public int deleteLog(long ID) throws SQLException {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		return db.delete(
				LogEntryTable.TABLE_NAME,
				LogEntryTable._ID + " = ?",
				new String[] {Long.toString(ID)});
	}

	/**
	 * Run a simple query against a known set of arguments (all AND'd together)
	 * @param values key/value pairs for the WHERE clause
	 * @param columns
	 * @return the cursor for the query, null if issues
	 */
	private Cursor query(ContentValues values, String[] columns) throws SQLException {
		final Cursor cursor;

		if (values.size() > 0) {
			final StringBuilder where = new StringBuilder();
			final ArrayList<String> args = new ArrayList<String>();

			String joiner = "";
			for (Map.Entry<String, Object> entry : values.valueSet()) {
				where.append(joiner).append(entry.getKey()).append(" = ?");
				args.add(entry.getValue().toString());
				joiner = " AND ";
			}

			cursor = query(where.toString(), args.toArray(new String[args.size()]), columns);
		} else {
			cursor = query(null, null, columns);
		}

		return cursor;
	}

	/**
	 * Run a query to retrieve certain columns
	 * @param where
	 * @param args
	 * @param columns
	 * @return the cursor for the query, null if issues
	 */
	private Cursor query(String where, String[] args, String[] columns) throws SQLException {
		final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(LogEntryTable.TABLE_NAME);

		final String sort = LogEntryTable.COLUMN_CREATION_DATE + " DESC";

		Cursor cursor = builder.query(dbHelper.getReadableDatabase(),
				columns, where, args, null, null, sort);

		if (cursor != null && !cursor.moveToFirst()) {
			cursor.close();
			cursor = null;
		}

		return cursor;
	}

	/**
	 * Class to define our table and other useful things
	 */
	public static class LogEntryTable implements BaseColumns {
		public static final String TABLE_NAME = "logentry";
		public static final String COLUMN_ENTRY_ID = "entryid";
		public static final String COLUMN_LOG_NAME = "name";
		public static final String COLUMN_LOG_PATH = "path";
		public static final String COLUMN_LOG_LENGTH = "length";
		public static final String COLUMN_APP_UID = "appuid";
		public static final String COLUMN_CREATION_DATE = "createdate";

		public static final String[] ALL_COLUMNS = new String[] {
				_ID,
				COLUMN_ENTRY_ID,
				COLUMN_LOG_NAME,
				COLUMN_LOG_PATH,
				COLUMN_LOG_LENGTH,
				COLUMN_APP_UID,
				COLUMN_CREATION_DATE,
		};
	}

	/**
	 * The DB Helper for the Log Entry db helper
	 */
	private static class LogEntryDbHelper extends SQLiteOpenHelper {
		// If database schema changes, increment version number
		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "NetworkLogEntry.db";

		private static final String LOG_ENTRY_CREATE =
				"CREATE TABLE " + LogEntryTable.TABLE_NAME + " (" +
					LogEntryTable._ID + " INTEGER PRIMARY KEY," +
					LogEntryTable.COLUMN_APP_UID + " INTEGER NOT NULL," +
					LogEntryTable.COLUMN_ENTRY_ID + " TEXT UNIQUE NOT NULL," +
					LogEntryTable.COLUMN_LOG_NAME + " TEXT NOT NULL," +
					LogEntryTable.COLUMN_LOG_PATH + " TEXT NOT NULL," +
					LogEntryTable.COLUMN_LOG_LENGTH + " INTEGER DEFAULT 0," +
					LogEntryTable.COLUMN_CREATION_DATE + " TIMESTAMP DEFAULT (strftime('%s', 'now'))" +
				" )";

		public LogEntryDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(LOG_ENTRY_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Nothing to update, yet
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Nothing to downgrade, yet
		}
	}
}
