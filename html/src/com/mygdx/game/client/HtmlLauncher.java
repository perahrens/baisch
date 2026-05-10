package com.mygdx.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;
import com.mygdx.game.Localization;
import com.mygdx.game.MyGdxGame;
import com.mygdx.game.net.DiagListener;

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
        MyGdxGame.keyboardHelper = new BrowserKeyboardHelper();
        MyGdxGame app = new MyGdxGame();
        // Tell the core that the HTML layer owns the music button visual.
        MyGdxGame.nativeMusicButton = true;
        // Inject the animated GIF music button into the DOM and wire up callbacks.
        installMusicButton(app);
        installLanguageButton(app);
        // Register the UI-update callback so setMusicEnabled() keeps the GIF in sync.
        MyGdxGame.onMusicUiUpdate = new Runnable() {
            @Override
            public void run() {
                refreshMusicButton(MyGdxGame.playerStorage.getMusicEnabled());
            }
        };
        MyGdxGame.onLanguageUiUpdate = new Runnable() {
            @Override
            public void run() {
                refreshLanguageButton(Localization.getLanguage());
            }
        };
        MyGdxGame.onGameScreenActive = new Runnable() {
            @Override
            public void run() {
                setMusicButtonVisible(false);
                setLanguageButtonVisible(false);
                setViewportBackgroundMode(false);
            }
        };
        MyGdxGame.onMenuScreenActive = new Runnable() {
            @Override
            public void run() {
                setMusicButtonVisible(true);
                setLanguageButtonVisible(true);
                refreshLanguageButton(Localization.getLanguage());
                setViewportBackgroundMode(true);
            }
        };
        MyGdxGame.onNameEntryScreenActive = new Runnable() {
            @Override
            public void run() {
                setNameEntryLogoVisible(true);
            }
        };
        MyGdxGame.onNameEntryScreenDone = new Runnable() {
            @Override
            public void run() {
                setNameEntryLogoVisible(false);
            }
        };
        MyGdxGame.onRoundtripRecorded = new DiagListener() {
            @Override
            public void onRoundtrip(int seq, int ms) {
                WebSocketClient.nativeRecordRoundtrip(seq, ms);
            }
        };
        installAudioUnlocker(app);
        return app;
    }

    /**
     * Installs a DOM listener that unlocks audio and starts the music track
     * on the first valid user activation event.
     *
     * The listener keeps retrying until the music track is actually loaded
     * (activeMusic != null), so it works correctly even when the first user
     * interaction occurs during the loading screen before GWT initialises.
     *
     * IMPORTANT: We listen for 'touchend' and 'click', NOT 'touchstart'.
     * Android Chrome only grants audio playback permission on activation events
     * like touchend, click, or pointerup — NOT on touchstart.
     * (touchstart fires too early; the browser hasn't committed to treating
     * the gesture as user activation yet.)
     */
    private static native void installAudioUnlocker(MyGdxGame app) /*-{
        var musicTriggered = false;
        var handler = function(evt) {
            if (musicTriggered) return;

            // Unlock Web Audio API (iOS Safari)
            var AudioCtx = $wnd.AudioContext || $wnd.webkitAudioContext;
            if (AudioCtx) {
                try { var ctx = new AudioCtx(); ctx.resume().then(function() { ctx.close(); }); } catch(e) {}
            }

            // Start the actual music track — now from a valid activation event
            app.@com.mygdx.game.MyGdxGame::resumeMusicIfEnabled()();

            // Remove listeners only once the music track is ready.
            // If the game hadn't finished loading yet (activeMusic == null),
            // keep the listeners so we retry on the next user interaction.
            if (app.@com.mygdx.game.MyGdxGame::isMusicActive()()) {
                musicTriggered = true;
                $doc.removeEventListener('touchend', handler, true);
                $doc.removeEventListener('click',    handler, true);
            }
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

    /**
     * Injects a fixed-position language button next to the music button.
     * Clicking opens a small downward popup with other language flags.
     */
    private static native void installLanguageButton(MyGdxGame app) /*-{
        var langs = [
            { code: 'en', src: '/assets/data/graphics/ui/lang_en.png', label: 'EN' },
            { code: 'de', src: '/assets/data/graphics/ui/lang_de.png', label: 'DE' },
            { code: 'ru', src: '/assets/data/graphics/ui/lang_ru.png', label: 'RU' },
            { code: 'no', src: '/assets/data/graphics/ui/lang_no.png', label: 'NO' },
            { code: 'it', src: '/assets/data/graphics/ui/lang_it.png', label: 'IT' }
        ];

        var holder = $doc.createElement('div');
        holder.id = 'baisch-lang-holder';
        holder.style.cssText =
            'position:fixed;top:6px;right:66px;z-index:9999;display:block;';

        var btn = $doc.createElement('img');
        btn.id = 'baisch-lang-btn';
        btn.style.cssText =
            'width:48px;height:32px;cursor:pointer;display:block;border-radius:2px;';

        var popup = $doc.createElement('div');
        popup.id = 'baisch-lang-popup';
        popup.style.cssText =
            'position:absolute;top:36px;right:0;display:none;padding:4px;background:rgba(20,20,20,0.95);' +
            'border:1px solid rgba(255,255,255,0.25);border-radius:4px;';

        holder.appendChild(btn);
        holder.appendChild(popup);
        $doc.body.appendChild(holder);

        function langByCode(code) {
            for (var i = 0; i < langs.length; i++) if (langs[i].code === code) return langs[i];
            return langs[0];
        }

        function setButtonLanguage(code) {
            var lang = langByCode(code || 'en');
            btn.src = lang.src;
            btn.alt = lang.label;
        }

        function rebuildPopup(currentCode) {
            popup.innerHTML = '';
            for (var i = 0; i < langs.length; i++) {
                var lang = langs[i];
                if (lang.code === currentCode) continue;
                var opt = $doc.createElement('img');
                opt.src = lang.src;
                opt.alt = lang.label;
                opt.style.cssText = 'width:48px;height:32px;display:block;cursor:pointer;margin:2px 0;border-radius:2px;';
                (function(code) {
                    opt.addEventListener('click', function(ev) {
                        ev.stopPropagation();
                        popup.style.display = 'none';
                        @com.mygdx.game.MyGdxGame::handleLanguageButtonClick(Ljava/lang/String;)(code);
                    });
                })(lang.code);
                popup.appendChild(opt);
            }
        }

        $wnd._baischSetLangBtn = function(code) {
            var current = code || ($wnd.localStorage.getItem('baisch_language') || 'en');
            setButtonLanguage(current);
            rebuildPopup(current);
        };

        btn.addEventListener('click', function(ev) {
            ev.stopPropagation();
            popup.style.display = (popup.style.display === 'none' || popup.style.display === '') ? 'block' : 'none';
        });

        $doc.addEventListener('click', function() {
            popup.style.display = 'none';
        }, true);

        var initialLang = $wnd.localStorage.getItem('baisch_language') || 'en';
        $wnd._baischSetLangBtn(initialLang);
    }-*/;

    /** Called from the onMusicUiUpdate Runnable to keep the GIF in sync with Java state. */
    private static native void refreshMusicButton(boolean enabled) /*-{
        if ($wnd._baischSetMusicBtn) $wnd._baischSetMusicBtn(enabled);
    }-*/;

    /** Called from Java whenever language state changes. */
    private static native void refreshLanguageButton(String languageCode) /*-{
        if ($wnd._baischSetLangBtn) $wnd._baischSetLangBtn(languageCode);
    }-*/;

    /** Shows or hides the DOM music button when switching between game and menu screens. */
    private static native void setMusicButtonVisible(boolean visible) /*-{
        var img = $doc.getElementById('baisch-music-img');
        if (img) img.style.display = visible ? 'block' : 'none';
    }-*/;

    /** Shows or hides the DOM language button. */
    private static native void setLanguageButtonVisible(boolean visible) /*-{
        var holder = $doc.getElementById('baisch-lang-holder');
        if (!holder) return;
        holder.style.display = visible ? 'block' : 'none';
        var popup = $doc.getElementById('baisch-lang-popup');
        if (popup) popup.style.display = 'none';
    }-*/;

    /**
     * Keeps the browser letterbox around the canvas visually aligned with the
     * active screen: darkmoon cover for menus, neutral gradient for gameplay.
     */
    private static native void setViewportBackgroundMode(boolean menuActive) /*-{
        var body = $doc.body;
        if (!body) return;

        if (menuActive) {
            body.style.background = "#000 url('assets/data/graphics/bg_darkmoon.jpg') center center / cover no-repeat";
        } else {
            body.style.background = 'radial-gradient(ellipse at center, #3a3a3a 0%, #000000 100%)';
        }
    }-*/;

    /** Shows or hides the name-entry logo overlay (crisp HTML BAISCH title + suits). */
    private static native void setNameEntryLogoVisible(boolean visible) /*-{
        var el = $doc.getElementById('name-entry-logo');
        if (el) el.style.display = visible ? 'flex' : 'none';
    }-*/;
}
