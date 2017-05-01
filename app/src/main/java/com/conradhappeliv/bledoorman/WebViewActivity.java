package com.conradhappeliv.bledoorman;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

public class WebViewActivity extends AppCompatActivity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        mWebView = (WebView) findViewById(R.id.main_web_view);
        mWebView.loadUrl(url);
    }

}
