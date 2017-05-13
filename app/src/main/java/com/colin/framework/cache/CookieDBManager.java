package com.colin.framework.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.colin.framework.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CookieDBManager {

	private final String DB_NAME = "cookie.db";
	private final String TABLE_NAME = "cookie";

	private DBHelper dbHelper;
	private SQLiteDatabase db;

	private static CookieDBManager instance;

	public static CookieDBManager getInstance(Context con) {
		if (instance == null) {
			synchronized (CookieDBManager.class) {
                if (instance == null) {
                    instance = new CookieDBManager(con.getApplicationContext());
                }
            }
		}
		return instance;
	}

	private CookieDBManager(Context con) {
		File dir = FileUtils.getDBDir(con);
		File file = new File(dir, DB_NAME);
		dbHelper = new DBHelper(con, file.getPath(), null, 1);
		db = dbHelper.getWritableDatabase();
	}

	private class DBHelper extends SQLiteOpenHelper {

		private String SQL_CAREATE_DB = "CREATE TABLE IF NOT EXISTS "//
		        + TABLE_NAME + " (" + //
		        Column.AUTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + //
		        Column.VALUE + " TEXT," + //
		        Column.NAME + " TEXT," + //
		        Column.COMMENT + " TEXT," + //
		        Column.DOMAIN + " TEXT," + //
		        Column.EXPIRY_DATE + " INTEGER," + //
		        Column.PATH + " TEXT," + //
		        Column.SECURE + " INTEGER," + //
		        Column.VERSION + " TEXT)";//

		private String CREATE_COOKIE_INDEX = "CREATE UNIQUE INDEX cookieIndex ON "
				+ TABLE_NAME + "("
				+ Column.NAME + ")";

		public DBHelper(Context context, String name, CursorFactory factory,
		        int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CAREATE_DB);
			db.execSQL(CREATE_COOKIE_INDEX);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			db.execSQL(SQL_CAREATE_DB);
		}

	}
	
	public List<CookieInternal> getAllCookies() {
		List<CookieInternal> cookies = new ArrayList<>();

		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String cacheKey = cursor.getString(cursor.getColumnIndex(Column.NAME));
			String value = cursor.getString(cursor.getColumnIndex(Column.VALUE));

			int lastIndex = cacheKey.lastIndexOf("|");
			String name = cacheKey.substring(lastIndex + 1);
			CookieInternal cookie = new CookieInternal(name, value);

			cookie.setComment(cursor.getString(cursor
			        .getColumnIndex(Column.COMMENT)));
			cookie.setDomain(cursor.getString(cursor
			        .getColumnIndex(Column.DOMAIN)));
			cookie.setExpiryAt(cursor.getLong(cursor.getColumnIndex(Column.EXPIRY_DATE)));
			cookie.setPath(cursor.getString(cursor.getColumnIndex(Column.PATH)));
			cookie.setSecure(cursor.getInt(cursor.getColumnIndex(Column.SECURE)) == 1);
			cookie.setVersion(cursor.getInt(cursor
			        .getColumnIndex(Column.VERSION)));

			cookies.add(cookie);
		}

		cursor.close();

		return cookies;
	}

	public void saveCookie(CookieInternal cookie) {
		if (cookie == null) {
			return;
		}
		db.delete(TABLE_NAME, Column.NAME + " = ? ",new String[] { cookie.getCookieKey() });
		ContentValues values = new ContentValues();
		values.put(Column.NAME, cookie.getCookieKey());
		values.put(Column.VALUE, cookie.getValue());
		values.put(Column.COMMENT, cookie.getComment());
		values.put(Column.DOMAIN, cookie.getDomain());
		values.put(Column.EXPIRY_DATE, cookie.getExpiryAt());
		values.put(Column.PATH, cookie.getPath());
		values.put(Column.SECURE, cookie.isSecure() ? 1 : 0);
		values.put(Column.VERSION, cookie.getVersion());

		db.insert(TABLE_NAME, null, values);
	}

    public void removeCookie(CookieInternal cookie) {
        if (cookie == null) {
            return;
        }
		db.delete(TABLE_NAME, Column.NAME + " = ? ",
		        new String[] { cookie.getCookieKey() });
    }

    public void removeCookies(CookieInternal[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return;
        }
        db.beginTransaction();
        for (CookieInternal cookie : cookies) {
            removeCookie(cookie);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

	public void saveCookies(CookieInternal[] cookies) {
		if (cookies == null || cookies.length == 0) {
			return;
		}

		db.beginTransaction();

		for (CookieInternal cookie : cookies) {
			saveCookie(cookie);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public void clear() {
		db.delete(TABLE_NAME, null, null);
	}

	public void clearExpired() {
		long time = System.currentTimeMillis();
		db.delete(TABLE_NAME, "EXPIRY_DATE < ? AND EXPIRY_DATE != 0",
		        new String[] { String.valueOf(time) });
	}

	private static class Column {
		public static final String AUTO_ID = "AUTO_ID";
		public static final String VALUE = "VALUE";
		public static final String NAME = "NAME";
		public static final String COMMENT = "COMMENT";
		public static final String DOMAIN = "DOMAIN";
		public static final String EXPIRY_DATE = "EXPIRY_DATE";
		public static final String PATH = "PATH";
		public static final String SECURE = "SECURE";
		public static final String VERSION = "VERSION";
	}
}
