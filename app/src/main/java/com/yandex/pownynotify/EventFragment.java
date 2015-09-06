package com.yandex.pownynotify;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.YandexApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.Collection;
import java.util.LinkedHashMap;

public class EventFragment extends Fragment {
    private Context mContext;
    private String mOAuthToken;
    private String mOAuthSecret;

    public BroadcastReceiver mEventsBroadcastReceiver;

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface Callbacks {
        void onPreExecute();
        void onProgressUpdate(Event... items);
        void onCancelled();
        void onPostExecute(AsyncTaskResult<JSONObject> result);
    }

    private Callbacks mCallbacks;
    private EventsTask mTask;
    SharedPreferences mPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.err.println("!!! EventFragment onCreate");

        Activity activity = getActivity();

        mPref    = activity.getSharedPreferences("PownyAppPref", activity.MODE_PRIVATE);
        mContext = activity.getApplicationContext();

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        mEventsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.err.println("!!! EventFragment onReceive");

                mOAuthToken = mPref.getString("OAuthToken", "");
                mOAuthSecret = mPref.getString("OAuthSecret", "");

                if (mOAuthToken.isEmpty()) {
                    System.err.println("!!! EventFragment onReceive OAuthToken empty!");
                    return;
                }

                mTask = new EventsTask();
                mTask.execute(new Token(mOAuthToken, mOAuthSecret));
            }
        };
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mEventsBroadcastReceiver, new IntentFilter("GetEvents"));
    }

    @Override
    public void onAttach(Activity activity) {
        System.err.println("!!! EventFragment onAttach");

        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        System.err.println("!!! EventFragment onDetach");

        super.onDetach();
        mCallbacks = null;
        //LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mEventsBroadcastReceiver);
    }

    private class EventsTask extends AsyncTask<Token, Event, AsyncTaskResult<JSONObject>> {
        private static final String TAG = "EventTask";

        @Override
        protected AsyncTaskResult<JSONObject> doInBackground(Token... tokens) {
            Response response;

            try {
                OAuthService service = new ServiceBuilder()
                        .provider(YandexApi.class)
                        .apiKey(OAuthCredentials.clientId)
                        .apiSecret(OAuthCredentials.clientSecret)
                        .build();

                OAuthRequest request = new OAuthRequest(Verb.GET, BackendParams.apiUrl);

                service.signRequest(tokens[0], request);
                response = request.send();

                System.err.println("!!! EventsTask " + response.getCode());

                if (response.getCode() == 401) {
                    return new AsyncTaskResult<>(new Exception("Unauthorized"));
                }

            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }

            JSONArray Messages;
            JSONObject ServerData;

            try {
                if (isCancelled()) {
                    return new AsyncTaskResult<>(new Exception("isCanceled"));
                }
                ServerData = new JSONObject(response.getBody());

                if (! "success".equals(ServerData.getString("status"))) {
                    throw new Exception("Bad status: " + ServerData.getString("message"));
                }
                Messages = ServerData.getJSONArray("messages");
                System.err.println("!!! EventsTask Got " + Messages.length() + " messages");

            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }

            DatabaseEvents db = new DatabaseEvents(mContext);
            int n = db.deleteRecords();
            System.err.println("!!! EventsTask removed from DB " + n + " messages");

            LinkedHashMap<String, Event> groups = new LinkedHashMap<>();

            for (int i = 0; !isCancelled() && i < Messages.length(); i++) {
                Event ev = new Event();

                try {
                    JSONObject m = Messages.getJSONObject(i);

                    ev.setId(m.getString(BackendParams.JSON_KEY_ID));
                    ev.setSubject(m.getString(BackendParams.JSON_KEY_SUBJECT));
                    ev.setBody(m.getString(BackendParams.JSON_KEY_DETAILS));
                    ev.setSeverity(m.getString(BackendParams.JSON_KEY_SEVERITY));
                    ev.setTime((long) m.getInt(BackendParams.JSON_KEY_EVENTTIME));

                } catch (JSONException e) {
                    return new AsyncTaskResult<>(e);
                }

                try {
                    db.createRecord(ev);
                } catch (SQLiteConstraintException e) {
                    Log.e(TAG, "Unable to add record: " + ev.getId());
                    continue;
                }

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

            return new AsyncTaskResult<>(ServerData);
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute();
            }
        }

        @Override
        protected void onProgressUpdate(Event... items) {
            if (mCallbacks != null) {
                mCallbacks.onProgressUpdate(items);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<JSONObject> result) {
            if (mCallbacks != null) {
                mCallbacks.onPostExecute(result);
            }
        }
    }
}
