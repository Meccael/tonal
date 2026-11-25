package com.meccael.tonal.bottom_menu;

import android.graphics.drawable.Drawable;

/** One row of a bottom sheet menu. */
class Item {
    private Drawable icon;
    private int id;
    private String title;

    Item() {
    }

    public Drawable getIcon() { return icon; }
    public void setIcon(Drawable icon) { this.icon = icon; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
