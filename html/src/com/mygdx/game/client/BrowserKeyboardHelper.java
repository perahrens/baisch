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
 * Additionally, a visualViewport resize listener translates the game container
 * upward when the keyboard appears so the text field remains visible.
 *
 * IMPORTANT: focus() must be called synchronously inside a user-gesture
 * handler (touchstart / touchend).  LibGDX Stage dispatches touchDown from
 * the JS touchstart event chain, so this works on both iOS and Android Chrome.
 */
public class BrowserKeyboardHelper implements KeyboardHelper {

    public BrowserKeyboardHelper() {
        installHiddenInput();
        installViewportListener();
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

    /**
     * Listens to visualViewport resize events so the game container scrolls
     * upward when the keyboard appears, keeping the active text field visible.
     */
    private native void installViewportListener() /*-{
        if (!$wnd.visualViewport || $wnd._gdxViewportInstalled) return;
        $wnd._gdxViewportInstalled = true;
        function onViewportChange() {
            var kbHeight = $wnd.innerHeight
                         - $wnd.visualViewport.height
                         - $wnd.visualViewport.offsetTop;
            if (kbHeight < 0) kbHeight = 0;
            var container = $doc.getElementById('embed-html');
            if (!container) return;
            if (kbHeight > 10) {
                container.style.transform = 'translateY(-' + kbHeight + 'px)';
            } else {
                container.style.transform = '';
            }
        }
        $wnd.visualViewport.addEventListener('resize', onViewportChange);
        $wnd.visualViewport.addEventListener('scroll', onViewportChange);
    }-*/;

    @Override
    public void showKeyboard(final TextField field) {
        focusAndBind(field, null);
    }

    @Override
    public void showKeyboard(final TextField field, final Runnable onEnter) {
        focusAndBind(field, onEnter);
    }

    @Override
    public void hideKeyboard() {
        blurHiddenInput();
    }

    /**
     * Focuses the hidden input and wires an 'input' listener that mirrors
     * text back to the LibGDX TextField on every keystroke.
     * When Enter/Done is pressed, {@code onEnter} is invoked (if non-null).
     */
    private native void focusAndBind(TextField field, Runnable onEnter) /*-{
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

        // Enter / Done key: call onEnter callback then blur (closes keyboard).
        if (inp._gdxKeyHandler) {
            inp.removeEventListener('keydown', inp._gdxKeyHandler);
        }
        inp._gdxKeyHandler = function(e) {
            if (e.key === 'Enter' || e.keyCode === 13) {
                if (onEnter) {
                    onEnter.@java.lang.Runnable::run()();
                }
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
