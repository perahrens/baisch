package com.mygdx.game.client;

import com.mygdx.game.TurnNotifier;

/**
 * Browser implementation of TurnNotifier.
 *
 * Priority:
 *   1. Web Notifications API (requires user permission).
 *   2. document.title flash as universal fallback (works on all browsers incl. iOS Safari).
 *
 * Permission is requested lazily on the first turn transition so it happens
 * inside a user-gesture context where possible, and is not re-requested if
 * already granted or denied.
 */
public class BrowserTurnNotifier implements TurnNotifier {

  @Override
  public void notifyYourTurn(String playerName) {
    nativeNotify(playerName);
  }

  private native void nativeNotify(String playerName) /*-{
    var title = "Your turn, " + playerName + "!";
    var body  = "It's your turn in Baisch!";

    // ---- Title flash (always set up, works even when Notifications are denied) ----
    var originalTitle = $doc.title || "Baisch";
    var flashInterval = null;
    var flashCount = 0;

    function startFlash() {
      if (flashInterval) return;
      flashInterval = $wnd.setInterval(function() {
        flashCount++;
        $doc.title = (flashCount % 2 === 0) ? originalTitle : "\uD83D\uDD14 " + title;
        if (flashCount > 20) stopFlash(); // auto-stop after ~10 s
      }, 500);
    }

    function stopFlash() {
      if (flashInterval) {
        $wnd.clearInterval(flashInterval);
        flashInterval = null;
      }
      $doc.title = originalTitle;
    }

    // Stop flashing when the player comes back to the tab
    $doc.addEventListener("visibilitychange", function onVisible() {
      if (!$doc.hidden) {
        stopFlash();
        $doc.removeEventListener("visibilitychange", onVisible);
      }
    });

    $wnd.addEventListener("focus", function onFocus() {
      stopFlash();
      $wnd.removeEventListener("focus", onFocus);
    });

    // Only flash / notify if the tab is not currently focused
    if ($doc.hidden || !$doc.hasFocus()) {
      startFlash();
    }

    // ---- Web Notifications ----
    if (!$wnd.Notification) return; // browser doesn't support it

    if ($wnd.Notification.permission === "granted") {
      var n = new $wnd.Notification(title, { body: body, icon: "/favicon.ico" });
      $wnd.setTimeout(function() { n.close(); }, 6000);
    } else if ($wnd.Notification.permission !== "denied") {
      $wnd.Notification.requestPermission().then(function(permission) {
        if (permission === "granted") {
          var n = new $wnd.Notification(title, { body: body, icon: "/favicon.ico" });
          $wnd.setTimeout(function() { n.close(); }, 6000);
        }
      });
    }
  }-*/;
}
