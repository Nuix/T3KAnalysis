package com.nuix.proserv.t3k.ws;

import nuix.CustomMetadataMap;
import nuix.WorkerItem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorkerItemMetadataMapWrapper implements CustomMetadataMap {
    private final WorkerItem workerItem;

    public WorkerItemMetadataMapWrapper(WorkerItem itemToStoreIn) {
        this.workerItem = itemToStoreIn;
    }

    @Override
    public Object putText(String fieldName, Object value) {
        return put(fieldName, value, "text", "user", Map.of());
    }

    @Override
    public Object putDate(String fieldName, Object value) {
        return put(fieldName, value, "date-time", "user", Map.of());
    }

    /**
     * Store a date time in the worker item, providing time zone and string format
     * @param fieldName the name of the custom metadata field
     * @param value an instance of DateTime, RubyTime, java.utl.Date, or anything that has a toString() method that can be parsed with the passed in "format" or Joda Time's formats.
     * @param options Map containing:
     *                optional "timeZone" key with a String ID that is parsable by Joda Time's DateTimeZone.forID(String) method.
     *                optional "format" key with a string that can be used to parse the date with String.format().
     *
     * @return null - assume this metadata is not yet present in the worker item
     */
    @Override
    public Object putDate(String fieldName, Object value, Map<?, ?> options) {
        return put(fieldName, value, "date-time", "user", options);
    }

    @Override
    public Object putInteger(String fieldName, Object value) {
        return put(fieldName, value, "integer", "user", Map.of());
    }

    @Override
    public Object putFloat(String fieldName, Object value) {
        return put(fieldName, value, "float", "user", Map.of());
    }

    @Override
    public Object putBoolean(String fieldName, Object value) {
        return put(fieldName, value, "boolean", "user", Map.of());
    }

    /**
     * Store the provided value into the wrapped worker item's custom metadata.
     *
     * @param fieldName the name of the custom metadata field
     * @param value the value to store the worker item
     * @param type One of "text", "integer" (which handles long), "float" (which handles double), "date-time", "text", "boolean" (which handles many 'truthy' types), or "binary"
     * @param mode One of "user" or "api".  Most should be "user" so it is displayed in application, while "api" is available through code but not on the UI.
     * @param params A map to help parse values.  For example, must contain "mimeType" to parse "binary" type data.  Other keys depend on the type being passed in.
     * @return null - it is assumed this metadata was never added to the WorkerItem previously.
     */
    @Override
    public Object put(String fieldName, Object value, String type, String mode, Map<?, ?> params) {
        workerItem.addCustomMetadata(fieldName, value, type, mode, params);
        return null;
    }

    /**
     * Not implemented - just returns an empty string.
     * @param fieldName
     * @return an empty string
     */
    @Override
    public String getType(String fieldName) {
        return "";
    }

    /**
     * Not implemented - just returns an empty string.
     * @param fieldName
     * @return an empty string
     */
    @Override
    public String getMode(String fieldName) {
        return "";
    }

    /**
     * Not implemented - just returns 0
     * @return 0
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * Not implemented - just returns true to be consistent with size()
     * @return true
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * Not implemented - just returns false
     * @param key key whose presence in this map is to be tested
     * @return false
     */
    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    /**
     * Not implemented - just returns false
     * @param value value whose presence in this map is to be tested
     * @return false
     */
    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    /**
     * Not implemented, just returns null
     * @param key the key whose associated value is to be returned
     * @return null
     */
    @Override
    public Object get(Object key) {
        return null;
    }

    /**
     * Stores the value as text in the provided field, with mode "user"
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return null - assume the value has not been set before.
     */
    @Override
    public Object put(String key, Object value) {
        return put(key, value, "text", "user", Map.of());
    }

    /**
     * Not implemented.  Nothing will be removed.
     * @param key key whose mapping is to be removed from the map
     * @return null
     */
    @Override
    public Object remove(Object key) {
        return null;
    }

    /**
     * Put all the values of the provided map into the worker using this class' {@link #put(String, Object)} method.
     * @param toCopy mappings to be stored in this map
     */
    @Override
    public void putAll(Map<? extends String, ?> toCopy) {
        for(Map.Entry<? extends String, ?> entry : toCopy.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Not implemented.  Does nothing.
     */
    @Override
    public void clear() {

    }

    /**
     * Not implemented, returns an empty Set.
     * @return an empty Set
     */
    @Override
    public Set<String> keySet() {
        return new HashSet<>();
    }

    /**
     * Not implemented, returns an empty collection
     * @return an empty collection
     */
    @Override
    public Collection<Object> values() {
        return new HashSet<>();
    }

    /**
     * Not implemented, returns an empty Set.
     * @return an empty Set
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new HashSet<>();
    }
}
