package com.yandex.pownynotify;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class EventDetailsActivity extends Activity {

    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.eventdetails);

        final Activity activity = this;

        System.out.println("!!! EventDetailsActivity");

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        Intent mIntent = getIntent();
        String eventId = mIntent.getStringExtra("eventid");

        DatabaseEvents db = new DatabaseEvents(getApplicationContext());
        Event ev = db.selectRecordById(eventId);

        TextView eventTime = (TextView) findViewById(R.id.EventDetailsTime);
        TextView subject   = (TextView) findViewById(R.id.EventDetailsSubject);
        TextView severity  = (TextView) findViewById(R.id.EventDetailsSeverity);

        eventTime.setText(ev.getTime().toString());
        subject.setText(ev.getSubject());

        switch (ev.getSeverity()) {
            case "CRIT":
                severity.setBackgroundResource(R.color.danger);
                break;
            case "WARN":
                severity.setBackgroundResource(R.color.warning);
                break;
            case "OK":
                severity.setBackgroundResource(R.color.success);
                break;
        }

        final ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setVisibility(View.VISIBLE);

        String body = ev.getBody();
        if (body.isEmpty()) {
            Toast.makeText(activity, "Empty", Toast.LENGTH_SHORT).show();
            return;
        }

        WebView webView = (WebView) findViewById(R.id.eventdetails);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, "Error: " + description, Toast.LENGTH_SHORT).show();
            }
            public void onPageFinished(final WebView view, final String url) {
                progress.setVisibility(View.GONE);
            }
        });

        webView.loadData("<html><body>" + body + "</body></html>", "text/html", "UTF-8");
    }
}
