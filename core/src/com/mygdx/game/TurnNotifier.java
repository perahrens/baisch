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

  /**
   * Request browser notification permission.  Must be called from a user-gesture
   * context. {@code onComplete} is invoked after the dialog is dismissed (or
   * immediately if permission is already decided) so the caller can refresh UI.
   */
  void requestPermission(Runnable onComplete);

  /** Returns true if OS-level notification permission has been granted. */
  boolean isPermissionGranted();

  /** Clears any active title flash / pending notification (e.g. when the player ends their turn). */
  void clearNotification();

  /** No-op implementation used on desktop and as a safe default. */
  TurnNotifier NOOP = new TurnNotifier() {
    @Override
    public void notifyYourTurn(String playerName) { }
    @Override
    public void requestPermission(Runnable onComplete) {
      if (onComplete != null) onComplete.run();
    }
    @Override
    public boolean isPermissionGranted() {
      return false;
    }
    @Override
    public void clearNotification() { }
  };
}
