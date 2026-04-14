package com.mygdx.game;

import android.os.Bundle;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class AndroidLauncher extends AndroidApplication {

	/** Production server URL. Change to http://10.0.2.2:8080 for local emulator testing. */
	private static final String SERVER_URL = "https://baisch-game.fly.dev";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			SocketIoClient socketClient = new SocketIoClient(SERVER_URL);
			MyGdxGame.socketInstance = socketClient;
		} catch (Exception e) {
			Log.e("Baisch", "Failed to create socket client: " + e.getMessage());
		}
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new MyGdxGame(), config);
	}
}

