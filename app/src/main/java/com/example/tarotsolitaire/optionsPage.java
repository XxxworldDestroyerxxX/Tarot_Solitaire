package com.example.tarotsolitaire;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class optionsPage extends BaseActivity {

    private MusicManager musicManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_options_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button Back = findViewById(R.id.btn_back);
        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        musicManager = MusicManager.get(this);

        // Example: build playlist from known resource names in res/raw (optional override)
        Resources r = getResources();
        int track1 = r.getIdentifier("hiphop_ithink", "raw", getPackageName());
        int track2 = r.getIdentifier("track2", "raw", getPackageName());
        int track3 = r.getIdentifier("track3", "raw", getPackageName());
        int[] ids = new int[]{};
        String[] names = new String[]{};
        java.util.List<Integer> idList = new java.util.ArrayList<>();
        java.util.List<String> nameList = new java.util.ArrayList<>();
        if (track1 != 0) { idList.add(track1); nameList.add("hiphop I think"); }
        if (track2 != 0) { idList.add(track2); nameList.add("Track 2"); }
        if (track3 != 0) { idList.add(track3); nameList.add("Track 3"); }
        if (!idList.isEmpty()) {
            ids = new int[idList.size()];
            names = new String[idList.size()];
            for (int i = 0; i < idList.size(); i++) { ids[i] = idList.get(i); names[i] = nameList.get(i); }
            musicManager.setPlaylistFromRaw(ids, names);
        }

        // Wire controls
        Switch swMute = findViewById(R.id.switch_music_mute);
        swMute.setChecked(musicManager.isMuted());
        swMute.setOnCheckedChangeListener((buttonView, isChecked) -> musicManager.setMuted(isChecked));

        TextView tvSong = findViewById(R.id.tv_current_song);

        ImageButton btnPlay = findViewById(R.id.btn_play_pause);
        ImageButton btnNext = findViewById(R.id.btn_next);
        ImageButton btnPrev = findViewById(R.id.btn_prev);

        Button btnLoop = findViewById(R.id.btn_loop_mode);
        SeekBar sb = findViewById(R.id.sb_music_volume);
        Spinner spinner = findViewById(R.id.spinner_song_list);

        // If there are no playable songs, show a clear message and disable controls
        String[] titles = musicManager.getTitles();
        int[] playlist = musicManager.getPlaylist();
        if (titles == null || titles.length == 0 || playlist == null || playlist.length == 0) {
            tvSong.setText("No playable songs found. Add MP3 files to res/raw (lowercase names) and re-run the app.");
            btnPlay.setEnabled(false);
            btnNext.setEnabled(false);
            btnPrev.setEnabled(false);
            btnLoop.setEnabled(false);
            sb.setEnabled(false);
            spinner.setEnabled(false);
            // keep mute switch available
            return;
        }

        // We have songs: initialize UI state
        tvSong.setText("Song: " + musicManager.getCurrentTitle());

        btnPlay.setOnClickListener(v -> {
            musicManager.togglePlayPause();
            btnPlay.setImageResource(musicManager.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });
        btnNext.setOnClickListener(v -> { musicManager.next(); tvSong.setText("Song: " + musicManager.getCurrentTitle()); });
        btnPrev.setOnClickListener(v -> { musicManager.prev(); tvSong.setText("Song: " + musicManager.getCurrentTitle()); });

        btnLoop.setOnClickListener(v -> {
            MusicManager.LoopMode cur = musicManager.getLoopMode();
            MusicManager.LoopMode next;
            if (cur == MusicManager.LoopMode.OFF) next = MusicManager.LoopMode.ONE;
            else if (cur == MusicManager.LoopMode.ONE) next = MusicManager.LoopMode.ALL;
            else next = MusicManager.LoopMode.OFF;
            musicManager.setLoopMode(next);
            btnLoop.setText("Loop: " + (next == MusicManager.LoopMode.OFF ? "Off" : next == MusicManager.LoopMode.ONE ? "One" : "All"));
        });

        sb.setProgress((int)(musicManager.getVolume() * 100f));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { musicManager.setVolume(progress / 100f); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (titles != null && titles.length > 0) {
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(musicManager.getIndex());
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position != musicManager.getIndex()) {
                        // set index by stepping until desired position
                        while (musicManager.getIndex() != position) musicManager.next();
                        tvSong.setText("Song: " + musicManager.getCurrentTitle());
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        // Register a listener to update UI when state changes
        musicManager.registerListener((isPlaying, idx, title) -> runOnUiThread(() -> {
            btnPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            tvSong.setText("Song: " + title);
            if (spinner.getAdapter() != null && spinner.getCount() > idx) spinner.setSelection(idx);
        }));
    }
}

