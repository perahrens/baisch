package com.mygdx.game;

/**
 * Platform-agnostic interface for persisting the player's identity across browser
 * refresh / tab close.
 *
 * The GWT/browser implementation (BrowserPlayerStorage in the html module) stores
 * a UUID guest-token and the player's display name in localStorage so that
 * returning visitors are recognised without re-entering their name.
 *
 * The desktop no-op implementation (NOOP) returns empty strings and discards saves.
 */
public interface PlayerStorage {

  /**
   * Returns (or lazily generates) a stable UUID that uniquely identifies this
   * browser installation.  The token is persisted in localStorage so it survives
   * page refreshes and tab closes.
   */
  String getToken();

  /** Returns the last-saved player name, or an empty string if none is stored. */
  String getSavedName();

  /** Persists the player's display name so it survives a page refresh. */
  void saveName(String name);

  /**
   * Returns the session ID the player was last in (lobby or running game), or an empty string.
   * Used to auto-rejoin the right screen on page refresh.
   */
  String getSavedSessionId();

  /** Persists the session ID the player just joined. */
  void saveSessionId(String id);

  /** Clears the saved session ID (e.g. when leaving a session or returning to the session list). */
  void clearSessionId();

  /** Returns true if the "Players" tab was active on the session-list screen before the last refresh. */
  boolean getSavedShowPlayersTab();

  /** Persists the current tab selection on the session-list screen. */
  void saveShowPlayersTab(boolean playersTabActive);

  /** Returns true if background music should play (default: true). */
  boolean getMusicEnabled();

  /** Persists the music on/off preference. */
  void saveMusicEnabled(boolean enabled);

  /** Returns true if in-game sound effects should play (default: true). */
  boolean getSoundEnabled();

  /** Persists the sound effects on/off preference. */
  void saveSoundEnabled(boolean enabled);

  /** Returns the currently selected UI language code (default: en). */
  String getLanguage();

  /** Persists the selected UI language code (supported: en, de). */
  void saveLanguage(String languageCode);

  /** Clears the saved player name (logout). */
  void clearName();

  /** Returns the last-saved avatar icon name, or an empty string if none is stored. */
  String getSavedIcon();

  /** Persists the player's chosen avatar icon name. */
  void saveIcon(String icon);

  /** Clears the saved avatar icon (logout). */
  void clearIcon();

  /**
   * Returns the last-saved account username, or an empty string if the player is a guest
   * or has never logged in with a registered account.
   */
  String getSavedUsername();

  /** Persists the registered account username (set after a successful register or login). */
  void saveUsername(String username);

  /** Clears the saved account username (logout from registered account). */
  void clearUsername();

  /** No-op implementation used on desktop and as a safe default. */
  PlayerStorage NOOP = new PlayerStorage() {
    @Override public String  getToken()                          { return ""; }
    @Override public String  getSavedName()                      { return ""; }
    @Override public void    saveName(String name)               { }
    @Override public String  getSavedSessionId()                 { return ""; }
    @Override public void    saveSessionId(String id)            { }
    @Override public void    clearSessionId()                    { }
    @Override public boolean getSavedShowPlayersTab()            { return false; }
    @Override public void    saveShowPlayersTab(boolean val)     { }
    @Override public boolean getMusicEnabled()                   { return true; }
    @Override public void    saveMusicEnabled(boolean enabled)   { }
    @Override public boolean getSoundEnabled()                   { return true; }
    @Override public void    saveSoundEnabled(boolean enabled)   { }
    @Override public String  getLanguage()                       { return "en"; }
    @Override public void    saveLanguage(String languageCode)   { }
    @Override public void    clearName()                         { }
    @Override public String  getSavedIcon()                      { return ""; }
    @Override public void    saveIcon(String icon)               { }
    @Override public void    clearIcon()                         { }
    @Override public String  getSavedUsername()                  { return ""; }
    @Override public void    saveUsername(String username)       { }
    @Override public void    clearUsername()                     { }
  };
}
