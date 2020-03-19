package edu.gatech.edtech.culturechatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.webkit.WebViewFragment;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import edu.gatech.edtech.culturechatapp.ui.mentor.MentorFragment;

public class LoggedInActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        //if it is an admin - we show modules, student and mentors
        if (ApplicationSetup.userRole.contentEquals("admin")) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_modules, R.id.nav_students, R.id.nav_mentors)
                    .setDrawerLayout(drawer)
                    .build();

            ApplicationSetup.setupMenuItems(new int[]{R.id.nav_mentors, R.id.nav_modules, R.id.nav_students}, navigationView.getMenu());
        } else {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_modules, R.id.nav_sessions, R.id.nav_account)
                    .setDrawerLayout(drawer)
                    .build();
            ApplicationSetup.setupMenuItems(new int[]{R.id.nav_modules, R.id.nav_sessions, R.id.nav_account}, navigationView.getMenu());
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (ApplicationSetup.userName != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView userNameText = headerView.findViewById(R.id.user_name_text);
            userNameText.setText(ApplicationSetup.userName);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void logoutUser(View logoutButton) {
        //on the logout - switch to the login page and delete all info from app
        SharedPreferences pref = getApplicationContext().getSharedPreferences("login_pref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("role");
        editor.remove("token");
        editor.apply();
        ApplicationSetup.userRole = null;
        ApplicationSetup.userToken = null;

        Intent intent = new Intent(LoggedInActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
        startActivity(intent);
    }
}
