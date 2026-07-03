package com.example.webviewshelldemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    public static final String EXTRA_WEB_URL = "web_url";

    private WebView webView;
    private FingerprintSdkAdapter fingerprintSdkAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        fingerprintSdkAdapter = new FingerprintSdkAdapter(this);
        fingerprintSdkAdapter.start();

        configureWebView(webView);
        setContentView(webView);
        webView.loadUrl(resolveInitialUrl(getIntent()));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        view.setWebChromeClient(new WebChromeClient());
        view.addJavascriptInterface(new AndroidBridge(this, view, fingerprintSdkAdapter), "AndroidBridge");
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView webView, String url, Bitmap favicon) {
                super.onPageStarted(webView, url, favicon);
                setTitle(url);
            }
        });
    }

    private String resolveInitialUrl(Intent intent) {
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && data.getScheme() != null) {
                return data.toString();
            }

            String extraUrl = intent.getStringExtra(EXTRA_WEB_URL);
            if (extraUrl != null && !extraUrl.trim().isEmpty()) {
                return extraUrl.trim();
            }
        }

        return getString(R.string.default_web_url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (fingerprintSdkAdapter != null) {
            fingerprintSdkAdapter.release();
            fingerprintSdkAdapter = null;
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
