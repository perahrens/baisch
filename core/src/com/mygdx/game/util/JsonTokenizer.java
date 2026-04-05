package com.mygdx.game.util;

/**
 * Simple JSON tokenizer used by JSONObject/JSONArray parser.
 * Pure Java, GWT-compatible (no java.io, no regex).
 */
class JsonTokenizer {

  private final String src;
  private int pos;

  JsonTokenizer(String src) {
    this.src = src;
    this.pos = 0;
  }

  boolean hasMore() {
    return pos < src.length();
  }

  char peek() throws JSONException {
    if (pos >= src.length()) throw new JSONException("Unexpected end of JSON");
    return src.charAt(pos);
  }

  char next() throws JSONException {
    if (pos >= src.length()) throw new JSONException("Unexpected end of JSON");
    return src.charAt(pos++);
  }

  void consume(char expected) throws JSONException {
    char c = next();
    if (c != expected) throw new JSONException("Expected '" + expected + "' but got '" + c + "' at pos " + (pos - 1));
  }

  void consume(String s) throws JSONException {
    for (int i = 0; i < s.length(); i++) consume(s.charAt(i));
  }

  void skipWhitespace() {
    while (pos < src.length()) {
      char c = src.charAt(pos);
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
      else break;
    }
  }
}
