package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 450;
		config.height = 800;
		try {
			SocketIoClient socketClient = new SocketIoClient("http://localhost:8082");
			// connect() is called by MyGdxGame after event listeners are registered
			MyGdxGame.socketInstance = socketClient;
		} catch (Exception e) {
			System.err.println("Failed to connect socket: " + e.getMessage());
		}
		new LwjglApplication(new MyGdxGame(), config);
	}
}
