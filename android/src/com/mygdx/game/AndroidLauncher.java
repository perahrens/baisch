package com.mygdx.game;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AndroidLauncher extends AndroidApplication {

	/** Production server URL. Change to http://10.0.2.2:8080 for local emulator testing. */
	private static final String SERVER_URL = "https://baisch-game.fly.dev";

	private static final String RELEASES_URL = "https://github.com/perahrens/baisch/releases/latest";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyGdxGame.playerStorage = new AndroidPlayerStorage(this);
		try {
			SocketIoClient socketClient = new SocketIoClient(SERVER_URL);
			MyGdxGame.socketInstance = socketClient;
		} catch (Exception e) {
			Log.e("Baisch", "Failed to create socket client: " + e.getMessage());
		}
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new MyGdxGame(), config);
		checkForUpdate();
	}

	/** Checks the server for a newer version in the background. Silently ignored if offline. */
	private void checkForUpdate() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				try {
					conn = (HttpURLConnection) new URL(SERVER_URL + "/version").openConnection();
					conn.setConnectTimeout(5000);
					conn.setReadTimeout(5000);
					conn.setRequestMethod("GET");
					if (conn.getResponseCode() == 200) {
						StringBuilder sb = new StringBuilder();
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
							String line;
							while ((line = reader.readLine()) != null) sb.append(line);
						}
						String serverVersion = parseVersion(sb.toString());
						if (serverVersion != null && !serverVersion.equals(BuildConfig.VERSION_NAME)) {
							showUpdateDialog();
						}
					}
				} catch (Exception e) {
					Log.w("Baisch", "Version check failed: " + e.getMessage());
				} finally {
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	/** Parses the version string out of a JSON object like {"version":"1.2.3"}. */
	private String parseVersion(String json) {
		int idx = json.indexOf("\"version\"");
		if (idx < 0) return null;
		int open = json.indexOf("\"", json.indexOf(":", idx) + 1);
		if (open < 0) return null;
		int close = json.indexOf("\"", open + 1);
		if (close < 0) return null;
		return json.substring(open + 1, close);
	}

	private void showUpdateDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (isFinishing() || isDestroyed()) return;
				new AlertDialog.Builder(AndroidLauncher.this)
						.setTitle("Update available")
						.setMessage("A new version of Baisch is available. Update now?")
						.setPositiveButton("Update", (dialog, which) ->
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))))
						.setNegativeButton("Later", null)
						.show();
			}
		});
	}
}

