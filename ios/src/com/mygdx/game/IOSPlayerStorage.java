package com.mygdx.game;

import org.robovm.apple.foundation.NSUserDefaults;

/**
 * iOS implementation of PlayerStorage using NSUserDefaults.
 * Preferences survive app restarts and are backed up via iCloud if the user
 * has iCloud Backup enabled.
 */
public class IOSPlayerStorage implements PlayerStorage {

    private static final String KEY_TOKEN         = "token";
    private static final String KEY_NAME          = "name";
    private static final String KEY_SESSION_ID    = "session_id";
    private static final String KEY_SHOW_PLAYERS  = "show_players_tab";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";

    private final NSUserDefaults prefs;

    public IOSPlayerStorage() {
        prefs = NSUserDefaults.getStandardUserDefaults();
    }

    @Override
    public String getToken() {
        String token = prefs.getString(KEY_TOKEN);
        if (token == null || token.isEmpty()) {
            token = java.util.UUID.randomUUID().toString();
            prefs.put(KEY_TOKEN, token);
            prefs.synchronize();
        }
        return token;
    }

    @Override public String  getSavedName()                      { String v = prefs.getString(KEY_NAME);       return v != null ? v : ""; }
    @Override public void    saveName(String name)               { prefs.put(KEY_NAME, name);                  prefs.synchronize(); }
    @Override public void    clearName()                         { prefs.remove(KEY_NAME);                     prefs.synchronize(); }
    @Override public String  getSavedSessionId()                 { String v = prefs.getString(KEY_SESSION_ID); return v != null ? v : ""; }
    @Override public void    saveSessionId(String id)            { prefs.put(KEY_SESSION_ID, id);              prefs.synchronize(); }
    @Override public void    clearSessionId()                    { prefs.remove(KEY_SESSION_ID);               prefs.synchronize(); }
    @Override public boolean getSavedShowPlayersTab()            { return prefs.getBoolean(KEY_SHOW_PLAYERS); }
    @Override public void    saveShowPlayersTab(boolean val)     { prefs.put(KEY_SHOW_PLAYERS, val);           prefs.synchronize(); }
    @Override public boolean getMusicEnabled()                   { String v = prefs.getString(KEY_MUSIC_ENABLED); if (v == null) return true; return prefs.getBoolean(KEY_MUSIC_ENABLED); }
    @Override public void    saveMusicEnabled(boolean enabled)   { prefs.put(KEY_MUSIC_ENABLED, enabled);     prefs.synchronize(); }
}
