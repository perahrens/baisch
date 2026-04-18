package com.mygdx.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
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
    public Preloader.PreloaderCallback getPreloaderCallback() {
        // Return a no-op callback so the default libGDX logo/progress bar
        // is never rendered. Our custom HTML loading overlay handles this instead.
        return new Preloader.PreloaderCallback() {
            @Override
            public void update(Preloader.PreloaderState state) {
                // intentionally empty
            }
            @Override
            public void error(String file) {
                // silently ignore individual asset load errors
            }
        };
    }

    @Override
    public ApplicationListener createApplicationListener() {
        WebSocketClient socketClient = new WebSocketClient(getServerUrl());
        MyGdxGame.socketInstance = socketClient;
        MyGdxGame.turnNotifier = new BrowserTurnNotifier();
        MyGdxGame.playerStorage = new BrowserPlayerStorage();
        MyGdxGame app = new MyGdxGame();
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
}
