package com.nuix.proserv.t3k.ws.metadata;

import nuix.CustomMetadataMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomMetadataMapMock implements CustomMetadataMap {
    Map<String, Object> backingMap = new HashMap<>();

    @Override
    public Object putText(String s, Object o) {
        backingMap.put(s, o);
        return o;
    }

    @Override
    public Object putDate(String s, Object o) {
        backingMap.put(s, o);
        return o;
    }

    @Override
    public Object putDate(String s, Object o, Map<?, ?> map) {
        backingMap.put(s, o);
        return o;
    }

    @Override
    public Object putInteger(String s, Object o) {
        backingMap.put(s, o);
        return o;
    }

    @Override
    public Object putFloat(String s, Object o) {
        backingMap.put(s, o);
        return  o;
    }

    @Override
    public Object putBoolean(String s, Object o) {
        backingMap.put(s, o);
        return o;
    }

    @Override
    public Object put(String s, Object o, String s1, String s2, Map<?, ?> map) {
        backingMap.put(s, o);
        return o;
    }

    @Override
    public String getType(String s) {
        throw new RuntimeException("This method is not implemented.");
    }

    @Override
    public String getMode(String s) {
        throw new RuntimeException("This method is not implemented.");
    }

    @Override
    public int size() {
        return this.backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.backingMap.size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return backingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backingMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return backingMap.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return backingMap.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return backingMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        backingMap.putAll(m);
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return backingMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return backingMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return backingMap.entrySet();
    }
}
