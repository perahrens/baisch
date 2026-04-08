package com.mygdx.game.client;

import com.mygdx.game.TurnNotifier;

/**
 * Browser implementation of TurnNotifier.
 *
 * On your turn:
 *   1. Vibration (Android Chrome, no permission needed).
 *   2. Audio beep via Web Audio API (no permission needed).
 *   3. document.title flash (always, stopped via clearNotification() when turn ends).
 *   4. OS notification via ServiceWorker (mobile) or direct Notification (desktop).
 *
 * Call requestPermission() from a button-click so Chrome shows the permission dialog.
 */
public class BrowserTurnNotifier implements TurnNotifier {

  @Override
  public void notifyYourTurn(String playerName) {
    nativeNotify(playerName);
  }

  @Override
  public void clearNotification() {
    nativeClear();
  }

  @Override
  public void requestPermission(Runnable onComplete) {
    nativeRequestPermission(onComplete);
  }

  @Override
  public boolean isPermissionGranted() {
    return nativeIsPermissionGranted();
  }

  private native void nativeRequestPermission(Runnable onComplete) /*-{
    if (!$wnd.Notification || $wnd.Notification.permission !== "default") {
      onComplete.@java.lang.Runnable::run()();
      return;
    }
    $wnd.Notification.requestPermission().then(function() {
      onComplete.@java.lang.Runnable::run()();
    });
  }-*/;

  private native boolean nativeIsPermissionGranted() /*-{
    return !!($wnd.Notification && $wnd.Notification.permission === "granted");
  }-*/;

  /** Stops any active title flash and resets the document title. */
  private native void nativeClear() /*-{
    if ($wnd.__baischNotifFlash) {
      $wnd.clearInterval($wnd.__baischNotifFlash);
      $wnd.__baischNotifFlash = null;
    }
    if ($wnd.__baischNotifOrigTitle) {
      $doc.title = $wnd.__baischNotifOrigTitle;
      $wnd.__baischNotifOrigTitle = null;
    }
  }-*/;

  private native void nativeNotify(String playerName) /*-{
    var title = "Your turn, " + playerName + "!";
    var body  = "It's your turn in Baisch!";

    // ---- 1. Vibration (Android Chrome, no permission needed) ----
    try {
      if ($wnd.navigator.vibrate) $wnd.navigator.vibrate([200, 100, 200]);
    } catch (e) { }

    // ---- 2. Audio beep via Web Audio API (no permission needed) ----
    try {
      var AudioCtx = $wnd.AudioContext || $wnd.webkitAudioContext;
      if (AudioCtx) {
        var ctx = new AudioCtx();
        var osc  = ctx.createOscillator();
        var gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880,  ctx.currentTime);
        osc.frequency.setValueAtTime(1100, ctx.currentTime + 0.12);
        gain.gain.setValueAtTime(0.4, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.7);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.7);
      }
    } catch (e) { }

    // ---- 3. Title flash using global state so clearNotification() can stop it ----
    if ($wnd.__baischNotifFlash) {
      $wnd.clearInterval($wnd.__baischNotifFlash);
      $wnd.__baischNotifFlash = null;
    }
    $wnd.__baischNotifOrigTitle = $doc.title || "Baisch";
    var flashCount = 0;
    $wnd.__baischNotifFlash = $wnd.setInterval(function() {
      flashCount++;
      $doc.title = (flashCount % 2 === 0) ? $wnd.__baischNotifOrigTitle : "\uD83D\uDD14 " + title;
      if (flashCount > 20) {
        $wnd.clearInterval($wnd.__baischNotifFlash);
        $wnd.__baischNotifFlash = null;
        $doc.title = $wnd.__baischNotifOrigTitle;
      }
    }, 500);

    $doc.addEventListener("visibilitychange", function onVisible() {
      if (!$doc.hidden) {
        if ($wnd.__baischNotifFlash) { $wnd.clearInterval($wnd.__baischNotifFlash); $wnd.__baischNotifFlash = null; }
        if ($wnd.__baischNotifOrigTitle) { $doc.title = $wnd.__baischNotifOrigTitle; $wnd.__baischNotifOrigTitle = null; }
        $doc.removeEventListener("visibilitychange", onVisible);
      }
    });
    $wnd.addEventListener("focus", function onFocus() {
      if ($wnd.__baischNotifFlash) { $wnd.clearInterval($wnd.__baischNotifFlash); $wnd.__baischNotifFlash = null; }
      if ($wnd.__baischNotifOrigTitle) { $doc.title = $wnd.__baischNotifOrigTitle; $wnd.__baischNotifOrigTitle = null; }
      $wnd.removeEventListener("focus", onFocus);
    });

    // ---- 4. OS notification ----
    if (!$wnd.Notification || $wnd.Notification.permission !== "granted") return;

    // Use ServiceWorker registration for mobile Chrome (new Notification() throws there).
    // Falls back to direct Notification for desktop browsers without SW support.
    if ($wnd.navigator.serviceWorker) {
      $wnd.navigator.serviceWorker.register('/sw.js').then(function(reg) {
        reg.showNotification(title, { body: body, icon: "/favicon.ico", vibrate: [200, 100, 200] });
      }, function() {
        // SW registration failed — fall back to direct Notification
        try {
          var nb = new $wnd.Notification(title, { body: body, icon: "/favicon.ico" });
          $wnd.setTimeout(function() { nb.close(); }, 6000);
        } catch (e2) { }
      });
    } else {
      try {
        var nd = new $wnd.Notification(title, { body: body, icon: "/favicon.ico" });
        $wnd.setTimeout(function() { nd.close(); }, 6000);
      } catch (e) { }
    }
  }-*/;
}

