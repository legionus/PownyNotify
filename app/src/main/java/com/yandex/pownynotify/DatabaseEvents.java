package com.yandex.pownynotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseEvents {

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDatabase;

    public DatabaseEvents(Context context) {
        mDBHelper = new DatabaseHelper(context);
        mDatabase = mDBHelper.getWritableDatabase();
    }

    public synchronized long createRecord(Event ev) throws SQLiteConstraintException {
        ContentValues values = new ContentValues();
        values.put("id",       ev.getId());
        values.put("severity", ev.getSeverity());
        values.put("subject",  ev.getSubject());
        values.put("body",     ev.getBody());
        values.put("time",     ev.getTime().getTime() / 1000);
        return mDatabase.insertOrThrow("events", null, values);
    }

    public int deleteRecords() {
        return mDatabase.delete("events", null, null);
    }

    public ArrayList<Event> selectAllRecords() {
        ArrayList<Event> list = new ArrayList<>();
        Cursor mCursor = mDatabase.rawQuery("SELECT * FROM events", null);

        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                Event ev = new Event();

                ev.setId(mCursor.getString(mCursor.getColumnIndex("id")));
                ev.setSeverity(mCursor.getString(mCursor.getColumnIndex("severity")));
                ev.setSubject(mCursor.getString(mCursor.getColumnIndex("subject")));
                ev.setBody(mCursor.getString(mCursor.getColumnIndex("body")));
                ev.setTime(mCursor.getLong(mCursor.getColumnIndex("time")));

                list.add(ev);
            } while (mCursor.moveToNext());
            mCursor.close();
        }

        return list;
    }

    public ArrayList<Event> selectRecordsByGroup(String group) {
        ArrayList<Event> list = new ArrayList<>();
        Cursor mCursor = mDatabase.rawQuery("SELECT * FROM events WHERE subject = ?", new String[]{group});

        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                Event ev = new Event();

                ev.setId(mCursor.getString(mCursor.getColumnIndex("id")));
                ev.setSeverity(mCursor.getString(mCursor.getColumnIndex("severity")));
                ev.setSubject(mCursor.getString(mCursor.getColumnIndex("subject")));
                ev.setBody(mCursor.getString(mCursor.getColumnIndex("body")));
                ev.setTime(mCursor.getLong(mCursor.getColumnIndex("time")));

                list.add(ev);
            } while (mCursor.moveToNext());
            mCursor.close();
        }

        return list;
    }

    public Event selectRecordById(String id) {
        Cursor mCursor = mDatabase.rawQuery("SELECT * FROM events WHERE id = ?", new String[]{id});

        if (mCursor != null && mCursor.moveToFirst()) {
            Event ev = new Event();

            ev.setId(mCursor.getString(mCursor.getColumnIndex("id")));
            ev.setSeverity(mCursor.getString(mCursor.getColumnIndex("severity")));
            ev.setSubject(mCursor.getString(mCursor.getColumnIndex("subject")));
            ev.setBody(mCursor.getString(mCursor.getColumnIndex("body")));
            ev.setTime(mCursor.getLong(mCursor.getColumnIndex("time")));

            mCursor.close();
            return ev;
        }

        return null;
    }
}
