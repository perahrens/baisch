package com.mygdx.game.client;

import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.mygdx.game.KeyboardHelper;

/**
 * GWT/HTML implementation of KeyboardHelper.
 *
 * Mobile browsers won't show their soft keyboard for a WebGL canvas element.
 * The workaround is to keep a hidden native &lt;input type="text"&gt; in the DOM,
 * position it off-screen, focus it whenever the player taps the chat field
 * (which persuades the browser to open its keyboard), and mirror every
 * keystroke back into the LibGDX TextField via an 'input' event listener.
 *
 * IMPORTANT: focus() must be called synchronously inside a user-gesture
 * handler (touchstart / touchend).  LibGDX Stage dispatches touchDown from
 * the JS touchstart event chain, so this works on both iOS and Android Chrome.
 */
public class BrowserKeyboardHelper implements KeyboardHelper {

    public BrowserKeyboardHelper() {
        installHiddenInput();
    }

    /** Injects the hidden <input> into the DOM once. */
    private native void installHiddenInput() /*-{
        if ($doc.getElementById('_gdx_kbd')) return;
        var inp = $doc.createElement('input');
        inp.setAttribute('id', '_gdx_kbd');
        inp.setAttribute('type', 'text');
        inp.setAttribute('autocomplete', 'off');
        inp.setAttribute('autocorrect', 'off');
        inp.setAttribute('autocapitalize', 'off');
        inp.setAttribute('spellcheck', 'false');
        inp.style.position = 'fixed';
        inp.style.top      = '-9999px';
        inp.style.left     = '-9999px';
        inp.style.width    = '1px';
        inp.style.height   = '1px';
        inp.style.opacity  = '0';
        inp.style.pointerEvents = 'none';
        $doc.body.appendChild(inp);
    }-*/;

    @Override
    public void showKeyboard(final TextField field) {
        focusAndBind(field);
    }

    @Override
    public void hideKeyboard() {
        blurHiddenInput();
    }

    /**
     * Focuses the hidden input and wires an 'input' listener that mirrors
     * text back to the LibGDX TextField on every keystroke.
     */
    private native void focusAndBind(TextField field) /*-{
        var inp = $doc.getElementById('_gdx_kbd');
        if (!inp) return;

        // Seed with the current TextField text so the cursor lands at the end.
        inp.value = field.@com.badlogic.gdx.scenes.scene2d.ui.TextField::getText()();

        // Re-bind the input listener to the new (possibly different) field.
        if (inp._gdxInputHandler) {
            inp.removeEventListener('input', inp._gdxInputHandler);
        }
        inp._gdxInputHandler = function() {
            field.@com.badlogic.gdx.scenes.scene2d.ui.TextField::setText(Ljava/lang/String;)(inp.value);
            // Move cursor to end so subsequent keystrokes append correctly.
            var len = inp.value.length;
            field.@com.badlogic.gdx.scenes.scene2d.ui.TextField::setCursorPosition(I)(len);
        };
        inp.addEventListener('input', inp._gdxInputHandler);

        // Also sync on Enter / Done key so the text is up-to-date before Send.
        if (inp._gdxKeyHandler) {
            inp.removeEventListener('keydown', inp._gdxKeyHandler);
        }
        inp._gdxKeyHandler = function(e) {
            if (e.key === 'Enter' || e.keyCode === 13) {
                inp.blur();
            }
        };
        inp.addEventListener('keydown', inp._gdxKeyHandler);

        inp.focus();
        var len = inp.value.length;
        try { inp.setSelectionRange(len, len); } catch(e) {}
    }-*/;

    private native void blurHiddenInput() /*-{
        var inp = $doc.getElementById('_gdx_kbd');
        if (inp) inp.blur();
    }-*/;
}
