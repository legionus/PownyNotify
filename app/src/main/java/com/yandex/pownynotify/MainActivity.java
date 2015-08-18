package com.yandex.pownynotify;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity  implements EventTaskFragment.TaskCallbacks, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG_TASK_FRAGMENT = "powny_fragment";

    public static final int OAUTH_REQUEST = 1;

    public SwipeRefreshLayout mSwipeView;

    public ListView mList;
    public EventAdapter<Event> mEvAdapter;

    public SharedPreferences mPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.err.println("!!! MainActivity onCreate");

        mPref = getSharedPreferences("PownyAppPref", MODE_PRIVATE);


        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mSwipeView.setRefreshing(false);
        mSwipeView.setOnRefreshListener(this);

        ArrayList<Event> eventList = new ArrayList<>();

        mEvAdapter = new EventAdapter(this, R.layout.listview_item, eventList);

        mList = (ListView) findViewById(R.id.topList);
        mList.setAdapter(mEvAdapter);

        GetEvents();
    }

    public void GetEvents() {
        System.err.println("!!! MainActivity GetEvents");

        FragmentManager fm = getFragmentManager();
        EventTaskFragment mTaskFragment = (EventTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        FragmentTransaction ft = fm.beginTransaction();

        if (mTaskFragment != null) {
            ft.remove(mTaskFragment);
        }

        Bundle args = new Bundle();
        args.putString("OAuthToken",  mPref.getString("OAuthToken", ""));
        args.putString("OAuthSecret", mPref.getString("OAuthSecret", ""));

        mTaskFragment = new EventTaskFragment();
        mTaskFragment.setArguments(args);

        ft.add(mTaskFragment, TAG_TASK_FRAGMENT);
        ft.commit();
    }

    @Override
    public void onRefresh() {
        GetEvents();
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
                    GetEvents();
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
}
