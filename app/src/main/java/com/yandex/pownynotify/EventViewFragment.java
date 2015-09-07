package com.yandex.pownynotify;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

public class EventViewFragment extends Fragment {
    interface Callbacks {
        void onEventViewPreExecute();
        void onEventViewProgressUpdate(Event... items);
        void onEventViewCancelled();
        void onEventViewPostExecute();
    }

    private Callbacks mCallbacks;
    private EventsView mTask;
    private Context mContext;
    private BroadcastReceiver mEventsBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.err.println("!!! EventViewFragment onCreate");

        Activity activity = getActivity();
        mContext = activity.getApplicationContext();

        setRetainInstance(true);

        mEventsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.err.println("!!! EventViewFragment onReceive");

                mTask = new EventsView();
                mTask.execute();
            }
        };
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mEventsBroadcastReceiver, new IntentFilter("EventsView"));
    }

    @Override
    public void onAttach(Activity activity) {
        System.err.println("!!! EventViewFragment onAttach");

        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        System.err.println("!!! EventViewFragment onDetach");

        super.onDetach();
        mCallbacks = null;
    }

    private class EventsView extends AsyncTask<Void, Event, Void> {
        @Override
        protected Void doInBackground(Void... Params) {
            DatabaseEvents db = new DatabaseEvents(mContext);

            LinkedHashMap<String, Event> groups = new LinkedHashMap<>();
            ArrayList<Event> messages = db.selectAllRecords();

            for (int i = 0; i < messages.size(); i++) {
                Event ev = messages.get(i);

                if (groups.containsKey(ev.getSubject())) {
                    groups.get(ev.getSubject()).incCount();
                    continue;
                }
                groups.put(ev.getSubject(), ev);
            }

            Collection c = groups.values();
            for (Object aC : c) {
                Event ev = (Event) aC;
                publishProgress(ev);
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onEventViewPreExecute();
            }
        }

        @Override
        protected void onProgressUpdate(Event... items) {
            if (mCallbacks != null) {
                mCallbacks.onEventViewProgressUpdate(items);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onEventViewCancelled();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mCallbacks != null) {
                mCallbacks.onEventViewPostExecute();
            }
        }
    }
}
