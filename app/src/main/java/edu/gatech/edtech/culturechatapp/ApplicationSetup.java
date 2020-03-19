package edu.gatech.edtech.culturechatapp;

import android.view.Menu;

public class ApplicationSetup {
    public static String userRole = null;
    public static String userToken = null;
    public static String userName = null;

    public static void setupMenuItems(int[] visibleMenuItems, Menu menuNav) {
        int[] allMenuItems = new int[] {
                R.id.nav_students, R.id.nav_modules,
                R.id.nav_mentors, R.id.nav_account, R.id.nav_sessions
        };

        for (int item : allMenuItems) {
            menuNav.findItem(item).setEnabled(false);
            menuNav.findItem(item).setVisible(false);
        }

        for (int item : visibleMenuItems) {
            menuNav.findItem(item).setEnabled(true);
            menuNav.findItem(item).setVisible(true);
        }

    }
}
