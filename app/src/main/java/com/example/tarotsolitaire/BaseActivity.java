package com.example.tarotsolitaire;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * BaseActivity centralizes status-bar hiding behavior.
 * By default activities that extend this will hide the status bar while they have focus.
 * Override shouldHideStatusBar() to opt-out for specific activities.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // no-op here; concrete activities will still set contentView and edge-to-edge handling
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldHideStatusBar()) hideStatusBar();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // "Hide when activity has focus" means: once this Activity is in front and has window focus,
        // re-apply the hide so transient system UI (e.g. from dialogs or toasts) does not stay visible.
        if (hasFocus && shouldHideStatusBar()) hideStatusBar();
    }

    /**
     * Override this to opt-out of hiding the status bar for a specific Activity.
     */
    protected boolean shouldHideStatusBar() {
        return true;
    }

    /**
     * Override this to control whether the navigation (bottom) bar should also be hidden.
     * Default is true (immersive).
     */
    protected boolean shouldHideNavigationBar() { return true; }

    /**
     * Hide only the status bar (time/battery area) and optionally the navigation bar.
     * Leaves navigation bars alone if shouldHideNavigationBar() returns false.
     */
    protected void hideStatusBar() {
        try {
            // Let content draw behind system bars so we can be truly fullscreen
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

            int typesToHide = WindowInsetsCompat.Type.statusBars();
            if (shouldHideNavigationBar()) {
                typesToHide |= WindowInsetsCompat.Type.navigationBars();
            }

            insetsController.hide(typesToHide);
            // Allow users to swipe to temporarily reveal and then auto-hide again
            insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } catch (Exception ignored) {
            // Swallow any issues on older devices; hiding status bar is best-effort.
        }
    }

    /**
     * Show the status/navigation bars again and restore decor fitting.
     */
    protected void showStatusBar() {
        try {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            int typesToShow = WindowInsetsCompat.Type.statusBars();
            if (shouldHideNavigationBar()) typesToShow |= WindowInsetsCompat.Type.navigationBars();
            insetsController.show(typesToShow);
        } catch (Exception ignored) {}
    }
}
