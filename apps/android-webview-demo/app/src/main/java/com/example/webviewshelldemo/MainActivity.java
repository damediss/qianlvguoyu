package com.example.webviewshelldemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    public static final String EXTRA_WEB_URL = "web_url";
    private static final String TAG = "WebViewShell";

    private WebView webView;
    private FingerprintSdkAdapter fingerprintSdkAdapter;
    private QrCodeScanHelper qrCodeScanHelper;
    private AndroidBridge androidBridge;
    private final List<String> remainingFallbackUrls = new ArrayList<>();

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
        qrCodeScanHelper = new QrCodeScanHelper(this);

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
        androidBridge = new AndroidBridge(this, view, fingerprintSdkAdapter, qrCodeScanHelper);
        view.addJavascriptInterface(androidBridge, "AndroidBridge");
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView webView, String url, Bitmap favicon) {
                super.onPageStarted(webView, url, favicon);
                setTitle(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request == null || !request.isForMainFrame() || remainingFallbackUrls.isEmpty()) {
                    return;
                }
                String nextUrl = remainingFallbackUrls.remove(0);
                Log.w(TAG, "Main frame failed: " + request.getUrl()
                        + " error=" + (error == null ? "?" : error.getErrorCode())
                        + " retry=" + nextUrl);
                view.loadUrl(nextUrl);
            }
        });
    }

    private String resolveInitialUrl(Intent intent) {
        String primary = getString(R.string.default_web_url);
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && data.getScheme() != null) {
                primary = data.toString();
            } else {
                String extraUrl = intent.getStringExtra(EXTRA_WEB_URL);
                if (extraUrl != null && !extraUrl.trim().isEmpty()) {
                    primary = extraUrl.trim();
                }
            }
        }
        remainingFallbackUrls.clear();
        remainingFallbackUrls.addAll(buildFallbackUrls(primary));
        return primary;
    }

    private List<String> buildFallbackUrls(String primary) {
        List<String> fallbacks = new ArrayList<>();
        Uri primaryUri = Uri.parse(primary);
        if (primaryUri == null || primaryUri.getHost() == null) {
            return fallbacks;
        }
        for (String host : getResources().getStringArray(R.array.fallback_web_hosts)) {
            if (host == null || host.trim().isEmpty() || host.equals(primaryUri.getHost())) {
                continue;
            }
            int port = primaryUri.getPort();
            String authority = port > 0 ? host + ":" + port : host;
            Uri next = primaryUri.buildUpon().encodedAuthority(authority).build();
            fallbacks.add(next.toString());
        }
        return fallbacks;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (qrCodeScanHelper != null) {
            qrCodeScanHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (qrCodeScanHelper != null) {
            qrCodeScanHelper.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
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
