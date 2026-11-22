package com.cloudpc.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "cloudpc";
    private static final String URL_KEY = "server_url";
    private static final String DEFAULT_URL = "http://localhost:3000";

    private WebView webView;
    private ProgressBar progressBar;
    private boolean fallbackShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 12));

        root.addView(webView);
        root.addView(progressBar);
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void openSettings() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.openSettings();
                    }
                });
            }
        }, "CloudPC");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                fallbackShown = false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame() && !fallbackShown) {
                    fallbackShown = true;
                    showFallback();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                if (!fallbackShown) {
                    fallbackShown = true;
                    showFallback();
                }
            }

        });

        loadServer();
    }

    private String getServerUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(URL_KEY, DEFAULT_URL).trim();
        if (url.isEmpty()) {
            return DEFAULT_URL;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }

    private void loadServer() {
        webView.loadUrl(getServerUrl());
    }

    private void showFallback() {
        String html = "<!doctype html><html><head><meta name=\"viewport\" "
                + "content=\"width=device-width,initial-scale=1\"><style>"
                + "body{margin:0;background:#020617;color:#fff;font-family:system-ui,"
                + "-apple-system,sans-serif;display:grid;place-items:center;min-height:100vh}"
                + ".card{width:min(88%,420px);text-align:center}.logo{width:64px;height:64px;"
                + "margin:auto;border-radius:18px;background:linear-gradient(135deg,#38bdf8,#2563eb);"
                + "display:grid;place-items:center;font-size:32px}.muted{color:#94a3b8}"
                + "button{width:100%;border:0;border-radius:12px;padding:14px;margin-top:16px;"
                + "background:#2563eb;color:#fff;font-size:16px;font-weight:700}</style></head>"
                + "<body><div class=\"card\"><div class=\"logo\">&#9638;</div>"
                + "<h1>CloudPC</h1><p class=\"muted\">The CloudPC server is not reachable.</p>"
                + "<p class=\"muted\">Enter a deployed web address or the LAN address of your server.</p>"
                + "<button onclick=\"CloudPC.openSettings()\">Set server address</button>"
                + "</div>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    public void openSettings() {
        final EditText input = new EditText(this);
        input.setText(getServerUrl());
        input.setHint("https://cloudpc.example.com");
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("CloudPC server")
                .setMessage("Enter the address of the deployed CloudPC web app.")
                .setView(input)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = input.getText().toString().trim();
                        if (!url.isEmpty()) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "http://" + url;
                            }
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                    .edit().putString(URL_KEY, url).apply();
                            webView.loadUrl(url);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
