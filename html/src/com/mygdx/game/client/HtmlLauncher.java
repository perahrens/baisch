package com.mygdx.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(450, 800);
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
     * Installs a one-shot DOM touchstart/click listener that on the first user gesture:
     *  1. Resumes the Web Audio AudioContext (required on iOS Safari).
     *  2. Plays a tiny silent HTMLAudioElement to grant "sticky activation".
     *  3. Calls resumeMusicIfEnabled() on the game to start the actual music track
     *     synchronously from the DOM event — the ONLY way to reliably start audio
     *     on the very first interaction on mobile browsers.
     */
    private static native void installAudioUnlocker(MyGdxGame app) /*-{
        var handler = function() {
            $doc.removeEventListener('touchstart', handler, true);
            $doc.removeEventListener('click',      handler, true);
            // Unlock Web Audio API (iOS Safari)
            var AudioCtx = $wnd.AudioContext || $wnd.webkitAudioContext;
            if (AudioCtx) {
                try { var ctx = new AudioCtx(); ctx.resume().then(function() { ctx.close(); }); } catch(e) {}
            }
            // Play silent clip to grant sticky activation
            try {
                var s = new $wnd.Audio();
                s.src = 'data:audio/wav;base64,UklGRigAAABXQVZFZm10IBIAAAABAAEARKwAAIhYAQACABAAAABkYXRhAgAAAAEA';
                s.play();
            } catch(e) {}
            // Start the actual music track from DOM context
            app.@com.mygdx.game.MyGdxGame::resumeMusicIfEnabled()();
        };
        $doc.addEventListener('touchstart', handler, true);
        $doc.addEventListener('click',      handler, true);
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
