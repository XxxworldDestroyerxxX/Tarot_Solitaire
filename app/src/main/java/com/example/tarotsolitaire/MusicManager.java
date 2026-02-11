package com.example.tarotsolitaire;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MusicManager {
    private static final String TAG = "MusicManager";
    private static MusicManager instance;
    private final Context app;
    private MediaPlayer player;
    private int[] playlist = new int[0];
    private String[] titles = new String[0];
    private int index = 0;
    private boolean isMuted = false;
    private float volume = 1.0f;
    public enum LoopMode { OFF, ONE, ALL }
    private LoopMode loopMode = LoopMode.OFF;
    private SharedPreferences prefs;
    private final List<Listener> listeners = new ArrayList<>();

    public interface Listener {
        void onStateChanged(boolean isPlaying, int index, String title);
    }

    private MusicManager(Context ctx) {
        app = ctx.getApplicationContext();
        prefs = app.getSharedPreferences("musicPrefs", Context.MODE_PRIVATE);
        index = prefs.getInt("music_index", 0);
        isMuted = prefs.getBoolean("music_muted", false);
        volume = prefs.getFloat("music_volume", 1.0f);
        int lm = prefs.getInt("music_loop_mode", 0);
        try { loopMode = LoopMode.values()[lm]; } catch (Exception e) { loopMode = LoopMode.OFF; }

        // If no playlist provided by code, try to auto-load any raw resources
        if (playlist == null || playlist.length == 0) {
            loadPlaylistFromRawResources();
        }

        // Auto-start playback if preference requests it and we have a playlist
        boolean shouldPlay = prefs.getBoolean("music_is_playing", true);
        if (shouldPlay && playlist != null && playlist.length > 0) {
            try {
                play();
            } catch (Exception e) {
                Log.w(TAG, "Auto-play failed", e);
            }
        }
    }

    public static synchronized MusicManager get(@NonNull Context ctx) {
        if (instance == null) instance = new MusicManager(ctx);
        return instance;
    }

    public void setPlaylistFromRaw(int[] resIds, String[] names) {
        this.playlist = resIds != null ? resIds : new int[0];
        this.titles = names != null ? names : new String[0];
        if (index < 0 || index >= playlist.length) index = 0;
    }

    /**
     * Reflectively inspects R.raw to build a playlist. Only resource names with
     * lowercase letters, digits and underscores are included. The list is sorted
     * alphabetically by resource name to produce a deterministic order.
     *
     * This method now attempts to verify that each found resource can be opened by
     * MediaPlayer. Non-playable resources (e.g. placeholder text files) are skipped.
     */
    private void loadPlaylistFromRawResources() {
        try {
            Class<?> rawClass = Class.forName(app.getPackageName() + ".R$raw");
            Field[] fields = rawClass.getFields();
            List<String> names = new ArrayList<>();
            for (Field f : fields) {
                String name = f.getName();
                // accept common resource-name characters; skip generated/invalid names
                if (name != null && name.matches("[a-z0-9_]+")) {
                    names.add(name);
                }
            }
            if (names.isEmpty()) return;
            // sort names to have deterministic order
            Collections.sort(names, Comparator.naturalOrder());

            List<Integer> validIds = new ArrayList<>();
            List<String> validTitles = new ArrayList<>();

            for (String nm : names) {
                int resId = app.getResources().getIdentifier(nm, "raw", app.getPackageName());
                if (resId == 0) continue;
                // Try to create a short-lived MediaPlayer to validate the resource is a playable audio file
                MediaPlayer probe = null;
                boolean playable = false;
                try {
                    probe = MediaPlayer.create(app, resId);
                    if (probe != null) playable = true;
                } catch (Exception e) {
                    Log.w(TAG, "Resource not playable: " + nm + " (resId=" + resId + ")", e);
                } finally {
                    if (probe != null) {
                        try { probe.release(); } catch (Exception ignored) {}
                        probe = null;
                    }
                }
                if (!playable) {
                    Log.i(TAG, "Skipping non-playable raw resource: " + nm);
                    continue;
                }

                // make a user-friendly title: replace _ with space and capitalize words
                String title = nm.replace('_', ' ');
                String[] parts = title.split(" ");
                StringBuilder sb = new StringBuilder();
                for (int p = 0; p < parts.length; p++) {
                    if (parts[p].length() == 0) continue;
                    sb.append(parts[p].substring(0,1).toUpperCase()).append(parts[p].substring(1));
                    if (p < parts.length - 1) sb.append(' ');
                }

                validIds.add(resId);
                validTitles.add(sb.toString());
            }

            if (!validIds.isEmpty()) {
                playlist = new int[validIds.size()];
                titles = new String[validTitles.size()];
                for (int i = 0; i < validIds.size(); i++) { playlist[i] = validIds.get(i); titles[i] = validTitles.get(i); }
                if (index < 0 || index >= playlist.length) index = 0;
            }
        } catch (ClassNotFoundException cnfe) {
            Log.i(TAG, "R.raw not found via reflection: " + cnfe.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "Error loading raw resources playlist", e);
        }
    }

    private void createPlayer() {
        release();
        if (playlist.length == 0) return;
        int res = playlist[index];
        try {
            player = MediaPlayer.create(app, res);
            if (player == null) {
                Log.w(TAG, "createPlayer: MediaPlayer.create returned null for res=" + res);
                return;
            }
            player.setLooping(false);
            player.setOnCompletionListener(mp -> onTrackCompleted());
            applyVolume();
        } catch (Exception e) {
            Log.e(TAG, "createPlayer: failed to create MediaPlayer", e);
            player = null;
        }
    }

    private void onTrackCompleted() {
        if (loopMode == LoopMode.ONE) {
            play();
            return;
        }
        index++;
        if (index >= playlist.length) {
            if (loopMode == LoopMode.ALL) index = 0; else index = playlist.length - 1;
        }
        saveIndex();
        play();
    }

    public void play() {
        if (playlist.length == 0) return;
        if (player == null) createPlayer();
        if (player != null && !player.isPlaying()) player.start();
        prefs.edit().putBoolean("music_is_playing", true).apply();
        notifyListeners();
    }

    public void pause() {
        if (player != null && player.isPlaying()) player.pause();
        prefs.edit().putBoolean("music_is_playing", false).apply();
        notifyListeners();
    }

    public void togglePlayPause() {
        if (player != null && player.isPlaying()) pause(); else play();
    }

    public void next() {
        if (playlist.length == 0) return;
        index = (index + 1) % playlist.length;
        saveIndex();
        if (player != null) createPlayer();
        play();
    }

    public void prev() {
        if (playlist.length == 0) return;
        index = (index - 1 + playlist.length) % playlist.length;
        saveIndex();
        if (player != null) createPlayer();
        play();
    }

    public void setLoopMode(LoopMode m) {
        loopMode = m;
        prefs.edit().putInt("music_loop_mode", m.ordinal()).apply();
    }

    public LoopMode getLoopMode() { return loopMode; }

    public void setMuted(boolean muted) {
        isMuted = muted;
        prefs.edit().putBoolean("music_muted", muted).apply();
        applyVolume();
    }

    public boolean isMuted() { return isMuted; }

    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        prefs.edit().putFloat("music_volume", volume).apply();
        applyVolume();
    }

    public float getVolume() { return volume; }

    private void applyVolume() {
        if (player != null) {
            float actual = isMuted ? 0f : volume;
            player.setVolume(actual, actual);
        }
    }

    private void saveIndex() { prefs.edit().putInt("music_index", index).apply(); }

    public String getCurrentTitle() {
        if (titles == null || titles.length == 0) return "";
        if (index < 0 || index >= titles.length) return "";
        return titles[index];
    }

    public boolean isPlaying() { return player != null && player.isPlaying(); }

    public int getIndex() { return index; }

    public void registerListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void unregisterListener(Listener l) { listeners.remove(l); }

    private void notifyListeners() {
        boolean playing = isPlaying();
        String title = getCurrentTitle();
        for (Listener l : listeners) {
            try { l.onStateChanged(playing, index, title); } catch (Exception ignored) {}
        }
    }

    public void release() { if (player != null) { try { player.release(); } catch (Exception ignored) {} player = null; } }

    // Public getters for playlist and titles
    public int[] getPlaylist() { return playlist.clone(); }
    public String[] getTitles() { return titles.clone(); }
}
