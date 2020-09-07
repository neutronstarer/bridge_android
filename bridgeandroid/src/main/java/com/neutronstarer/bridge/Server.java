    package com.neutronstarer.bridge;

    import android.content.Context;
    import android.webkit.JavascriptInterface;
    import android.webkit.ValueCallback;
    import android.webkit.WebView;
    import com.neutronstarer.bridge.functions.Function0Void;
    import com.neutronstarer.bridge.functions.Function1Void;
    import com.neutronstarer.bridge.functions.Function2Void;
    import com.neutronstarer.bridge.functions.Function3;
    import java.io.IOException;
    import java.io.InputStream;
    import java.net.URLDecoder;
    import java.util.HashMap;
    import java.util.Locale;
    import java.util.Map;
    import java.util.WeakHashMap;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    public class Server {
        /**
         * get instance of Server.server's life cycle is associated web view.
         * @param webView web view.
         * @param name namespace is used to mark different service.
         * @return server
         */
        public static Server create(final WebView webView, final String name){
            return createIfNotExists(webView, name, true);
        }

        /**
         * check that any server can handle url.
         * @param webView web view
         * @param urlString url string
         * @return can?
         */
        public static boolean canHandle(final WebView webView, final String urlString){
            final Matcher matcher = regex.matcher(urlString);
            if (!matcher.find()) return false;
            final String action = matcher.group(1);
            String name = null;
            try {
                name = URLDecoder.decode(matcher.group(2), "utf-8");
            }catch (Throwable t){
                t.printStackTrace();
            }
            final Server server = createIfNotExists(webView, name, false);
            if (server == null) return true;
            if (action == null) return true;
            switch (action) {
                case "load":
                    server.load();
                    break;
                case "query":
                    //is unreachable for android platform.
                    break;
                default:
                    break;
            }
            return true;
        }

        /**
         * create a handler by event type.
         * @param type event type
         * @return handler
         */
        public final Handler on(final String type){
            Handler handler;
            synchronized (handlerByMethod){
                handler = handlerByMethod.get(type);
                if (null != handler) {
                    return handler;
                }
                handler = new Handler();
                handlerByMethod.put(type, handler);
                return handler;
            }
        }

        @JavascriptInterface
        public void postMessage(final String s){
            try {
                final Message message = new Message(s);
                final long   mid = message.id;
                final String from = message.from;
                final String type = message.type;
                final String method = message.method;
                final Object payload = message.payload;
                final Object error  = message.error;
                if (null == from || from.length() == 0){
                    return;
                }
                if (type.equals("connect")){
                    if (connectionById.get(from) != null){
                        return;
                    }
                    final Connection connection = new Connection(from, new Function2Void<String, Object>() {
                        @Override
                        public void invoke(String method, Object payload) {
                            emit(from, method, payload);
                        }
                    }, new Function3<String, Object, Function2Void<Object, Object>, Object>() {
                        @Override
                        public Object invoke(String method, Object payload, Function2Void<Object, Object> reply) {
                            return deliver(from, method, payload, reply);
                        }
                    });
                    connectionById.put(from, connection);
                    final  Handler handler = handlerByMethod.get("connect");
                    if (handler ==null){
                        return;
                    }
                    handler.event.invoke(connection, payload, new Function2Void<Object, Object>() {
                        @Override
                        public void invoke(Object ack, Object error) {
                        }
                    });
                    return;
                }
                final Connection connection = connectionById.get(from);
                if (connection == null){
                    return;
                }
                if (type.equals("disconnect")){
                    connectionById.remove(from);
                    final String prefix = String.format("%s-", from);
                    for (Map.Entry<String, Operation> entry : operationById.entrySet()) {
                        final String operationId = entry.getKey();
                        if (operationId.startsWith(prefix)) {
                            completeOperation(operationId, null, "disconnected");
                        }
                    }
                    final Handler handler = handlerByMethod.get("disconnect");
                    if (handler ==null){
                        return;
                    }
                    handler.event.invoke(connection, payload, new Function2Void<Object, Object>() {
                        @Override
                        public void invoke(Object ack, Object error) {
                        }
                    });
                    return;
                }

                if (type.equals("emit")){
                    final Handler handler = handlerByMethod.get(method);
                    if (handler ==null){
                        return;
                    }
                    handler.event.invoke(connection, payload, new Function2Void<Object, Object>() {
                        @Override
                        public void invoke(Object ack, Object error) {
                        }
                    });
                }

                if (type.equals("ack")){
                    final String operationId = operationIdOf(from, mid);
                    completeOperation(operationId, payload, error);
                    return;
                }

                if (type.equals("cancel")) {
                    final String cancelId = cancelIdOf(from, mid);
                    final Function0Void cancel = cancelById.get(cancelId);
                    if (cancel == null) {
                        return;
                    }
                    cancel.invoke();
                    return;
                }

                if (type.equals("deliver")){
                    final Function2Void<Object, Object> reply = new Function2Void<Object, Object>() {
                        @Override
                        public void invoke(Object o, Object o2) {
                            final Message message = new Message();
                            message.id = mid;
                            message.to = from;
                            message.type = "ack";
                            message.payload = o;
                            message.error = o2;
                            sendMessage(message,null);
                        }
                    };
                    final Handler handler = handlerByMethod.get(method);
                    if (handler == null){
                        reply.invoke(null, "unsupported method");
                        return;
                    }
                    final String cancelId = cancelIdOf(from, mid);
                    final Object cancelContext = handler.event.invoke(connection, payload, new Function2Void<Object, Object>() {
                        @Override
                        public void invoke(Object ack, Object error) {
                            if (cancelById.get(cancelId) == null){
                                return;
                            }
                            cancelById.remove(cancelId);
                            reply.invoke(ack, error);
                        }
                    });
                    final Function1Void<Object> cancel = handler.cancel;
                    cancelById.put(cancelId, new Function0Void() {
                        @Override
                        public void invoke() {
                            if (cancelById.get(cancelId) == null){
                                return;
                            }
                            cancelById.remove(cancelId);
                            if (cancel == null){
                                return;
                            }
                            cancel.invoke(cancelContext);
                        }
                    });
                }
            }catch (Throwable t){
                t.printStackTrace();
            }
        }

        private void emit(final String connectionId, final String method, final Object payload){
            final long mid = idx++;
            final Message message = new Message();
            message.id = mid;
            message.type = "emit";
            message.to = connectionId;
            message.method = method;
            message.payload = payload;
            sendMessage(message,null);
        }

        private Operation deliver(final String connectionId, final String method, final Object payload, final Function2Void<Object, Object> reply){
            final long mid = idx++;
            final String operationId = operationIdOf(connectionId, mid);
            final Operation operation = new Operation(reply);
            operationById.put(operationId, operation);
            final Message message = new Message();
            message.id = mid;
            message.type = "deliver";
            message.to = connectionId;
            message.method = method;
            message.payload = payload;
            sendMessage(message, new Function1Void<String>() {
                @Override
                public void invoke(String s) {
                    if (s== null || s.equals("false")) {
                        completeOperation(operationId, null, "fail to send message");
                    }
                }
            });
            return operation;
        }

        private void completeOperation(final String id, final Object ack, final Object error){
            final Operation operation = operationById.get(id);
            if (operation == null) return;
            operation.complete(ack, error);
            operationById.remove(id);
        }

        private void sendMessage(Message message, final Function1Void<String> completion){
            try {
                final String jsonString = message.toJsonString();
                if (jsonString.length() == 0){
                    if (completion !=null) completion.invoke("false");
                    return;
                }
                final String js =String.format(transmitFormat, name, StringUtil.escape(jsonString));
                evaluate.invoke(js, new Function1Void<String>() {
                    @Override
                    public void invoke(String s) {
                        if (completion != null) completion.invoke(s);
                    }
                });
            }catch (Throwable t){
                completion.invoke("false");
                t.printStackTrace();
            }
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void load() {
            InputStream inputStream;
            try {
                //file:///android_asset/hub.js
                inputStream = context.getAssets().open("hub.js");
                byte[] bytes;
                bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                String js = new String(bytes);
                evaluate.invoke(js, null);
             } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private final String name;
        private final Context context;
        private final Function2Void<String, Function1Void<String>> evaluate;
        private final Map<String, Handler> handlerByMethod = new HashMap<>();
        private final Map<String, Connection> connectionById = new HashMap<>();
        private final Map<String, Function0Void> cancelById = new HashMap<>();
        private final Map<String, Operation> operationById = new HashMap<>();
        private long idx = 0;

        Server(final WebView webView, final String name){
            super();
            this.name = name;
            this.evaluate = new Function2Void<String, Function1Void<String>>() {
                @Override
                public void invoke(String s, final Function1Void<String> callback) {
                    final  String js = s;
                    final Runnable runnable=new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript(js, new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {
                                    if (callback == null) return;
                                    callback.invoke(s);
                                }
                            });
                        }
                    };
                    webView.post(runnable);
                }
            };
            this.context = webView.getContext();
            webView.addJavascriptInterface(this, name);
        }

        private String operationIdOf(final String connectionId, final long mid){
            return String.format(Locale.getDefault(),"%s-%d", connectionId, mid);
        }

        private String cancelIdOf(final String connectionId, final long mid){
            return String.format(Locale.getDefault(),"%s-%d", connectionId, mid);
        }

    //    private static final String queryFormat = ";(function(){try{return window['bridge_hub_%s'].query();}catch(e){return '[]'};})();";
        private static final String transmitFormat = ";(function(){try{return window['bridge_hub_%s'].transmit('%s');}catch(e){return false};})();";
        private static final Pattern regex = Pattern.compile("^https://bridge/([^/]+)\\?name=(.+)$");
        private static final WeakHashMap<WebView, HashMap<String, Server>> serverByWebView = new WeakHashMap<>();

        private static Server createIfNotExists(final WebView webView, String name, final boolean createIfNotExists){
            if (null == name || name.isEmpty()){
                name = "<name>";
            }
            synchronized (Server.class){
                HashMap<String, Server> serverByName = serverByWebView.get(webView);
                if (serverByName == null){
                    serverByName = new HashMap<>();
                    serverByWebView.put(webView, serverByName);
                }
                Server server =serverByName.get(name);
                if (!createIfNotExists) return server;
                if (server != null) return server;
                server = new Server(webView, name);
                serverByName.put(name, server);
                return server;
            }
        }
    }
