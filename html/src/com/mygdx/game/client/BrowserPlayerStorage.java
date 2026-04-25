package com.mygdx.game.client;

import com.mygdx.game.PlayerStorage;

/**
 * Browser implementation of PlayerStorage using the Web Storage API (localStorage)
 * via JSNI so it works inside the GWT-compiled runtime.
 *
 * Keys used in localStorage:
 *   baisch_guest_token  — stable UUID v4 that survives refresh / tab-close
 *   baisch_player_name  — last-entered display name
 */
public class BrowserPlayerStorage implements PlayerStorage {

  @Override
  public native String getToken() /*-{
    var key = 'baisch_guest_token';
    var token = $wnd.localStorage.getItem(key);
    if (!token) {
      // Generate a UUID v4 without relying on crypto.randomUUID (wider browser support)
      token = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0;
        var v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
      $wnd.localStorage.setItem(key, token);
    }
    return token;
  }-*/;

  @Override
  public native String getSavedName() /*-{
    return $wnd.localStorage.getItem('baisch_player_name') || '';
  }-*/;

  @Override
  public native void saveName(String name) /*-{
    if (name) $wnd.localStorage.setItem('baisch_player_name', name);
  }-*/;

  @Override
  public native String getSavedSessionId() /*-{
    return $wnd.localStorage.getItem('baisch_session_id') || '';
  }-*/;

  @Override
  public native void saveSessionId(String id) /*-{
    if (id) $wnd.localStorage.setItem('baisch_session_id', id);
  }-*/;

  @Override
  public native void clearSessionId() /*-{
    $wnd.localStorage.removeItem('baisch_session_id');
  }-*/;

  @Override
  public native boolean getSavedShowPlayersTab() /*-{
    return $wnd.localStorage.getItem('baisch_players_tab') === '1';
  }-*/;

  @Override
  public native void saveShowPlayersTab(boolean val) /*-{
    $wnd.localStorage.setItem('baisch_players_tab', val ? '1' : '0');
  }-*/;

  @Override
  public native boolean getMusicEnabled() /*-{
    var val = $wnd.localStorage.getItem('baisch_music_enabled');
    return val === null || val === '1';
  }-*/;

  @Override
  public native void saveMusicEnabled(boolean enabled) /*-{
    $wnd.localStorage.setItem('baisch_music_enabled', enabled ? '1' : '0');
  }-*/;

  @Override
  public native void clearName() /*-{
    $wnd.localStorage.removeItem('baisch_player_name');
  }-*/;

  @Override
  public native String getSavedIcon() /*-{
    return $wnd.localStorage.getItem('baisch_player_icon') || '';
  }-*/;

  @Override
  public native void saveIcon(String icon) /*-{
    if (icon) $wnd.localStorage.setItem('baisch_player_icon', icon);
  }-*/;

  @Override
  public native void clearIcon() /*-{
    $wnd.localStorage.removeItem('baisch_player_icon');
  }-*/;
}
