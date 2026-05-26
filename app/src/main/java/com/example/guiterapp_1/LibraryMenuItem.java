package com.example.guiterapp_1;

import androidx.fragment.app.Fragment;

public class LibraryMenuItem {
    private final String title;
    private final int iconResId;
    private final Fragment fragment;

    public LibraryMenuItem(String title, int iconResId, Fragment fragment) {
        this.title = title;
        this.iconResId = iconResId;
        this.fragment = fragment;
    }

    public String getTitle() { return title; }
    public int getIconResId() { return iconResId; }
    public Fragment getFragment() { return fragment; }
}
