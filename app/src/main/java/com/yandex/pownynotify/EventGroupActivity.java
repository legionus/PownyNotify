package com.yandex.pownynotify;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class EventGroupActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventgroup);

        System.out.println("!!! EventGroupActivity");

        Intent mIntent = getIntent();
        String group = mIntent.getStringExtra("group");

        DatabaseEvents db = new DatabaseEvents(this);
        ArrayList<Event> eventList = db.selectRecordsByGroup(group);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            LayoutInflater mInflater = LayoutInflater.from(this);
            View mCustomView = mInflater.inflate(R.layout.eventgroup_actionbar, null);

            TextView subject = (TextView) mCustomView.findViewById(R.id.EventGroupTopSubject);
            subject.setText(eventList.get(0).getSubject());

            actionBar.setCustomView(mCustomView);

            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        EventGroupAdapter mEvAdapter = new EventGroupAdapter(this, R.layout.eventgroup_item, eventList);

        ListView list = (ListView) findViewById(R.id.groupList);
        list.setAdapter(mEvAdapter);

        registerForContextMenu(list);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        System.out.println("!!! EventGroupActivity onCreateContextMenu");
/*
        if (v.getId() != R.id.groupList) {
            return;
        }
*/
        //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        //menu.setHeaderTitle(Countries[info.position]);
        String[] menuItems = getResources().getStringArray(R.array.event_actions_menu);

        for (int i = 0; i < menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return true;
    }
}
