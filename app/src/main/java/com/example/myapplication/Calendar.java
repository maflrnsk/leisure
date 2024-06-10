package com.example.myapplication;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Calendar extends Fragment implements CalendarAdapter.OnItemListener {
    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;
    private View view;
    private ArrayList<LocalDate> markedDates;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private static final String TAG = "CalendarFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_calendar, container, false);
        initWidgets(view);
        firestore = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        selectedDate = LocalDate.now();
        markedDates = new ArrayList<>();
        loadMarkedDatesFromFirestore();

        Button previousMonthButton = view.findViewById(R.id.previousMonthAction);
        Button nextMonthButton = view.findViewById(R.id.nextMonthAction);
        Button clear = view.findViewById(R.id.clear);
        previousMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousMonthAction(v);
            }
        });
        nextMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextMonthAction(v);
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearConfirmationDialog();
            }
        });
        return view;
    }

    private void initWidgets(View view) {
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView);
        monthYearText = view.findViewById(R.id.monthYearTV);
    }

    private void setMonthView() {
        monthYearText.setText(monthYearFromDate(selectedDate));
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);

        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this, selectedDate, markedDates);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
    }

    private ArrayList<String> daysInMonthArray(LocalDate date) {
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);

        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate firstOfMonth = selectedDate.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;

        for (int i = 1; i <= 42; i++) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("");
            } else {
                daysInMonthArray.add(String.valueOf(i - dayOfWeek));
            }
        }
        return daysInMonthArray;
    }

    private String monthYearFromDate(LocalDate date) {
        String[] monthNamesNominative = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        String month = monthNamesNominative[date.getMonthValue() - 1];
        int year = date.getYear();
        return month + " " + year;
    }

    public void previousMonthAction(View view) {
        selectedDate = selectedDate.minusMonths(1);
        setMonthView();
    }

    public void nextMonthAction(View view) {
        selectedDate = selectedDate.plusMonths(1);
        setMonthView();
    }

    @Override
    public void onItemClick(int position, String dayText) {
        if (!dayText.equals("")) {
            LocalDate selectedDay = selectedDate.withDayOfMonth(Integer.parseInt(dayText));
            showEventDialog(selectedDay);
        }
    }

    private void getEventNamesForDate(LocalDate date, final EventNamesCallback callback) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            firestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        ArrayList<String> eventNames = new ArrayList<>();
                        if (document.exists()) {
                            Map<String, Object> existingData = (Map<String, Object>) document.get("datanames");
                            if (existingData != null) {
                                for (Map.Entry<String, Object> entry : existingData.entrySet()) {
                                    String dateString = (String) entry.getValue();
                                    LocalDate eventDate = LocalDate.parse(dateString);
                                    if (eventDate.equals(date)) {
                                        eventNames.add(entry.getKey());
                                    }
                                }
                            }
                        }
                        callback.onCallback(eventNames);
                    }
                }
            });
        }
    }

    private interface EventNamesCallback {
        void onCallback(ArrayList<String> eventNames);
    }

    private void showClearConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Подтверждение удаления");
        builder.setMessage("Вы уверены, что хотите удалить все события?");

        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearAllEvents();
            }
        });

        builder.setNegativeButton("Нет", null);
        builder.show();
    }

    private void showEventDialog(LocalDate date) {
        getEventNamesForDate(date, new EventNamesCallback() {
            @Override
            public void onCallback(ArrayList<String> eventNames) {
                if (eventNames.isEmpty()) {
                    Toast.makeText(getContext(), "В эту дату нет событий", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder events = new StringBuilder();
                for (String eventName : eventNames) {
                    events.append(eventName).append("\n");
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("События " + date.toString());
                builder.setMessage(events.toString());

                builder.setNegativeButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteEvent(date);
                    }
                });

                builder.setNeutralButton("Закрыть", null);
                builder.show();
            }
        });
    }

    private void loadMarkedDatesFromFirestore() {
        try {
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DocumentReference datesRef = firestore.collection("Users").document(userId);
                datesRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            markedDates = new ArrayList<>();
                            if (document.exists()) {
                                Map<String, Object> existingData = (Map<String, Object>) document.get("datanames");
                                if (existingData != null) {
                                    Map<LocalDate, List<String>> eventsByDate = new HashMap<>();

                                    for (Map.Entry<String, Object> entry : existingData.entrySet()) {
                                        String dateString = (String) entry.getValue();
                                        LocalDate date = LocalDate.parse(dateString);
                                        markedDates.add(date);
                                        if (!eventsByDate.containsKey(date)) {
                                            eventsByDate.put(date, new ArrayList<>());
                                        }
                                        eventsByDate.get(date).add(entry.getKey());
                                    }
                                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                                    if (eventsByDate.containsKey(tomorrow)) {
                                        List<String> eventNames = eventsByDate.get(tomorrow);
                                        NotificationHelper.showExpandableNotification(getContext(), "События на завтра!", eventNames);
                                    } else {
                                        NotificationHelper.cancelNotification(getContext());
                                    }
                                } else {
                                    NotificationHelper.cancelNotification(getContext());
                                }
                            } else {
                                NotificationHelper.cancelNotification(getContext());
                            }
                            setMonthView();
                        } else {
                            Log.e(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMarkedDatesFromFirestore: ", e);
        }
    }
    private void deleteEvent(LocalDate date) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDocRef = firestore.collection("Users").document(userId);

            userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> existingData = (Map<String, Object>) document.get("datanames");
                            if (existingData != null) {
                                existingData.entrySet().removeIf(entry -> LocalDate.parse((String) entry.getValue()).equals(date));
                                userDocRef.update("datanames", existingData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getContext(), "Выбранные события удалены", Toast.LENGTH_SHORT).show();
                                            markedDates.remove(date);
                                            setMonthView();
                                            loadMarkedDatesFromFirestore();
                                            NotificationService notificationService = new NotificationService(getContext());
                                            notificationService.checkAndSendNotifications();
                                        } else {
                                            Toast.makeText(getContext(), "Ошибка при удалении событий", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    private void clearAllEvents() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDocRef = firestore.collection("Users").document(userId);

            userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            userDocRef.update("datanames", new HashMap<>()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Все события удалены", Toast.LENGTH_SHORT).show();
                                        markedDates.clear();
                                        setMonthView();
                                        NotificationService notificationService = new NotificationService(getContext());
                                        notificationService.checkAndSendNotifications();
                                    } else {
                                        Toast.makeText(getContext(), "Ошибка при удалении событий", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

}
