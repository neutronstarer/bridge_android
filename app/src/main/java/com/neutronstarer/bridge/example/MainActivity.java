package com.neutronstarer.bridge.example;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.neutronstarer.bridge.Connection;
import com.neutronstarer.bridge.Server;
import com.neutronstarer.bridge.functions.Function1Void;
import com.neutronstarer.bridge.functions.Function2Void;
import com.neutronstarer.bridge.functions.Function3;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WebView webView=findViewById(R.id.webView);
        WebView.setWebContentsDebuggingEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.d("web", url);
                if (Server.canHandle(webView, url)) {
                    return null;
                }
                return super.shouldInterceptRequest(view, url);
            }
        });
        final Server server = Server.create(webView, null);
        server.on("connect").onEvent(new Function3<Connection, Object, Function2Void<Object, Object>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function2Void<Object, Object> reply) {
                connections.add(connection);
                Log.d("app", "connected");
                return null;
            }
        });

        server.on("disconnect").onEvent(new Function3<Connection, Object, Function2Void<Object, Object>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function2Void<Object, Object> reply) {
                connections.remove(connection);
                Log.d("app", "disconnected");
                return null;
            }
        });

        server.on("request").onEvent(new Function3<Connection, Object, Function2Void<Object, Object>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, final Function2Void<Object, Object> reply) {
                connections.remove(connection);
                Log.d("app", "disconnected");
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        reply.invoke("[\\] ['] [\"] [\b] [\f] [\n] [\r] [\t] [\u2028] [\u2029]",null);
                    }
                };
                handler.postDelayed(runnable,2000);
                return runnable;
            }
        }).onCancel(new Function1Void<Object>() {
            @Override
            public void invoke(Object o) {
                final Runnable runnable = (Runnable) o;
                handler.removeCallbacks(runnable);
                Log.d("app", "do cancel");
            }
        });
        reload(null);
    }

    public void reload(View v){
        final WebView webView=findViewById(R.id.webView);
        webView.clearCache(true);
        webView.reload();
        // ????
        webView.loadUrl("file:///android_asset/index.html");
    }

    private final ArrayList<Connection> connections = new ArrayList<>();
    private static final Handler handler=new Handler(Looper.getMainLooper());
}
