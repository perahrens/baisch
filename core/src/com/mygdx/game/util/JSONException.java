package com.mygdx.game.util;

/**
 * GWT-compatible replacement for org.json.JSONException.
 */
public class JSONException extends Exception {
  public JSONException(String message) {
    super(message);
  }
}
