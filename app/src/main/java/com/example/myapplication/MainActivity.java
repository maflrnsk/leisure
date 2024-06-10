package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.myapplication.databinding.ActivityMainBinding;

import java.time.LocalDate;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#a5c089")));

        userRole = getIntent().getStringExtra("userRole");

        ArrayList<LocalDate> dates = (ArrayList<LocalDate>) getIntent().getSerializableExtra("dates");

        FragmentStateAdapter pageAdapter = new MyAdapter(this, userRole);
        ((MyAdapter) pageAdapter).updateCalendarData(createBundleForDates(dates));
        binding.pager.setAdapter(pageAdapter);

        String[] tabs;
        if ("Admin".equals(userRole)) {
            tabs = new String[]{"События", "Профиль"};
        } else {
            tabs = new String[]{"События", "Календарь", "Профиль"};
        }

        TabLayoutMediator tlm = new TabLayoutMediator(binding.tabLayout, binding.pager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(tabs[position]);
            }
        });
        tlm.attach();

        NotificationService notificationService = new NotificationService(this);
        notificationService.checkAndSendNotifications();
    }

    private Bundle createBundleForDates(ArrayList<LocalDate> dates) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("dates", dates);
        return bundle;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent info = new Intent(this, About.class);
        if (item.getItemId() == R.id.item1) {
            info.putExtra("keyabout", "Запущенная программа разработана в мае 2024 года\n" +
                    "как курсовой проект на языке Java. Разработка была основана на\n" +
                    "знаниях, полученных в течение курса 'Разработка мобильных приложений'.\n" +
                    "Название программы - 'Приложение для организации досуга'.");
            startActivity(info);
        } else if (item.getItemId() == R.id.item2) {
            userRole = getIntent().getStringExtra("userRole");
            if ("Admin".equals(userRole)) {
                info.putExtra("keyabout", "Инструкция по пользованию приложением:\n" +
                        "1. Вход в приложение, ознакомление с общей информацией в доп. вкладках.\n" +
                        "2. Переход на страницу 'События'.\n" +
                        "3. Добавление новых занятий в список.\n" +
                        "4. Удаление неактуальных занятий из списка.\n" +
                        "5. Изменение профиля.\n");
            } else {
                info.putExtra("keyabout", "Инструкция по пользованию приложением:\n" +
                        "1. Вход в приложение, ознакомление с общей информацией в доп. вкладках.\n" +
                        "2. Переход на страницу 'События'.\n" +
                        "3. Выбор интересующих занятий из списка.\n" +
                        "4. Занесение занятий в календарь.\n" +
                        "5. Получение уведомления о будущих событиях.\n" +
                        "6. Удаление событий из календаря.\n" +
                        "7. Изменение профиля.\n");
            }
            startActivity(info);
        } else if (item.getItemId() == R.id.item3) {
            Intent author = new Intent(this, Author.class);
            startActivity(author);
        }
        return super.onOptionsItemSelected(item);
    }
}



