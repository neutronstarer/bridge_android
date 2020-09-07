
package com.neutronstarer.bridge;

import java.util.HashMap;
import java.util.Map;

class Message {
    long      id;
    String    type;
    String    from;
    String    to;
    String    method;
    Object    payload;
    Object    error;
    Message(){
        super();
    }
    @SuppressWarnings("unchecked")
    Message(final String s) throws Throwable{
        this();
        if (null==s||s.isEmpty()){
            return;
        }
        Map<String,Object> map = (Map<String,Object>) Json.toJavaObject(s);
        Object o = map.get("id");
        if (o != null){
            id = Long.parseLong(o.toString());
        }
        o = map.get("type");
        if (o != null){
            type = o.toString();
        }
        o = map.get("from");
        if (o != null){
            from = o.toString();
        }
        o = map.get("to");
        if (o != null){
            to = o.toString();
        }
        o = map.get("method");
        if (o != null){
            method = o.toString();
        }
        payload = map.get("payload");
        error = map.get("error");
    }

    String toJsonString() throws Throwable{
        Map<String,Object> map = new HashMap<>();
        map.put("id", id);
        if (null != type){
            map.put("type", type);
        }
        if (null != from){
            map.put("from", from);
        }
        if (null != to){
            map.put("to", to);
        }
        if (null != method){
            map.put("method", method);
        }
        if (null != payload){
            map.put("payload", payload);
        }
        if (null != error){
            map.put("error", error);
        }
        return Json.toJsonString(map);
    }
}
