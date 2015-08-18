package com.yandex.pownynotify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

public class EventGroupActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventgroup);

        Intent mIntent = getIntent();
        String group = mIntent.getStringExtra("group");

        DatabaseEvents db = new DatabaseEvents(this);
        ArrayList<Event> eventList = db.selectRecordsByGroup(group);

        EventAdapter mEvAdapter = new EventAdapter(this, R.layout.listview_item, eventList);

        ListView list = (ListView) findViewById(R.id.groupList);
        list.setAdapter(mEvAdapter);
    }
}
