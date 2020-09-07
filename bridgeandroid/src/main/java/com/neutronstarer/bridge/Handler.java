package com.neutronstarer.bridge;

import com.neutronstarer.bridge.functions.Function1Void;
import com.neutronstarer.bridge.functions.Function2Void;
import com.neutronstarer.bridge.functions.Function3;

public class Handler {

    /**
     *  handle event
     * @param  onEvent event handler
     * @return current handler
     */
    public Handler onEvent(final Function3<Connection, Object, Function2Void<Object, Object>, Object> onEvent){
        this.event = onEvent;
        return this;
    }

    /**
     * handle cancel
     * @param onCancel cancel handler
     * @return current handler
     */
    public Handler onCancel(final Function1Void<Object> onCancel){
        this.cancel = onCancel;
        return this;
    }

    Function3<Connection, Object, Function2Void<Object, Object>, Object> event;
    Function1Void<Object> cancel;

}
