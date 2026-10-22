package ir.taklifyar.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.Toast;
import java.io.*;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int PICK_JSON = 2041;
    private static final int CREATE_JSON = 2042;
    private String pendingBackupJson;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF08111F);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setDatabaseEnabled(true);
        s.setSupportMultipleWindows(false);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "text/plain", "*/*"});
                startActivityForResult(intent, PICK_JSON);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void toast(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
            }
            @android.webkit.JavascriptInterface
            public void saveBackup(String json) {
                runOnUiThread(() -> {
                    pendingBackupJson = json;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "taklifyar-backup.json");
                    startActivityForResult(intent, CREATE_JSON);
                });
            }
        }, "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_JSON && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) results = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        } else if (requestCode == CREATE_JSON) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingBackupJson != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new IOException("مسیر ذخیره‌سازی در دسترس نیست");
                    out.write(pendingBackupJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                    Toast.makeText(this, "فایل پشتیبان ذخیره شد.", Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(this, "ذخیره فایل پشتیبان ناموفق بود.", Toast.LENGTH_LONG).show();
                }
            }
            pendingBackupJson = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
