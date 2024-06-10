package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {
    private static final String TAG = "NotificationService";
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private Context context;

    public NotificationService(Context context) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
    }

    public void checkAndSendNotifications() {
        try {
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DocumentReference datesRef = firestore.collection("Users").document(userId);
                datesRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Map<String, Object> existingData = (Map<String, Object>) document.get("datanames");
                                if (existingData != null) {
                                    Map<LocalDate, List<String>> eventsByDate = new HashMap<>();

                                    for (Map.Entry<String, Object> entry : existingData.entrySet()) {
                                        String dateString = (String) entry.getValue();
                                        LocalDate date = LocalDate.parse(dateString);
                                        if (!eventsByDate.containsKey(date)) {
                                            eventsByDate.put(date, new ArrayList<>());
                                        }
                                        eventsByDate.get(date).add(entry.getKey());
                                    }
                                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                                    if (eventsByDate.containsKey(tomorrow)) {
                                        List<String> eventNames = eventsByDate.get(tomorrow);
                                        NotificationHelper.showExpandableNotification(context, "События на завтра!", eventNames);
                                    } else {
                                        NotificationHelper.cancelNotification(context);
                                    }
                                } else {
                                    NotificationHelper.cancelNotification(context);
                                }
                            } else {
                                NotificationHelper.cancelNotification(context);
                            }
                        } else {
                            Log.e(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAndSendNotifications: ", e);
        }
    }
}
