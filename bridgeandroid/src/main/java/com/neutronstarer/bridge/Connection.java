package com.neutronstarer.bridge;

import com.neutronstarer.bridge.functions.Function2Void;
import com.neutronstarer.bridge.functions.Function3;

public class Connection {

    /**
     * @return connection id
     */
    public String getId(){
        return id;
    }

    /**
     * @param method emit method
     * @param payload emit payload
     */
    public void emit(final String method, final Object payload){
        _emit.invoke(method, payload);
    }

    /**
     * @param method deliver method
     * @param payload deliver payload
     * @param reply reply
     * @return cancel context
     */
    public Object deliver(final String method, final Object payload, final Function2Void<Object, Object> reply){
        return _deliver.invoke(method, payload, reply);
    }

    Connection(final String id, final Function2Void<String, Object> emit, final Function3<String, Object, Function2Void<Object, Object>, Object> deliver){
        super();
        this.id = id;
        this._emit = emit;
        this._deliver = deliver;
    }

    private final String id;
    private final Function2Void<String, Object> _emit;
    private final Function3<String, Object, Function2Void<Object, Object>, Object> _deliver;

}
