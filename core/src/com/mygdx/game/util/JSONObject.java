package com.mygdx.game.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GWT-compatible replacement for JSONObject.
 * Backed by LinkedHashMap; values must be String, Integer, Long, Double, Boolean,
 * JSONObject, JSONArray, or null.
 */
public class JSONObject {

  private final LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

  public JSONObject() {}

  // --- Mutators ---

  public JSONObject put(String key, Object value) throws JSONException {
    map.put(key, value);
    return this;
  }

  public JSONObject put(String key, int value) throws JSONException {
    map.put(key, value);
    return this;
  }

  public JSONObject put(String key, long value) throws JSONException {
    map.put(key, value);
    return this;
  }

  public JSONObject put(String key, boolean value) throws JSONException {
    map.put(key, value);
    return this;
  }

  public JSONObject put(String key, double value) throws JSONException {
    map.put(key, value);
    return this;
  }

  // --- Accessors ---

  public boolean has(String key) {
    return map.containsKey(key);
  }

  public int length() {
    return map.size();
  }

  public Object get(String key) throws JSONException {
    if (!map.containsKey(key)) throw new JSONException("No value for key: " + key);
    return map.get(key);
  }

  public String getString(String key) throws JSONException {
    Object v = get(key);
    if (v == null) throw new JSONException("Value is null for key: " + key);
    return v.toString();
  }

  public int getInt(String key) throws JSONException {
    Object v = get(key);
    if (v instanceof Number) return ((Number) v).intValue();
    try { return Integer.parseInt(v.toString()); }
    catch (NumberFormatException e) { throw new JSONException("Not a number for key '" + key + "': " + v); }
  }

  public long getLong(String key) throws JSONException {
    Object v = get(key);
    if (v instanceof Number) return ((Number) v).longValue();
    try { return Long.parseLong(v.toString()); }
    catch (NumberFormatException e) { throw new JSONException("Not a long for key '" + key + "': " + v); }
  }

  public boolean getBoolean(String key) throws JSONException {
    Object v = get(key);
    if (v instanceof Boolean) return (Boolean) v;
    if ("true".equals(v)) return true;
    if ("false".equals(v)) return false;
    throw new JSONException("Not a boolean for key '" + key + "': " + v);
  }

  public JSONObject getJSONObject(String key) throws JSONException {
    Object v = get(key);
    if (v instanceof JSONObject) return (JSONObject) v;
    throw new JSONException("Not a JSONObject for key '" + key + "': " + v);
  }

  public JSONArray getJSONArray(String key) throws JSONException {
    Object v = get(key);
    if (v instanceof JSONArray) return (JSONArray) v;
    throw new JSONException("Not a JSONArray for key '" + key + "': " + v);
  }

  public int optInt(String key, int defaultValue) {
    try { return getInt(key); } catch (Exception e) { return defaultValue; }
  }

  public long optLong(String key, long defaultValue) {
    try { return getLong(key); } catch (Exception e) { return defaultValue; }
  }

  public boolean optBoolean(String key, boolean defaultValue) {
    try { return getBoolean(key); } catch (Exception e) { return defaultValue; }
  }

  public String optString(String key, String defaultValue) {
    if (!map.containsKey(key) || map.get(key) == null) return defaultValue;
    return map.get(key).toString();
  }

  public JSONObject optJSONObject(String key) {
    try { return getJSONObject(key); } catch (Exception e) { return null; }
  }

  public JSONArray optJSONArray(String key) {
    try { return getJSONArray(key); } catch (Exception e) { return null; }
  }

  public Iterator<String> keys() {
    return map.keySet().iterator();
  }

  // --- Serialization ---

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!first) sb.append(",");
      sb.append("\"").append(escapeString(entry.getKey())).append("\":");
      sb.append(valueToString(entry.getValue()));
      first = false;
    }
    return sb.append("}").toString();
  }

  // Package-accessible for JSONArray
  static String valueToString(Object v) {
    if (v == null) return "null";
    if (v instanceof Boolean) return v.toString();
    if (v instanceof Number) return v.toString();
    if (v instanceof JSONObject) return v.toString();
    if (v instanceof JSONArray) return v.toString();
    // String
    return "\"" + escapeString(v.toString()) + "\"";
  }

  private static String escapeString(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"') sb.append("\\\"");
      else if (c == '\\') sb.append("\\\\");
      else if (c == '\n') sb.append("\\n");
      else if (c == '\r') sb.append("\\r");
      else if (c == '\t') sb.append("\\t");
      else sb.append(c);
    }
    return sb.toString();
  }

  // --- Parsing ---

  /**
   * Parse a JSON string into a JSONObject.
   * Called by SocketClient implementations when incoming data arrives as a plain string.
   */
  public static JSONObject parse(String json) throws JSONException {
    if (json == null || json.trim().length() == 0) throw new JSONException("Empty JSON");
    JsonTokenizer t = new JsonTokenizer(json.trim());
    Object v = readValue(t);
    if (!(v instanceof JSONObject)) throw new JSONException("Not a JSON object: " + json);
    return (JSONObject) v;
  }

  // Package-accessible for JSONArray
  static Object readValue(JsonTokenizer t) throws JSONException {
    t.skipWhitespace();
    char ch = t.peek();
    if (ch == '{') return readObject(t);
    if (ch == '[') return JSONArray.fromTokenizer(t);
    if (ch == '"') return readString(t);
    if (ch == 't') { t.consume("true"); return Boolean.TRUE; }
    if (ch == 'f') { t.consume("false"); return Boolean.FALSE; }
    if (ch == 'n') { t.consume("null"); return null; }
    if (ch == '-' || (ch >= '0' && ch <= '9')) return readNumber(t);
    throw new JSONException("Unexpected character: " + ch);
  }

  private static JSONObject readObject(JsonTokenizer t) throws JSONException {
    JSONObject obj = new JSONObject();
    t.consume('{');
    t.skipWhitespace();
    if (t.peek() == '}') { t.consume('}'); return obj; }
    while (true) {
      String key = readString(t);
      t.skipWhitespace();
      t.consume(':');
      t.skipWhitespace();
      Object value = readValue(t);
      obj.put(key, value);
      t.skipWhitespace();
      char next = t.peek();
      if (next == '}') { t.consume('}'); break; }
      if (next == ',') { t.consume(','); t.skipWhitespace(); }
      else throw new JSONException("Expected '}' or ',' in object, got: " + next);
    }
    return obj;
  }

  private static String readString(JsonTokenizer t) throws JSONException {
    t.consume('"');
    StringBuilder sb = new StringBuilder();
    while (true) {
      char c = t.next();
      if (c == '"') break;
      if (c == '\\') {
        char esc = t.next();
        if (esc == '"') sb.append('"');
        else if (esc == '\\') sb.append('\\');
        else if (esc == '/') sb.append('/');
        else if (esc == 'n') sb.append('\n');
        else if (esc == 'r') sb.append('\r');
        else if (esc == 't') sb.append('\t');
        else if (esc == 'b') sb.append('\b');
        else if (esc == 'f') sb.append('\f');
        else if (esc == 'u') {
          char u1 = t.next(); char u2 = t.next(); char u3 = t.next(); char u4 = t.next();
          sb.append((char) Integer.parseInt("" + u1 + u2 + u3 + u4, 16));
        } else sb.append(esc);
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static Number readNumber(JsonTokenizer t) throws JSONException {
    StringBuilder sb = new StringBuilder();
    boolean isFloat = false;
    if (t.peek() == '-') sb.append(t.next());
    while (t.hasMore() && (t.peek() >= '0' && t.peek() <= '9')) sb.append(t.next());
    if (t.hasMore() && t.peek() == '.') { isFloat = true; sb.append(t.next()); while (t.hasMore() && (t.peek() >= '0' && t.peek() <= '9')) sb.append(t.next()); }
    if (t.hasMore() && (t.peek() == 'e' || t.peek() == 'E')) { isFloat = true; sb.append(t.next()); if (t.hasMore() && (t.peek() == '+' || t.peek() == '-')) sb.append(t.next()); while (t.hasMore() && (t.peek() >= '0' && t.peek() <= '9')) sb.append(t.next()); }
    String s = sb.toString();
    try {
      if (isFloat) return Double.parseDouble(s);
      long l = Long.parseLong(s);
      if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
      return l;
    } catch (NumberFormatException e) { throw new JSONException("Invalid number: " + s); }
  }
}
