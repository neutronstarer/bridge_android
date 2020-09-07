package com.neutronstarer.bridge;

import android.os.Handler;
import android.os.Looper;

import com.neutronstarer.bridge.functions.Function2Void;

public class Operation {

    /**
     * @param timeout timeout in second
     * @return this operation
     */
    public Operation setTimeout(final long timeout){
        if (timeout <= 0){
            return this;
        }
        synchronized (this){
            if (done){
                return this;
            }
            if (runnable != null){
                handler.removeCallbacks(runnable);
            }
            runnable = new Runnable() {
                @Override
                public void run() {
                    complete(null, "timed out");
                }
            };
            handler.postDelayed(runnable, timeout);
            return this;
        }
    }

    /**
     * cancel operation
     */
    public void cancel() {
        complete(null, "cancelled");
    }

    Operation(Function2Void<Object, Object> completion){
        this.completion = completion;
    }

    void complete(final Object ack, final Object error){
        synchronized (this){
            if (done){
                return;
            }
            done = true;
            if (runnable != null){
                handler.removeCallbacks(runnable);
                runnable = null;
            }
            completion.invoke(ack, error);
        }
    }

    private final Function2Void<Object, Object> completion;
    private boolean  done;
    private Runnable runnable;
    private static final android.os.Handler handler = new Handler(Looper.getMainLooper());

}
