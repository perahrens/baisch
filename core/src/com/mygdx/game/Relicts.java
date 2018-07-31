package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class Relicts extends ApplicationAdapter implements InputProcessor {
	SpriteBatch batch;
	Sound sound;
	long id;
	Texture img;
	TiledMap tm;
	TiledMapRenderer tmr;
	int frame = 0;
	int zeile = 0;
	Sprite sprite;
	TextureRegion[][] regions;
	float movement = 0f;
	Timer attack;
	OrthographicCamera ocam;
	BitmapFont font;
	
	@Override
	public void create () {
		sound = Gdx.audio.newSound(Gdx.files.internal("data/sounds/Bum.mp3"));
		font = new BitmapFont();
		font.setColor(Color.BLUE);
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		ocam = new OrthographicCamera(1,h/w);
		tm = new TmxMapLoader().load("data/maps/myMap.tmx");
		tmr = new OrthogonalTiledMapRenderer(tm);
		batch = new SpriteBatch();
		img = new Texture("data/SpriteSheetCollection/dannyphantomtheultimateenemy_meatghost_sheet.png");
		regions = TextureRegion.split(img, 51, 50 );
		sprite = new Sprite(regions[0][0]);
		sprite.setScale(1f);
		attack = new Timer();
		attack.scheduleTask(new Task() {
			public void run() {
				if (frame >= 8) {
					frame = 0;
					if (zeile == 1 ) {
						zeile = 0;
					} else {
						zeile = 1;
					}
				}
				//System.out.println(zeile + "and" + frame);
				sprite.setRegion(regions[zeile][frame]);
				frame++;
			}
		}, 0, 1/10f);
		attack.stop();
		Gdx.input.setInputProcessor(this);
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		sprite.translateX(movement);
		
		if (!sprite.isFlipX() && movement > 0) 
			sprite.flip(true, false);
		if (sprite.isFlipX() && movement < 0) 
			sprite.flip(true, false);
		
		tmr.setView(ocam);
		tmr.render();
		
		ocam.position.x = sprite.getX() + sprite.getOriginX();
		ocam.position.y = sprite.getY() + sprite.getOriginY();
		ocam.zoom = 500f;
		
		ocam.update();
		batch.setProjectionMatrix(ocam.combined);
		batch.begin();
		sprite.draw(batch);
		
		font.draw(batch, "Miau das wird Baisch", 50, 50);
		
		//batch.draw(img, 0, 0);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Keys.LEFT) {
			if (sprite.getX() > 0) {
				movement = -5f;
				//sprite.translateX(-5f);
			}
		}
		if (keycode == Keys.RIGHT) {
			if (!sprite.isFlipX()) sprite.flip(true, false);
			if (sprite.getX() < 450) {
				movement = 5f;
			}
		}
		/*
		if (keycode == Keys.DOWN) {
			sprite.translateY(-5f);
		}
		if (keycode == Keys.UP) {
			sprite.translateY(5f);
		}*/
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (keycode == Keys.LEFT) {
			if (movement == -5f) {
				movement = 0f;
			}
		}
		if (keycode == Keys.RIGHT) {
			if (movement == 5f) {
				movement = 0f;
			}
		}
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (button == Buttons.LEFT) {
			attack.start();
			//sound.play();
			id = sound.loop();
		}
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (button == Buttons.LEFT) {
			attack.stop();
			sound.stop(id);
		}
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}
}
