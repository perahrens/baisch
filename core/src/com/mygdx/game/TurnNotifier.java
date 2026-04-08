package com.mygdx.game;

/**
 * Platform-agnostic interface for notifying the player that it is their turn.
 * The desktop/no-op implementation does nothing. The browser implementation
 * (BrowserTurnNotifier in the html module) fires a Web Notification and/or
 * flashes the document title.
 */
public interface TurnNotifier {

  /** Called when it becomes this client's turn. */
  void notifyYourTurn(String playerName);

  /** No-op implementation used on desktop and as a safe default. */
  TurnNotifier NOOP = new TurnNotifier() {
    @Override
    public void notifyYourTurn(String playerName) {
      // no-op
    }
  };
}
