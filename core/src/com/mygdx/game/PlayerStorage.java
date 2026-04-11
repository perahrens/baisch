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

  /** No-op implementation used on desktop and as a safe default. */
  PlayerStorage NOOP = new PlayerStorage() {
    @Override public String getToken()            { return ""; }
    @Override public String getSavedName()        { return ""; }
    @Override public void   saveName(String name) { }
  };
}
