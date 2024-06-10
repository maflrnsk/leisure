package com.example.myapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MyAdapter extends FragmentStateAdapter {
    private MainScreen mainScreen;
    private Profile profile;
    private Calendar calendar;
    private Admin admin;
    private String userRole;

    public MyAdapter(@NonNull FragmentActivity fragmentActivity, String userRole) {
        super(fragmentActivity);
        this.userRole = userRole;
        mainScreen = new MainScreen();
        profile = new Profile();
        calendar = new Calendar();
        admin = new Admin();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if ("Admin".equals(userRole)) {
            switch (position) {
                case 0:
                    return admin;
                case 1:
                    return profile;
                default:
                    return null;
            }
        } else {
            switch (position) {
                case 0:
                    return mainScreen;
                case 1:
                    return calendar;
                case 2:
                    return profile;
                default:
                    return null;
            }
        }
    }

    @Override
    public int getItemCount() {
        if ("Admin".equals(userRole)) {
            return 2;
        }
        return 3;
    }

    public void updateCalendarData(Bundle data) {
        calendar.setArguments(data);
        notifyDataSetChanged();
    }
}


