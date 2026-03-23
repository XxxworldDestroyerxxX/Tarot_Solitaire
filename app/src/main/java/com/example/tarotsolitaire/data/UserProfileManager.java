package com.example.tarotsolitaire.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface VoidCallback {
        void onComplete(boolean success, String message);
    }

    public interface LeaderboardCallback {
        void onComplete(boolean success, List<UserScore> results, String message);
    }

    public static class UserScore {
        public final String uid;
        public final String nickname;
        public final Long bestTime;

        public UserScore(String uid, String nickname, Long bestTime) {
            this.uid = uid;
            this.nickname = nickname;
            this.bestTime = bestTime;
        }
    }

    public void ensureProfileExists(@NonNull String uid, String email, String nickname, @NonNull VoidCallback cb) {
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cb.onComplete(true, "Profile exists");
                        return;
                    }

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("nickname", nickname != null ? nickname : "");
                    userMap.put("email", email != null ? email : "");
                    userMap.put("createdAt", FieldValue.serverTimestamp());
                    userMap.put("bestTime", null);
                    userMap.put("times", new ArrayList<Long>());

                    docRef.set(userMap)
                            .addOnSuccessListener(aVoid -> cb.onComplete(true, "Profile created"))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create profile", e);
                                cb.onComplete(false, e.getClass().getSimpleName() + ": " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to read profile", e);
                    cb.onComplete(false, e.getClass().getSimpleName() + ": " + e.getMessage());
                });
    }


    public void addCompletionTime(@NonNull String uid, long timeMillis, @NonNull VoidCallback cb) {
        DocumentReference docRef = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            if (!snap.exists()) {
                // If no doc exists, create a minimal one first (bestTime = time, times = [time])
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("nickname", "");
                userMap.put("email", "");
                userMap.put("createdAt", FieldValue.serverTimestamp());
                userMap.put("bestTime", timeMillis);
                List<Long> lst = new ArrayList<>();
                lst.add(timeMillis);
                userMap.put("times", lst);
                transaction.set(docRef, userMap);
                return null;
            } else {
                // Append to times array
                transaction.update(docRef, "times", FieldValue.arrayUnion(timeMillis));

                Object bestObj = snap.get("bestTime");
                if (bestObj == null) {
                    transaction.update(docRef, "bestTime", timeMillis);
                } else if (bestObj instanceof Number) {
                    long currentBest = ((Number) bestObj).longValue();
                    if (timeMillis < currentBest) {
                        transaction.update(docRef, "bestTime", timeMillis);
                    }
                }
                return null;
            }
        }).addOnSuccessListener(aVoid -> cb.onComplete(true, "Recorded time"))
          .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to record time", e);
              cb.onComplete(false, e.getClass().getSimpleName() + ": " + e.getMessage());
          });
    }


    public void fetchLeaderboard(int limit, @NonNull LeaderboardCallback cb) {
        Query q = db.collection("users").orderBy("bestTime", Query.Direction.ASCENDING).limit(limit);
        q.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserScore> out = new ArrayList<>();
                    for (DocumentSnapshot ds : queryDocumentSnapshots) {
                        Object bestObj = ds.get("bestTime");
                        if (bestObj instanceof Number) {
                            long best = ((Number) bestObj).longValue();
                            String nick = ds.getString("nickname");
                            out.add(new UserScore(ds.getId(), nick != null ? nick : "", best));
                        }
                    }
                    cb.onComplete(true, out, "OK");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch leaderboard", e);
                    cb.onComplete(false, null, e.getClass().getSimpleName() + ": " + e.getMessage());
                });
    }
}

