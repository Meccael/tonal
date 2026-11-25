package com.meccael.tonal.navigation;

/** One entry in the navigation drawer: a label and an icon. */
class NavigationItem {
    private int icon;
    private String title;

    NavigationItem() {
    }

    public int getIcon() { return icon; }
    public void setIcon(int icon) { this.icon = icon; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
