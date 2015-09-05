package com.yandex.pownynotify;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class EventGroupActivity extends Activity implements DeleteDialogFragment.Callbacks, DeleteEventFragment.Callbacks {
    Context mContext;
    SharedPreferences mPref;
    DatabaseEvents mDB;

    ArrayList<Event> eventList;
    EventGroupAdapter mEvAdapter;

    int mSelectedItem;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventgroup);

        System.out.println("!!! EventGroupActivity");

        mContext = this;

        mPref = getSharedPreferences("PownyAppPref", MODE_PRIVATE);
        mSelectedItem = mPref.getInt("EventGroup:ItemSelected", -1);

        Intent mIntent = getIntent();
        String group = mIntent.getStringExtra("group");

        mDB = new DatabaseEvents(this);
        eventList = mDB.selectRecordsByGroup(group);

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

        mEvAdapter = new EventGroupAdapter(this, R.layout.eventgroup_item, eventList);

        ListView list = (ListView) findViewById(R.id.groupList);
        list.setAdapter(mEvAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Event event = eventList.get(position);

                Intent intent = new Intent(mContext, EventDetailsActivity.class);
                intent.putExtra("eventid", event.getId());

                mContext.startActivity(intent);
            }
        });

        registerForContextMenu(list);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        System.out.println("!!! EventGroupActivity onCreateContextMenu");

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.add(Menu.NONE, info.position, 0, getResources().getString(R.string.context_menu_downtime));
        menu.add(Menu.NONE, info.position, 0, getResources().getString(R.string.context_menu_delete));

        mSelectedItem = info.position;
        mPref.edit().putInt("EventGroup:ItemSelected", mSelectedItem).apply();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(getResources().getString(R.string.context_menu_downtime))) {
            Toast.makeText(this, "Downtime", Toast.LENGTH_SHORT).show();
        }
        else if (item.getTitle().equals(getResources().getString(R.string.context_menu_delete))) {
            final String DIALOG_FRAGMENT = "DeleteDialogFragment";

            FragmentManager fm = getFragmentManager();
            DeleteDialogFragment fragment = (DeleteDialogFragment) fm.findFragmentByTag(DIALOG_FRAGMENT);

            FragmentTransaction ft = fm.beginTransaction();

            if (fragment != null) {
                ft.remove(fragment);
            }

            fragment = new DeleteDialogFragment();

            ft.add(fragment, DIALOG_FRAGMENT);
            ft.commit();
        }

        return true;
    }

    @Override
    public void onConfirmDelete() {
        if (mSelectedItem < 0) {
            return;
        }

        Event ev = eventList.get(mSelectedItem);

        String DELETE_FRAGMENT = "DeleteEventFragment:" + ev.getId();

        FragmentManager fm = getFragmentManager();
        DeleteEventFragment mFragment = (DeleteEventFragment) fm.findFragmentByTag(DELETE_FRAGMENT);

        if (mFragment != null) {
            return;
        }

        Bundle args = new Bundle();
        args.putString("OAuthToken", mPref.getString("OAuthToken", ""));
        args.putString("OAuthSecret", mPref.getString("OAuthSecret", ""));
        args.putString("EventId", ev.getId());

        mFragment = new DeleteEventFragment();
        mFragment.setArguments(args);

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(mFragment, DELETE_FRAGMENT);
        ft.commit();
    }

    @Override
    public void onCancelDelete() {
        Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPostDeleteEventExecute(AsyncTaskResult<Integer> result) {
        if (result.getError() != null) {
            Toast.makeText(this, "Unable to delete event", Toast.LENGTH_SHORT).show();
            return;
        }

        eventList.remove(mSelectedItem);
        mEvAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Delete: " + mSelectedItem, Toast.LENGTH_SHORT).show();
    }
}
