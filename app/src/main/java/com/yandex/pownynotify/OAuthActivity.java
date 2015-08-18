package com.yandex.pownynotify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.YandexApi;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public class OAuthActivity extends Activity implements OAuthFragment.OAuthCallbacks {
    private static final String TAG_TASK_FRAGMENT = "powny_oauth_fragment";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.oauthwebview);

        final Activity activity = this;

        OAuthService service = new ServiceBuilder()
                .provider(YandexApi.class)
                .apiKey(OAuthCredentials.clientId)
                .apiSecret(OAuthCredentials.clientSecret)
                .build();

        final ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setVisibility(View.VISIBLE);

        final WebView webview = (WebView) findViewById(R.id.OAuthWebView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setVisibility(View.VISIBLE);

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl.startsWith(OAuthCredentials.redirectUrl)) {
                    return;
                }

                System.err.println("!!! Error (code=" + errorCode + "): " + failingUrl + ": " + description);
                Toast.makeText(activity, "Error: " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageFinished(final WebView view, final String url) {
                progress.setProgress(0);
                //progress.setVisibility(View.GONE);

                if (url.startsWith(OAuthCredentials.redirectUrl)) {
                    webview.setVisibility(View.INVISIBLE);

                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");

                    GetTokenByCode(code);
                } else {
                    webview.setVisibility(View.VISIBLE);
                }
            }
        });

        webview.loadUrl(service.getAuthorizationUrl(null));
    }

    public void GetTokenByCode(String code) {
        FragmentManager fm = getFragmentManager();
        OAuthFragment mTaskFragment = (OAuthFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        if (mTaskFragment == null) {
            mTaskFragment = new OAuthFragment(code);
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }
    }

    @Override
    public void onPreExecute() {
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onPostExecute(AsyncTaskResult<Token> result) {
        if (result.getError() != null) {
            result.getError().printStackTrace();
        }

        Token accessToken = result.getResult();
        SharedPreferences mPref = getSharedPreferences("PownyAppPref", MODE_PRIVATE);

        SharedPreferences.Editor edit = mPref.edit();
        edit.putString("OAuthToken", accessToken.getToken());
        edit.putString("OAuthSecret", accessToken.getSecret());
        edit.apply();

        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
    }
}
