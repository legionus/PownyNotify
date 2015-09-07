package com.yandex.pownynotify;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.YandexApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class DeleteEventFragment extends Fragment {
    private Context mContext;
    private String mEventId;
    private String mEventSubject;

    interface Callbacks {
        void onPostDeleteEventExecute(AsyncTaskResult<Integer> result);
    }

    private Callbacks mCallbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        SharedPreferences mPref = activity.getSharedPreferences("PownyAppPref", activity.MODE_PRIVATE);

        String mOAuthToken  = mPref.getString("OAuthToken", "");
        String mOAuthSecret = mPref.getString("OAuthSecret", "");

        mContext      = activity.getApplicationContext();
        mEventId      = getArguments().getString("EventId", "");
        mEventSubject = getArguments().getString("EventSubject", "");

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Create OAuthToken
        Token token = new Token(mOAuthToken, mOAuthSecret);

        // Create and execute the background task.
        DeleteEvent mTask = new DeleteEvent();
        mTask.execute(token);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("EventId", mEventId);
        outState.putString("EventSubject", mEventSubject);
    }

    private class DeleteEvent extends AsyncTask<Token, Event, AsyncTaskResult<Integer>> {
        @Override
        protected AsyncTaskResult<Integer> doInBackground(Token... tokens) {
            Response response;

            try {
                DatabaseEvents db = new DatabaseEvents(mContext);

                OAuthService service = new ServiceBuilder()
                        .provider(YandexApi.class)
                        .apiKey(OAuthCredentials.clientId)
                        .apiSecret(OAuthCredentials.clientSecret)
                        .build();

                if (!mEventId.isEmpty()) {
                    OAuthRequest request = new OAuthRequest(Verb.DELETE, BackendParams.apiUrl + "?id=" + mEventId);

                    service.signRequest(tokens[0], request);
                    response = request.send();

                    System.err.println("!!! DeleteEvent (event) " + response.getCode());

                    if (response.getCode() == 401) {
                        return new AsyncTaskResult<>(new Exception("Unauthorized"));
                    }

                    db.deleteRecordById(mEventId);
                }

                if (!mEventSubject.isEmpty()) {
                    OAuthRequest request = new OAuthRequest(Verb.DELETE, BackendParams.apiUrl + "?subject=" + mEventSubject);

                    service.signRequest(tokens[0], request);
                    response = request.send();

                    System.err.println("!!! DeleteEvent (group) " + response.getCode());

                    if (response.getCode() == 401) {
                        return new AsyncTaskResult<>(new Exception("Unauthorized"));
                    }

                    db.deleteRecordsByGroup(mEventSubject);
                }

                Context mContext = getActivity().getApplicationContext();
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("EventsView"));

            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }

            return new AsyncTaskResult<>(0);
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<Integer> result) {
            if (mCallbacks != null) {
                mCallbacks.onPostDeleteEventExecute(result);
            }
        }
    }
}
