package com.yandex.pownynotify;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Event {
    private String mId;
    private String mSubject;
    private String mBody;
    private String mSeverity;
    private Date mTime;
    private int mCount;

    public Event() {
        mCount = 1;
        mTime = new Date();
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getSubject() {
        return mSubject;
    }

    public void setSubject(String mSubject) {
        this.mSubject = mSubject;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String mBody) {
        this.mBody = mBody;
    }

    public String getSeverity() {
        return mSeverity;
    }

    public void setSeverity(String mSeverity) {
        this.mSeverity = mSeverity;
    }

    public Date getTime() {
        return mTime;
    }

    public void setTime(Long mTime) {
        this.mTime = new java.util.Date(mTime * 1000);
    }

    public int getCount() {
        return mCount;
    }

    public void setCount(int mCount) {
        this.mCount = mCount;
    }

    public int incCount() {
        return this.mCount++;
    }
}
