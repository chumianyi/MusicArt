package com.musicart.player.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.musicart.player.MusicApp;
import com.musicart.player.view.MainView;
import com.musicart.player.view.PlayerView;
import com.musicart.player.view.PlaylistSheetView;

public class MainActivity extends Activity implements MainView.Host, PlayerView.Host, PlaylistSheetView.Host {

    private FrameLayout root;
    private MainView mainView;
    private PlayerView playerView;
    private PlaylistSheetView sheetView;
    private View searchBox;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        setContentView(root);

        mainView = new MainView(this);
        mainView.setHost(this);
        root.addView(mainView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        playerView = new PlayerView(this);
        playerView.setHost(this);
        playerView.setVisibility(View.GONE);
        root.addView(playerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        sheetView = new PlaylistSheetView(this);
        sheetView.setHost(this);
        sheetView.setVisibility(View.GONE);
        root.addView(sheetView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
    }

    public void openPlayer() { playerView.setVisibility(View.VISIBLE); }
    public void onClosePlayer() { playerView.setVisibility(View.GONE); }
    public void openPlaylist() { sheetView.setVisibility(View.VISIBLE); }
    public void onOpenPlaylist() { sheetView.setVisibility(View.VISIBLE); }

    public void onPick(int idx) { MusicApp.get().playIndex(idx); }
    public void onClose() { sheetView.setVisibility(View.GONE); }

    public void showToast(String msg) {
        Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        TextView tv = new TextView(this);
        tv.setText(msg); tv.setTextColor(Color.WHITE);
        tv.setPadding(dp(20), dp(12), dp(20), dp(12));
        tv.setBackgroundColor(0xCC33304D);
        t.setView(tv); t.show();
    }

    public void openSearch() {
        final FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(0xEE1E1B33);
        root.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        FrameLayout bar = new FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, dp(52));
        lp.setMargins(dp(16), dp(50), dp(16), 0);
        bar.setLayoutParams(lp);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(26));
        gd.setColor(Color.WHITE);
        bar.setBackgroundDrawable(gd);

        final EditText et = new EditText(this);
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setHint("输入歌曲 / 歌手…");
        et.setTextColor(0xFF2B2740);
        et.setHintTextColor(0xFF9A94B8);
        et.setPadding(dp(20), 0, dp(20), 0);
        bar.addView(et, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        box.addView(bar);

        // 自绘搜索按钮
        View btn = new View(this) {
            protected void onDraw(android.graphics.Canvas c) {}
        };
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(dp(80), dp(48));
        blp.gravity = android.view.Gravity.RIGHT | android.view.Gravity.TOP;
        blp.setMargins(0, dp(52), dp(16), 0);
        btn.setLayoutParams(blp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(24));
        bg.setColor(0xFF8E7CFF);
        btn.setBackgroundDrawable(bg);
        final TextView btntv = new TextView(this);
        btntv.setText("搜索"); btntv.setTextColor(Color.WHITE);
        btntv.setGravity(android.view.Gravity.CENTER);
        box.addView(btn, blp);
        box.addView(btntv, blp);

        et.requestFocus();
        et.postDelayed(new Runnable() { public void run() {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        }}, 200);

        btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doSearch(et); }});
        btntv.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doSearch(et); }});

        box.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent ev) {
                if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) closeSearch(box);
                return true;
            }
        });
        searchBox = box;
    }

    private void doSearch(EditText et) {
        String kw = et.getText().toString().trim();
        closeSearch(searchBox);
        if (kw.length() > 0) mainView.doSearch(kw);
    }
    private void closeSearch(View box) {
        if (box != null) root.removeView(box);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
