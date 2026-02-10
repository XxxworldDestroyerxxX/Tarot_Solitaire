package com.example.tarotsolitaire;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Leaderboard extends BaseActivity {

    private RecyclerView recyclerView;
    private LeaderAdapter adapter;
    private TextView tvPlayerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Edge-to-edge
        EdgeToEdge.enable(this);
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvPlayerInfo = findViewById(R.id.tv_player_info);
        recyclerView = findViewById(R.id.rv_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderAdapter();
        recyclerView.setAdapter(adapter);

        // Force LTR layout direction so leaderboard looks the same regardless of device locale (prevents RTL mirroring)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        Button back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> {
            startActivity(new Intent(Leaderboard.this, MainMenu.class));
            finish();
        });

        enableOfflinePersistence();
        attachLeaderboardListener();
    }

    private void enableOfflinePersistence() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }

    private void attachLeaderboardListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Listen to changes and also use cache-first for offline
        Query q = db.collection("users")
                .whereGreaterThanOrEqualTo("bestTime", 0)
                .orderBy("bestTime", Query.Direction.ASCENDING)
                .limit(100);

        // Real-time listener; UI updates as docs change. Uses server cache + sync.
        q.addSnapshotListener((snap, error) -> {
            if (error != null) return;
            if (snap == null) return;

            List<Entry> entries = new ArrayList<>();
            int rank = 1;
            String currentNick = getSavedNickname();
            int currentRank = -1;
            long currentTime = -1;

            for (QueryDocumentSnapshot doc : snap) {
                String nick = doc.getString("nickname");
                Long best = null;
                Object o = doc.get("bestTime");
                if (o instanceof Long) best = (Long)o;
                else if (o instanceof Integer) best = ((Integer)o).longValue();

                if (nick == null) nick = "(unknown)";
                if (best == null) continue;

                entries.add(new Entry(rank, nick, best));

                if (currentNick != null && !currentNick.isEmpty() && nick.equals(currentNick)) {
                    currentRank = rank;
                    currentTime = best;
                }

                rank++;
            }

            adapter.setEntries(entries);
            if (currentRank > 0) {
                tvPlayerInfo.setText(String.format(Locale.US, "Your rank: %d — %s", currentRank, formatTime(currentTime)));
            } else {
                tvPlayerInfo.setText("Your rank: -");
            }
        });
    }

    private String getSavedNickname() {
        SharedPreferences prefs = getSharedPreferences("userInfo", MODE_PRIVATE);
        return prefs.getString("nickname", "");
    }

    private static String formatTime(long ms) {
        long totalSec = ms / 1000;
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }

    private static class Entry {
        final int rank;
        final String nickname;
        final long timeMs;
        Entry(int rank, String nickname, long timeMs) { this.rank = rank; this.nickname = nickname; this.timeMs = timeMs; }
    }

    private class LeaderAdapter extends RecyclerView.Adapter<LeaderAdapter.VH> {
        private final List<Entry> list = new ArrayList<>();
        void setEntries(List<Entry> entries) {
            list.clear();
            list.addAll(entries);
            notifyDataSetChanged();
        }
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leader_row, parent, false);
            TextView tvRank = row.findViewById(R.id.tv_rank);
            TextView tvName = row.findViewById(R.id.tv_name);
            TextView tvTime = row.findViewById(R.id.tv_time);
            return new VH(row, tvRank, tvName, tvTime);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Entry e = list.get(position);
            holder.rank.setText(e.rank + ".");
            holder.name.setText(e.nickname);
            holder.time.setText(formatTime(e.timeMs));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView rank, name, time;
            VH(@NonNull View itemView, TextView rank, TextView name, TextView time) {
                super(itemView);
                this.rank = rank; this.name = name; this.time = time;
            }
        }
    }
}