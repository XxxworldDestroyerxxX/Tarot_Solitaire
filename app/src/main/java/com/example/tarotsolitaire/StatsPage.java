package com.example.tarotsolitaire;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class StatsPage extends AppCompatActivity {

    private TextView tvGamesPlayed, tvGamesWon, tvPctWon, tvBestTime, tvTimePlayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stats_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvGamesPlayed = findViewById(R.id.tv_games_played);
        tvGamesWon = findViewById(R.id.tv_games_won);
        tvPctWon = findViewById(R.id.tv_pct_won);
        tvBestTime = findViewById(R.id.tv_best_time);
        tvTimePlayed = findViewById(R.id.tv_time_played);

        Button back = findViewById(R.id.btn_back);
        back.setOnClickListener(view -> {
            Intent intent = new Intent(StatsPage.this, MainMenu.class);
            startActivity(intent);
            finish();
        });

        loadStats();
    }

    private void loadStats() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvGamesPlayed.setText("Games Played: (sign in to save stats)");
            tvGamesWon.setText("Games Won: —");
            tvPctWon.setText("Win %: —");
            tvBestTime.setText("Best Time: —");
            tvTimePlayed.setText("Time Played: —");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                long gamesPlayed = 0L;
                long gamesWon = 0L;
                long bestTimeMs = -1L;
                long totalPlayTimeMs = 0L;

                if (doc != null) {
                    if (doc.contains("gamesPlayed")) {
                        Object o = doc.get("gamesPlayed");
                        if (o instanceof Long) gamesPlayed = (Long) o;
                        else if (o instanceof Integer) gamesPlayed = ((Integer) o).longValue();
                    }
                    if (doc.contains("gamesWon")) {
                        Object o = doc.get("gamesWon");
                        if (o instanceof Long) gamesWon = (Long) o;
                        else if (o instanceof Integer) gamesWon = ((Integer) o).longValue();
                    }
                    if (doc.contains("bestTime")) {
                        Object o = doc.get("bestTime");
                        if (o instanceof Long) bestTimeMs = (Long) o;
                        else if (o instanceof Integer) bestTimeMs = ((Integer) o).longValue();
                    }
                    if (doc.contains("totalPlayTimeMs")) {
                        Object o = doc.get("totalPlayTimeMs");
                        if (o instanceof Long) totalPlayTimeMs = (Long) o;
                        else if (o instanceof Integer) totalPlayTimeMs = ((Integer) o).longValue();
                    }
                }

                tvGamesPlayed.setText(String.format(Locale.US, "Games Played: %d", gamesPlayed));
                tvGamesWon.setText(String.format(Locale.US, "Games Won: %d", gamesWon));
                String pct = "—";
                if (gamesPlayed > 0) {
                    double p = (double) gamesWon * 100.0 / (double) gamesPlayed;
                    pct = String.format(Locale.US, "%.1f%%", p);
                }
                tvPctWon.setText(String.format(Locale.US, "Win %%: %s", pct));

                if (bestTimeMs >= 0) tvBestTime.setText(String.format(Locale.US, "Best Time: %s", formatElapsed(bestTimeMs)));
                else tvBestTime.setText("Best Time: —");

                if (totalPlayTimeMs > 0) tvTimePlayed.setText(String.format(Locale.US, "Time Played: %s", formatTimeLong(totalPlayTimeMs)));
                else tvTimePlayed.setText("Time Played: —");
            } else {
                tvGamesPlayed.setText("Games Played: (error loading)");
                tvGamesWon.setText("Games Won: (error)");
                tvPctWon.setText("Win %: (error)");
                tvBestTime.setText("Best Time: (error)");
                tvTimePlayed.setText("Time Played: (error)");
            }
        });
    }

    private static String formatElapsed(long ms) {
        long totalSec = ms / 1000;
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }

    private static String formatTimeLong(long ms) {
        long totalSec = ms / 1000;
        long hours = totalSec / 3600;
        long mins = (totalSec % 3600) / 60;
        long secs = totalSec % 60;
        if (hours > 0) return String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs);
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }
}