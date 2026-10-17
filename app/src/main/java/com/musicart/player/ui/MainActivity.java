package com.musicart.player.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.musicart.player.view.MainView;
import com.musicart.player.view.PlayerView;
import com.musicart.player.view.PlaylistSheetView;

/** 主 Activity：装载全部自绘 View，管理页面/弹层层级。 */
public class MainActivity extends Activity implements MainView.Host, PlayerView.Host {

    private FrameLayout root;
    private MainView mainView;
    private PlayerView playerView;
    private PlaylistSheetView sheetView;
    private View toastView;
    private Handler ui = new Handler();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        setContentView(root);

        // 1) 主界面
        mainView = new MainView(this);
        mainView.setHost(this);
        root.addView(mainView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 2) 全屏播放页（默认隐藏）
        playerView = new PlayerView(this);
        playerView.setHost(this);
        playerView.setAlpha(0f);
        root.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 3) 播放列表 bottom sheet（默认隐藏）
        sheetView = new PlaylistSheetView(this);
        sheetView.setVisibility(View.GONE);
        root.addView(sheetView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    // ---------- Host 回调 ----------
    @Override public void openPlayer() {
        playerView.setPlayerOpen(true);
        mainView.setPlayerOpen(true);
        playerView.animate().alpha(1f).setDuration(260).start();
    }

    @Override public void onClosePlayer() {
        mainView.setPlayerOpen(false);
        playerView.animate().alpha(0f).setDuration(220).withEndAction(() -> {
            playerView.setPlayerOpen(false);
        }).start();
    }

    @Override public void openPlaylist() {
        showPlaylist();
    }

    @Override public void onOpenPlaylist() {
        showPlaylist();
    }

    private void showPlaylist() {
        sheetView.setHost(new PlaylistSheetView.Host() {
            @Override public void onPick(int idx) {
                com.musicart.player.MusicApp.get().playIndex(idx);
            }
            @Override public void onClose() { sheetView.setVisibility(View.GONE); }
        });
        sheetView.setVisibility(View.VISIBLE);
    }

    @Override public void openSearch() {
        showSearchDialog();
    }

    @Override public void showToast(String msg) {
        Toast t = new Toast(this);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(dp(20), dp(12), dp(20), dp(12));
        tv.setBackgroundColor(0xCC33304D);
        t.setView(tv);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
    }

    /** 自绘搜索弹窗：圆角卡片 + 透明 EditText 承接键盘输入。 */
    private void showSearchDialog() {
        final FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(0xEE1E1B33);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(box, bp);

        // 输入框容器
        FrameLayout bar = new FrameLayout(this);
        float pad = dp(16);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins((int)pad, (int)dp(50), (int)pad, 0);
        bar.setLayoutParams(lp);
        bar.setBackground(new android.graphics.drawable.GradientDrawable());
        ((android.graphics.drawable.GradientDrawable)bar.getBackground()).setShape(
                android.graphics.drawable.GradientDrawable.RECTANGLE);
        ((android.graphics.drawable.GradientDrawable)bar.getBackground()).setCornerRadius(dp(26));
        ((android.graphics.drawable.GradientDrawable)bar.getBackground()).setColor(Color.WHITE);

        final EditText et = new EditText(this);
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setHint("输入歌曲 / 歌手…");
        et.setTextColor(0xFF2B2740);
        et.setHintTextColor(0xFF9A94B8);
        et.setPadding(dp(20), 0, dp(20), 0);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        bar.addView(et, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        box.addView(bar);

        et.requestFocus();
        et.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        });

        et.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String kw = et.getText().toString().trim();
                if (!kw.isEmpty()) mainView.doSearch(kw);
                root.removeView(box);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        box.setOnTouchListener((v, ev) -> {
            root.removeView(box);
            return true;
        });
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
