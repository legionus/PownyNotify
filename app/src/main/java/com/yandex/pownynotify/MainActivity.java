package com.yandex.pownynotify;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity  implements EventViewFragment.Callbacks, EventDownloadFragment.Callbacks, SwipeRefreshLayout.OnRefreshListener, DeleteDialogFragment.Callbacks, DeleteEventFragment.Callbacks {
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public SwipeRefreshLayout mSwipeView;

    public Context mContext;
    public SharedPreferences mPref;
    public EventAdapter<Event> mEvAdapter;

    int mSelectedItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.err.println("!!! " + TAG + " onCreate");

        mContext = this;

        mPref = getSharedPreferences("PownyAppPref", MODE_PRIVATE);
        mSelectedItem = mPref.getInt(TAG + ":ItemSelected", -1);

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mSwipeView.setOnRefreshListener(this);

        mSwipeView.setVisibility(View.VISIBLE);
        mSwipeView.setRefreshing(true);

        ArrayList<Event> eventList = new ArrayList<>();
        mEvAdapter = new EventAdapter(this, R.layout.listview_item, eventList);

        ListView mList = (ListView) findViewById(R.id.topList);
        mList.setAdapter(mEvAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Event event = mEvAdapter.getItem(position);

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
        mEvAdapter.notifyDataSetChanged();

        registerForContextMenu(mList);
        registerEventViewReceiver();
        registerEventDownloadReceiver();

        if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    private void registerEventDownloadReceiver() {
        String TAG_FUNC = "EventDownloadReceiver";

        System.err.println("!!! " + TAG + " Register " + TAG_FUNC);

        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(TAG_FUNC) == null) {
            fm.beginTransaction().add(new EventDownloadFragment(), TAG_FUNC).commit();
        }
    }

    private void registerEventViewReceiver() {
        String TAG_FUNC = "EventViewReceiver";

        System.err.println("!!! " + TAG + " Register " + TAG_FUNC);

        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(TAG_FUNC) == null) {
            fm.beginTransaction().add(new EventViewFragment(), TAG_FUNC).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Context mContext = getApplicationContext();
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("EventsView"));
    }

    @Override
    public void onRefresh() {
        Context mContext = getApplicationContext();
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("EventsDownload"));
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

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_refresh) {
            Context mContext = getApplicationContext();
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("EventsDownload"));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEventDownloadPreExecute() {
        System.err.println("!!! " + TAG + " onPreExecute");

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        if (mSwipeView == null) {
            return;
        }
        mSwipeView.setVisibility(View.VISIBLE);
        mSwipeView.setRefreshing(true);
    }

    @Override
    public void onEventDownloadPostExecute( AsyncTaskResult<JSONObject> result) {
        System.err.println("!!! " + TAG + " onEventDownloadPostExecute");

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        if (mSwipeView == null) {
            return;
        }
        mSwipeView.setRefreshing(false);

        if (result.getError() != null) {
            Intent intent = new Intent(this, OAuthActivity.class);
            startActivity(intent);
            return;
        }

        Context mContext = getApplicationContext();
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("EventsView"));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        System.out.println("!!! " + TAG + " onCreateContextMenu");

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.add(Menu.NONE, info.position, 0, getResources().getString(R.string.context_menu_delete));

        mSelectedItem = info.position;
        mPref.edit().putInt(TAG + ":ItemSelected", mSelectedItem).apply();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(getResources().getString(R.string.context_menu_delete))) {
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

    @Override
    public void onCancelDelete() {
        Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfirmDelete() {
        if (mSelectedItem < 0) {
            return;
        }

        System.err.println("!!! " + TAG + " onConfirmDelete item=" + mSelectedItem);

        //Event ev = eventList.get(mSelectedItem);
        Event ev = mEvAdapter.getItem(mSelectedItem);

        String DELETE_FRAGMENT = "DeleteEventFragment";

        FragmentManager fm = getFragmentManager();
        DeleteEventFragment mFragment = (DeleteEventFragment) fm.findFragmentByTag(DELETE_FRAGMENT);

        FragmentTransaction ft = fm.beginTransaction();

        if (mFragment != null) {
            ft.remove(mFragment);
        }

        Bundle args = new Bundle();
        args.putString("EventSubject", ev.getSubject());

        mFragment = new DeleteEventFragment();
        mFragment.setArguments(args);

        ft.add(mFragment, DELETE_FRAGMENT);
        ft.commit();
    }

    @Override
    public void onPostDeleteEventExecute(AsyncTaskResult<Integer> result) {
        if (result.getError() != null) {
            Toast.makeText(this, "Unable to delete event", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEventViewPreExecute() {
        System.err.println("!!! " + TAG + " onEventViewPreExecute");

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        if (mSwipeView == null) {
            return;
        }
        mSwipeView.setVisibility(View.VISIBLE);
        mSwipeView.setRefreshing(true);

        mEvAdapter.clear();
    }

    @Override
    public void onEventViewProgressUpdate(Event... items) {
        mEvAdapter.addAll(items);
    }

    @Override
    public void onEventViewCancelled() {
        System.err.println("!!! " + TAG + " onEventViewCancelled");

        mSwipeView.setRefreshing(false);
        mEvAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEventViewPostExecute() {
        System.err.println("!!! " + TAG + " onEventViewPostExecute");

        mSwipeView.setRefreshing(false);
        mEvAdapter.notifyDataSetChanged();
    }
}
