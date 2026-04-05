package com.mygdx.game.util;

import java.util.ArrayList;

/**
 * GWT-compatible replacement for JSONArray.
 * Backed by ArrayList&lt;Object&gt;; all values must be String, Integer, Boolean,
 * Long, Double, JSONObject, JSONArray, or null.
 */
public class JSONArray {

  private final ArrayList<Object> list = new ArrayList<Object>();

  public JSONArray() {}

  // --- Mutators ---

  public JSONArray put(Object value) {
    list.add(value);
    return this;
  }

  // --- Accessors ---

  public int length() {
    return list.size();
  }

  public Object get(int index) throws JSONException {
    if (index < 0 || index >= list.size()) throw new JSONException("Index out of bounds: " + index);
    return list.get(index);
  }

  public int getInt(int index) throws JSONException {
    Object v = get(index);
    if (v instanceof Number) return ((Number) v).intValue();
    try { return Integer.parseInt(v.toString()); }
    catch (NumberFormatException e) { throw new JSONException("Not a number at index " + index + ": " + v); }
  }

  public String getString(int index) throws JSONException {
    Object v = get(index);
    if (v == null) throw new JSONException("Value is null at index " + index);
    return v.toString();
  }

  public boolean getBoolean(int index) throws JSONException {
    Object v = get(index);
    if (v instanceof Boolean) return (Boolean) v;
    throw new JSONException("Not a boolean at index " + index + ": " + v);
  }

  public JSONObject getJSONObject(int index) throws JSONException {
    Object v = get(index);
    if (v instanceof JSONObject) return (JSONObject) v;
    throw new JSONException("Not a JSONObject at index " + index + ": " + v);
  }

  public JSONArray getJSONArray(int index) throws JSONException {
    Object v = get(index);
    if (v instanceof JSONArray) return (JSONArray) v;
    throw new JSONException("Not a JSONArray at index " + index + ": " + v);
  }

  public JSONArray optJSONArray(int index) {
    try { return getJSONArray(index); } catch (JSONException e) { return null; }
  }

  // --- Serialization ---

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      if (i > 0) sb.append(",");
      sb.append(JSONObject.valueToString(list.get(i)));
    }
    return sb.append("]").toString();
  }

  // --- Parsing (called by JSONObject parser) ---

  static JSONArray fromTokenizer(JsonTokenizer t) throws JSONException {
    JSONArray arr = new JSONArray();
    t.consume('[');
    t.skipWhitespace();
    if (t.peek() == ']') { t.consume(']'); return arr; }
    while (true) {
      arr.put(readValue(t));
      t.skipWhitespace();
      char ch = t.peek();
      if (ch == ']') { t.consume(']'); break; }
      if (ch == ',') { t.consume(','); t.skipWhitespace(); }
      else throw new JSONException("Expected ']' or ',' in array, got: " + ch);
    }
    return arr;
  }

  public static JSONArray parse(String json) throws JSONException {
    return fromTokenizer(new JsonTokenizer(json.trim()));
  }

  private static Object readValue(JsonTokenizer t) throws JSONException {
    return JSONObject.readValue(t);
  }
}
