package com.nuix.proserv.t3k.ws;

import nuix.CustomMetadataMap;
import nuix.WorkerItem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This wraps a nuix.WorkerItem in a nuix.CustomMetadataMap interface.
 * <p>
 *     Doing so allows the WorkerItem to be treated as a custom metadata target the same way a CustomMetadataMap could
 *     be using post processing scripts and allows sharing code between the two types of scripts.
 * </p>
 * <p>
 *     WorkerItems only act as consumers of custom metadata - you can add metadata to them but you can't retrieve any
 *     from them.  In this wrapper we treat the WorkerItem as essentially an empty map, returning 0 size, empty
 *     iterators, and false/nulls when looking items up.
 * </p>
 */
public class WorkerItemMetadataMapWrapper implements CustomMetadataMap {
    private final WorkerItem workerItem;

    public WorkerItemMetadataMapWrapper(WorkerItem itemToStoreIn) {
        this.workerItem = itemToStoreIn;
    }

    /**
     * Store a text item in the WorkerItem's custom metadata.  The text will be stored as type=Text and mode=User.
     * The value will have its toString() method storing it.
     * @param fieldName The custom metadata field to store the data in
     * @param value The value, whose toString() result will be stored in the worker item
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object putText(String fieldName, Object value) {
        return put(fieldName, value.toString(), "text", "user", Map.of());
    }

    /**
     * Put a Date value into the WorkerItem.  No work is done to ensure the object passed in will correctly convert to
     * a Date representation in Nuix.  See {@link nuix.CustomMetadataMap#putDate(String, Object)} for details on what
     * constitutes a Date.
     * @param fieldName The custom metadata field to store the data
     * @param value The value stord in the worker item as is
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object putDate(String fieldName, Object value) {
        return put(fieldName, value, "date-time", "user", Map.of());
    }

    /**
     * Store a date time in the worker item, providing time zone and string format in the options map.  No effort is
     * made to ensure the object can be converted to a Date.  See
     * {@link nuix.CustomMetadataMap#putDate(String, Object, Map)} for details what constitutes a valid Date.
     * @param fieldName the name of the custom metadata field
     * @param value an instance of DateTime, RubyTime, java.utl.Date, or anything that has a toString() method that can be parsed with the passed in "format" or Joda Time's formats.
     * @param options Map containing:
     *                optional "timeZone" key with a String ID that is parsable by Joda Time's DateTimeZone.forID(String) method.
     *                optional "format" key with a string that can be used to parse the date with String.format().
     *
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object putDate(String fieldName, Object value, Map<?, ?> options) {
        return put(fieldName, value, "date-time", "user", options);
    }

    /**
     * Store an integer in the worker item.  No effort is made to ensure the passed in object can be converted to an
     * integer.  See {@link nuix.CustomMetadataMap#putInteger(String, Object)} for details.
     * @param fieldName the name of the custom metadata field to store the value in
     * @param value The value to store as an integer
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object putInteger(String fieldName, Object value) {
        return put(fieldName, value, "integer", "user", Map.of());
    }

    /**
     * Put a float or double to store in the worker item.  No effort is made to convert the object into a floating
     * point type.  See {@link nuix.CustomMetadataMap#putFloat(String, Object)} for details.
     * @param fieldName The name of the custom metadata field to store the value in
     * @param value The value to store as a float
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object putFloat(String fieldName, Object value) {
        return put(fieldName, value, "float", "user", Map.of());
    }

    /**
     * Put a 'truethy' value into the worker item.  No effort is made to ensure the value can be converted to a truethy
     * value.  See {@link nuix.CustomMetadataMap#putBoolean(String, Object)} for details on what constitutes Truethiness.
     * @param fieldName The name of the custom metadata field to store the value in
     * @param value The truethy value to store
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object putBoolean(String fieldName, Object value) {
        return put(fieldName, value, "boolean", "user", Map.of());
    }

    /**
     * Store the provided value into the wrapped worker item's custom metadata.  The value being stored must match the
     * type passed in as the type parameter.  See the more specific putX(String, Object) methods in
     * {@link nuix.CustomMetadataMap} for details on how each type is handled.
     *
     * @param fieldName the name of the custom metadata field
     * @param value the value to store the worker item
     * @param type One of "text", "integer" (which handles long), "float" (which handles double), "date-time", "text",
     *             "boolean" (which handles many 'truthy' types), or "binary"
     * @param mode One of "user" or "api".  Most should be "user" so it is displayed in application, while "api" is
     *             available through code but not on the UI.
     * @param params A map to help parse values.  For example, must contain "mimeType" to parse "binary" type data.
     *               Other keys depend on the type being passed in.
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object put(String fieldName, Object value, String type, String mode, Map<?, ?> params) {
        workerItem.addCustomMetadata(fieldName, value, type, mode, params);
        return null;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return null.
     * @param fieldName name of the custom metadata field to look up the type
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public String getType(String fieldName) {
        return null;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return null.
     * @param fieldName name of the custom metadata field to look up the mode
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public String getMode(String fieldName) {
        return null;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return 0.
     * @return 0, as we treat the WorkerItem as always empty
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return true.
     * @return true, as we treat the WorkerItem as always empty
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return false.
     * @param key name of the custom metadata field to check for
     * @return false, as we treat the WorkerItem as always empty
     */
    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return false.
     * @param value value whose presence in this map is to be tested
     * @return false, as we treat the WorkerItem as always empty
     */
    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will always return null.
     * @param key name of the custom metadata field to look up
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object get(Object key) {
        return null;
    }

    /**
     * Stores the value as text in the provided field, with mode "user".  See {@link #putText(String, Object)}
     * for details.
     * @param key name of the custom metadata field to store the value with
     * @param value objects which will be stored as a text value in the worker item
     * @return null, as we treat the WorkerItem as always empty
     */
    @Override
    public Object put(String key, Object value) {
        return putText(key, value);
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will be a no-op that always return null.
     * @param key name of the custom metadata field to remove
     * @return null, as we treat the WorkerItem as always empty
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
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will be a no-op.
     */
    @Override
    public void clear() {

    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will return an empty Set.
     * @return an empty Set
     */
    @Override
    public Set<String> keySet() {
        return new HashSet<>();
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will return an empty Collection.
     * @return an empty collection
     */
    @Override
    public Collection<Object> values() {
        return new HashSet<>();
    }

    /**
     * The WorkerItem will always be considered an empty CustomMetadataMap, so this will return an empty Set.
     * @return an empty Set
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new HashSet<>();
    }
}
