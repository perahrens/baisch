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
     * Installs a one-shot DOM touchstart/click listener that on the first user
     * gesture:
     *  1. Resumes the Web Audio AudioContext (required on iOS Safari).
     *  2. Calls play() on the active music track synchronously from the DOM event
     *     handler — the only reliable way to start HTMLAudioElement on the very
     *     first gesture on Android Chrome/Firefox, which reject play() when called
     *     from requestAnimationFrame on first interaction.
     */
    private static native void installAudioUnlocker(MyGdxGame app) /*-{
        var handler = function() {
            $doc.removeEventListener('touchstart', handler, true);
            $doc.removeEventListener('click',      handler, true);
            var AudioCtx = $wnd.AudioContext || $wnd.webkitAudioContext;
            if (AudioCtx) {
                var ctx = new AudioCtx();
                ctx.resume().then(function() { ctx.close(); });
            }
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
