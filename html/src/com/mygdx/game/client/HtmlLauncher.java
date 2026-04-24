package com.mygdx.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;
import com.mygdx.game.MyGdxGame;

public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        // Use the actual CSS viewport dimensions so the canvas fills the screen
        // on all devices. The Java FitViewport / letterbox in GameScreen handles
        // aspect-ratio centering with black bars.
        return new GwtApplicationConfiguration(
                Window.getClientWidth(),
                Window.getClientHeight());
    }

    @Override
    public ApplicationListener createApplicationListener() {
        WebSocketClient socketClient = new WebSocketClient(getServerUrl());
        MyGdxGame.socketInstance = socketClient;
        MyGdxGame.turnNotifier = new BrowserTurnNotifier();
        MyGdxGame.playerStorage = new BrowserPlayerStorage();
        MyGdxGame app = new MyGdxGame();
        // Tell the core that the HTML layer owns the music button visual.
        MyGdxGame.nativeMusicButton = true;
        // Inject the animated GIF music button into the DOM and wire up callbacks.
        installMusicButton(app);
        // Register the UI-update callback so setMusicEnabled() keeps the GIF in sync.
        MyGdxGame.onMusicUiUpdate = new Runnable() {
            @Override
            public void run() {
                refreshMusicButton(MyGdxGame.playerStorage.getMusicEnabled());
            }
        };
        MyGdxGame.onGameScreenActive = new Runnable() {
            @Override
            public void run() {
                setMusicButtonVisible(false);
            }
        };
        MyGdxGame.onMenuScreenActive = new Runnable() {
            @Override
            public void run() {
                setMusicButtonVisible(true);
            }
        };
        installAudioUnlocker(app);
        return app;
    }

    /**
     * Installs a one-shot DOM listener that on the first valid user activation event
     * unlocks audio and starts the music track.
     *
     * IMPORTANT: We listen for 'touchend' and 'click', NOT 'touchstart'.
     * Android Chrome only grants audio playback permission on activation events
     * like touchend, click, or pointerup — NOT on touchstart.
     * (touchstart fires too early; the browser hasn't committed to treating
     * the gesture as user activation yet.)
     */
    private static native void installAudioUnlocker(MyGdxGame app) /*-{
        var handler = function(evt) {
            $doc.removeEventListener('touchend', handler, true);
            $doc.removeEventListener('click',    handler, true);

            // Unlock Web Audio API (iOS Safari)
            var AudioCtx = $wnd.AudioContext || $wnd.webkitAudioContext;
            if (AudioCtx) {
                try { var ctx = new AudioCtx(); ctx.resume().then(function() { ctx.close(); }); } catch(e) {}
            }

            // Start the actual music track — now from a valid activation event
            app.@com.mygdx.game.MyGdxGame::resumeMusicIfEnabled()();
        };
        $doc.addEventListener('touchend', handler, true);
        $doc.addEventListener('click',    handler, true);
    }-*/;

    /**
     * In local dev the GWT frontend is served on port 8080 while the Node server
     * runs on 8082, so we special-case localhost.  On any other host (production)
     * the Node server serves both the static files and socket.io on the same origin.
     */
    private static native String getServerUrl() /*-{
        var hostname = $wnd.location.hostname;
        if (hostname === 'localhost' || hostname === '127.0.0.1') {
            return $wnd.location.protocol + '//' + hostname + ':8082';
        }
        return $wnd.location.protocol + '//' + $wnd.location.host;
    }-*/;

    /**
     * Injects a fixed-position animated GIF music toggle button into the DOM.
     *
     * A single <img> element is always visible. When music is ON the GIF plays
     * at full colour; when OFF it is shown greyed-out (grayscale + reduced opacity)
     * so the button is always findable regardless of localStorage state or load order.
     *
     * Clicks call MyGdxGame.handleMusicButtonClick() via JSNI so that the LibGDX
     * music state and the current menu screen re-render stay in sync.
     */
    private static native void installMusicButton(MyGdxGame app) /*-{
        var SIZE = '54px';
        var img = $doc.createElement('img');
        img.id  = 'baisch-music-img';
        img.src = '/music.gif';
        img.style.cssText =
            'position:fixed;top:6px;right:6px;width:' + SIZE + ';height:' + SIZE + ';' +
            'cursor:pointer;z-index:9999;border-radius:50%;display:block;';

        $doc.body.appendChild(img);

        // Public setter used by the Java onMusicUiUpdate callback.
        $wnd._baischSetMusicBtn = function(on) {
            if (on) {
                img.style.opacity = '1';
                img.style.filter  = 'none';
            } else {
                img.style.opacity = '0.4';
                img.style.filter  = 'grayscale(100%)';
            }
        };

        // Initialise visual state from localStorage.
        var enabled = ($wnd.localStorage.getItem('baisch_music_enabled') !== '0');
        $wnd._baischSetMusicBtn(enabled);

        // Hover glow — desktop pointer devices only.
        if ($wnd.matchMedia('(hover: hover) and (pointer: fine)').matches) {
            img.style.transition = 'transform 0.15s ease, box-shadow 0.15s ease';
            img.addEventListener('mouseenter', function() {
                img.style.transform = 'scale(1.12)';
                img.style.boxShadow = '0 0 14px rgba(245,200,66,0.75)';
            });
            img.addEventListener('mouseleave', function() {
                img.style.transform = '';
                img.style.boxShadow = '';
            });
        }

        // Click: delegate to Java so LibGDX music state and screen refresh stay in sync.
        img.addEventListener('click', function() {
            @com.mygdx.game.MyGdxGame::handleMusicButtonClick()();
        });
    }-*/;

    /** Called from the onMusicUiUpdate Runnable to keep the GIF in sync with Java state. */
    private static native void refreshMusicButton(boolean enabled) /*-{
        if ($wnd._baischSetMusicBtn) $wnd._baischSetMusicBtn(enabled);
    }-*/;

    /** Shows or hides the DOM music button when switching between game and menu screens. */
    private static native void setMusicButtonVisible(boolean visible) /*-{
        var img = $doc.getElementById('baisch-music-img');
        if (img) img.style.display = visible ? 'block' : 'none';
    }-*/;
}
