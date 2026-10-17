package com.musicart.player.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import com.musicart.player.view.SplashView;

public class SplashActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new SplashView(this));
        new Handler().postDelayed(new Runnable() { public void run() {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }}, 2600);
    }
}
