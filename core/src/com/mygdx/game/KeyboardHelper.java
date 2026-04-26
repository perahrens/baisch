package com.mygdx.game;

import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * Platform-agnostic interface for triggering the on-screen keyboard when a
 * LibGDX TextField is tapped on mobile.  The default no-op works for desktop.
 * The GWT/HTML implementation uses a hidden native &lt;input&gt; element to
 * persuade the mobile browser to show its keyboard and mirrors typed text back
 * into the LibGDX TextField.
 */
public interface KeyboardHelper {

  /** Show the on-screen keyboard and bind it to the given TextField. */
  void showKeyboard(TextField field);

  /** Hide / release the keyboard binding. */
  void hideKeyboard();

  KeyboardHelper NOOP = new KeyboardHelper() {
    public void showKeyboard(TextField field) {}
    public void hideKeyboard() {}
  };
}
