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
        WebSocketClient socketClient = new WebSocketClient("http://localhost:8082");
        MyGdxGame.socketInstance = socketClient;
        return new MyGdxGame();
    }
}
