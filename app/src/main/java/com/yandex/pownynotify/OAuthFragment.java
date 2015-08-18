package com.yandex.pownynotify;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebViewFragment;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.YandexApi;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class OAuthFragment extends WebViewFragment {
    private String mCode;

    public OAuthFragment(String code) {
        mCode = code;
    }

    interface OAuthCallbacks {
        void onPreExecute();
        void onCancelled();
        void onPostExecute(AsyncTaskResult<Token> accessToken);
    }

    private OAuthCallbacks mCallbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (OAuthCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Create and execute the background task.
        TokenGet mTask = new TokenGet();
        mTask.execute(mCode);
    }

    private class TokenGet extends AsyncTask<String, Void, AsyncTaskResult<Token>> {
        final Token EMPTY_TOKEN = null;

        @Override
        protected AsyncTaskResult<Token> doInBackground(String... args) {
            Token token;
            try {
                OAuthService service = new ServiceBuilder()
                        .provider(YandexApi.class)
                        .apiKey(OAuthCredentials.clientId)
                        .apiSecret(OAuthCredentials.clientSecret)
                        .build();

                Verifier verifier = new Verifier(args[0]);
                token = service.getAccessToken(EMPTY_TOKEN, verifier);

            } catch (OAuthException e) {
                return new AsyncTaskResult<>(e);
            }
            return new AsyncTaskResult<>(token);
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute();
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<Token> accessToken) {
            if (mCallbacks != null) {
                mCallbacks.onPostExecute(accessToken);
            }
        }
    }
}