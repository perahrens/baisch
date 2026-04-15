package com.mygdx.game;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class IOSLauncher extends IOSApplication.Delegate {

    /** Production server URL. Change to http://localhost:8080 for local simulator testing. */
    private static final String SERVER_URL = "https://baisch-game.fly.dev";

    @Override
    protected IOSApplication createApplication() {
        MyGdxGame.playerStorage = new IOSPlayerStorage();
        try {
            IOSSocketClient socketClient = new IOSSocketClient(SERVER_URL);
            MyGdxGame.socketInstance = socketClient;
        } catch (Exception e) {
            System.err.println("Baisch: Failed to create socket client: " + e.getMessage());
        }
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        return new IOSApplication(new MyGdxGame(), config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}