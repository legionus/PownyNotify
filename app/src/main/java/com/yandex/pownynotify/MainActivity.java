package com.yandex.pownynotify;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity  implements EventTaskFragment.TaskCallbacks, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MainActivity";
    private static final String TAG_TASK_FRAGMENT = "powny_fragment";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final int OAUTH_REQUEST = 1;

    public SwipeRefreshLayout mSwipeView;

    public Context mContext;
    public ListView mList;
    public EventAdapter<Event> mEvAdapter;

    public BroadcastReceiver mEventsBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.err.println("!!! MainActivity onCreate");

        mContext = this;

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mSwipeView.setRefreshing(false);
        mSwipeView.setOnRefreshListener(this);

        final ArrayList<Event> eventList = new ArrayList<>();
        mEvAdapter = new EventAdapter(this, R.layout.listview_item, eventList);

        mList = (ListView) findViewById(R.id.topList);
        mList.setAdapter(mEvAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Event event = eventList.get(position);

                if (event.getCount() == 1) {
                    Intent intent = new Intent(mContext, EventDetailsActivity.class);
                    intent.putExtra("eventid", event.getId());
                    mContext.startActivity(intent);
                } else {
                    Intent intent = new Intent(mContext, EventGroupActivity.class);
                    intent.putExtra("group", event.getSubject());
                    mContext.startActivity(intent);
                }
            }
        });

        registerForContextMenu(mList);

        mEventsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                GetEvents();
            }
        };
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(mEventsBroadcastReceiver, new IntentFilter("GetEvents"));

        if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent("GetEvents");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mEventsBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onRefresh() {
        Intent intent = new Intent("GetEvents");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OAUTH_REQUEST:
                if(resultCode == RESULT_OK) {
                    Intent intent = new Intent("GetEvents");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                }
                break;
        }
    }

    @Override
    public void onPreExecute() {
        System.err.println("!!! MainActivity onPreExecute");

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        if (mSwipeView == null) {
            return;
        }
        mSwipeView.setVisibility(View.VISIBLE);
        mSwipeView.setRefreshing(true);

        mEvAdapter.clear();
    }

    @Override
    public final void onProgressUpdate(Event... items) {
        mEvAdapter.addAll(items);
    }

    @Override
    public void onCancelled() {
        System.err.println("!!! MainActivity onCancelled");

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        if (mSwipeView == null) {
            return;
        }
        mSwipeView.setRefreshing(false);
    }

    @Override
    public void onPostExecute( AsyncTaskResult<JSONObject> result) {
        System.err.println("!!! MainActivity onPostExecute");

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        if (mSwipeView == null) {
            return;
        }
        mSwipeView.setRefreshing(false);

        if (result.getError() != null) {
            Intent intent = new Intent(this, OAuthActivity.class);
            startActivityForResult(intent, OAUTH_REQUEST);
            return;
        }

        mEvAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        System.out.println("!!! MainActivity onCreateContextMenu");

        String[] menuItems = getResources().getStringArray(R.array.event_actions_menu);

        for (int i = 0; i < menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return true;
    }

    private void GetEvents() {
        System.err.println("!!! MainActivity GetEvents");

        SharedPreferences mPref = getSharedPreferences("PownyAppPref", MODE_PRIVATE);

        FragmentManager fm = getFragmentManager();
        EventTaskFragment mTaskFragment = (EventTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        FragmentTransaction ft = fm.beginTransaction();

        if (mTaskFragment != null) {
            ft.remove(mTaskFragment);
        }

        Bundle args = new Bundle();
        args.putString("OAuthToken", mPref.getString("OAuthToken", ""));
        args.putString("OAuthSecret", mPref.getString("OAuthSecret", ""));

        mTaskFragment = new EventTaskFragment();
        mTaskFragment.setArguments(args);

        ft.add(mTaskFragment, TAG_TASK_FRAGMENT);
        ft.commit();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
