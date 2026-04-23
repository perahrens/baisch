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
     * Two sibling elements are created:
     *   #baisch-music-img    — the animated GIF (shown when music is ON)
     *   #baisch-music-canvas — a canvas showing the GIF's first frame (shown when OFF)
     *
     * The first frame is captured once the image loads.  Subsequent toggles swap
     * visibility between the two elements via window._baischSetMusicBtn(bool).
     *
     * Clicks call MyGdxGame.handleMusicButtonClick() via JSNI so that the LibGDX
     * music state and the current menu screen re-render stay in sync.
     */
    private static native void installMusicButton(MyGdxGame app) /*-{
        var SIZE = '54px';
        var img = $doc.createElement('img');
        img.id  = 'baisch-music-img';
        img.src = 'music.gif';
        img.style.cssText =
            'position:fixed;top:6px;right:6px;width:' + SIZE + ';height:' + SIZE + ';' +
            'cursor:pointer;z-index:9999;border-radius:50%;display:block;';

        var canvas = $doc.createElement('canvas');
        canvas.id  = 'baisch-music-canvas';
        canvas.style.cssText =
            'position:fixed;top:6px;right:6px;width:' + SIZE + ';height:' + SIZE + ';' +
            'cursor:pointer;z-index:9999;border-radius:50%;display:none;';

        $doc.body.appendChild(img);
        $doc.body.appendChild(canvas);

        var firstFrameCaptured = false;
        function captureFirstFrame() {
            if (firstFrameCaptured) return;
            firstFrameCaptured = true;
            canvas.width  = img.naturalWidth  || 100;
            canvas.height = img.naturalHeight || 100;
            canvas.getContext('2d').drawImage(img, 0, 0);
        }
        if (img.complete) { captureFirstFrame(); }
        else { img.addEventListener('load', captureFirstFrame); }

        // Public setter used by the Java onMusicUiUpdate callback.
        $wnd._baischSetMusicBtn = function(on) {
            if (!on) {
                captureFirstFrame();        // ensure first frame is ready
                img.style.display    = 'none';
                canvas.style.display = 'block';
            } else {
                img.style.display    = 'block';
                canvas.style.display = 'none';
            }
        };

        // Initialise visual state from localStorage.
        var enabled = ($wnd.localStorage.getItem('baisch_music_enabled') !== '0');
        $wnd._baischSetMusicBtn(enabled);

        // Click: delegate to Java so LibGDX music state and screen refresh stay in sync.
        function handleClick() {
            @com.mygdx.game.MyGdxGame::handleMusicButtonClick()();
        }
        img.addEventListener('click',    handleClick);
        canvas.addEventListener('click', handleClick);
    }-*/;

    /** Called from the onMusicUiUpdate Runnable to keep the GIF in sync with Java state. */
    private static native void refreshMusicButton(boolean enabled) /*-{
        if ($wnd._baischSetMusicBtn) $wnd._baischSetMusicBtn(enabled);
    }-*/;
}
