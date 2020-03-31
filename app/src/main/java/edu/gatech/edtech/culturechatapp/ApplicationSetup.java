package edu.gatech.edtech.culturechatapp;

import android.view.Menu;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ApplicationSetup {
    public static String userRole = null;
    public static String userToken = null;
    public static String userName = null;
    public static String userEmail = null;
    public static String userId = null;

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

    public static Date dateFromMongoString(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = null;
        try {
            date = formatter.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String stringFromDateChat(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        return formatter.format(date);
    }
}
