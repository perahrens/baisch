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
        installAudioContextUnlocker();
        return new MyGdxGame();
    }

    /**
     * Installs a one-shot DOM-level listener that resumes the Web Audio AudioContext
     * on the first user gesture (click or touchstart). This activates the page for
     * audio playback under browser autoplay policy, which is required on mobile
     * before any HTMLAudioElement.play() call can succeed from a rAF callback.
     */
    private static native void installAudioContextUnlocker() /*-{
        var handler = function() {
            var AudioCtx = $wnd.AudioContext || $wnd.webkitAudioContext;
            if (AudioCtx) {
                var ctx = new AudioCtx();
                ctx.resume().then(function() { ctx.close(); });
            }
            $doc.removeEventListener('touchstart', handler, true);
            $doc.removeEventListener('click',      handler, true);
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
