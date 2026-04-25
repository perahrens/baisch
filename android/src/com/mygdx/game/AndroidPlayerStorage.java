package com.mygdx.game;

import android.content.Context;
import android.content.SharedPreferences;

public class AndroidPlayerStorage implements PlayerStorage {

    private static final String PREFS  = "baisch_prefs";
    private static final String KEY_TOKEN         = "token";
    private static final String KEY_NAME          = "name";
    private static final String KEY_SESSION_ID    = "session_id";
    private static final String KEY_SHOW_PLAYERS  = "show_players_tab";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_ICON          = "player_icon";

    private final SharedPreferences prefs;

    public AndroidPlayerStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public String getToken() {
        String token = prefs.getString(KEY_TOKEN, "");
        if (token.isEmpty()) {
            token = java.util.UUID.randomUUID().toString();
            prefs.edit().putString(KEY_TOKEN, token).apply();
        }
        return token;
    }

    @Override public String  getSavedName()                      { return prefs.getString(KEY_NAME, ""); }
    @Override public void    saveName(String name)               { prefs.edit().putString(KEY_NAME, name).apply(); }
    @Override public void    clearName()                         { prefs.edit().remove(KEY_NAME).apply(); }
    @Override public String  getSavedSessionId()                 { return prefs.getString(KEY_SESSION_ID, ""); }
    @Override public void    saveSessionId(String id)            { prefs.edit().putString(KEY_SESSION_ID, id).apply(); }
    @Override public void    clearSessionId()                    { prefs.edit().remove(KEY_SESSION_ID).apply(); }
    @Override public boolean getSavedShowPlayersTab()            { return prefs.getBoolean(KEY_SHOW_PLAYERS, false); }
    @Override public void    saveShowPlayersTab(boolean val)     { prefs.edit().putBoolean(KEY_SHOW_PLAYERS, val).apply(); }
    @Override public boolean getMusicEnabled()                   { return prefs.getBoolean(KEY_MUSIC_ENABLED, true); }
    @Override public void    saveMusicEnabled(boolean enabled)   { prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply(); }
    @Override public String  getSavedIcon()                      { return prefs.getString(KEY_ICON, ""); }
    @Override public void    saveIcon(String icon)               { prefs.edit().putString(KEY_ICON, icon).apply(); }
    @Override public void    clearIcon()                         { prefs.edit().remove(KEY_ICON).apply(); }
}
