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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private final Map<String, Integer> numSeverity = new HashMap<>();

    public EventViewFragment() {
        numSeverity.put("CRIT", 0);
        numSeverity.put("WARN", 1);
        numSeverity.put("INFO", 2);
        numSeverity.put("OK",   3);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.err.println("!!! EventViewFragment onCreate");

        Activity activity = getActivity();
        mContext = activity.getApplicationContext();

        setRetainInstance(true);

        BroadcastReceiver mEventsBroadcastReceiver = new BroadcastReceiver() {
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

            ArrayList<Event> l = new ArrayList<>(groups.values());
            Collections.sort(l, new Comparator<Event>() {
                @Override
                public int compare(Event lhs, Event rhs) {
                    int severity = numSeverity.get(lhs.getSeverity()).compareTo(numSeverity.get(rhs.getSeverity()));
                    if (severity == 0) {
                        return lhs.getSubject().compareTo(rhs.getSubject());
                    }
                    return severity;
                }
            });

            for (Event ev : l) {
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
