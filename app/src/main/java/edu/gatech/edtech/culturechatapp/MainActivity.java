package edu.gatech.edtech.culturechatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("login_pref", 0);

        String userRole = pref.getString("role", null);
        String userToken = pref.getString("token", null);
        String userName = pref.getString("name", null);
        String userEmail = pref.getString("email", null);
        String userId = pref.getString("user_id", null);

        if (userRole != null && userToken != null) {
            ApplicationSetup.userRole = userRole;
            ApplicationSetup.userToken = userToken;
            ApplicationSetup.userName = userName;
            ApplicationSetup.userEmail = userEmail;
            ApplicationSetup.userId = userId;

            Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);
    }

    public void loginUser(View v) {
        TextView loginEmailInputTextView = this.findViewById(R.id.login_email_input);
        TextView loginPasswordInputTextView = this.findViewById(R.id.login_password_input);
        final String userEmail = loginEmailInputTextView.getText().toString();
        final String password = loginPasswordInputTextView.getText().toString();

        Map<String, String> params = new HashMap<>();
        params.put("email", userEmail);
        params.put("password", password);
        JSONObject jsonObj = new JSONObject(params);

        new ServerRequestHandler()
            .setMethod(Request.Method.POST)
            .setActivity(this)
            .setLayout(R.id.login_layout)
            .setEndpoint("/login")
            .setJSONData(jsonObj)
            .setListenerJSONObject(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        // save token and role and switch to the appropriate interface
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("login_pref", 0);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("role", response.getString("role"));
                        editor.putString("token", response.getString("token"));
                        editor.putString("name", response.getString("name"));
                        editor.putString("user_id", response.getString("id"));
                        editor.putString("email", userEmail);
                        editor.apply();
                        ApplicationSetup.userRole = response.getString("role");
                        ApplicationSetup.userToken = response.getString("token");
                        ApplicationSetup.userName = response.getString("name");
                        ApplicationSetup.userId = response.getString("id");
                        ApplicationSetup.userEmail = userEmail;
                        Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        finish();
                        startActivity(intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            })
            .executeRequest();
    }

    public void sendResetPasswordEmailLoggedIn(final View view) {
        String userEmail = null;
        TextView userEmailTextView = this.findViewById(R.id.login_email_input);

        userEmail = userEmailTextView.getText().toString();

        if (userEmail.equals("")) {
            userEmailTextView.setError("Enter Email");
        }

        Map<String, String> params = new HashMap<>();
        params.put("email", userEmail);
        JSONObject jsonObj = new JSONObject(params);

        new ServerRequestHandler()
                .setMethod(Request.Method.POST)
                .setActivity(this)
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/reset-password")
                .setJSONData(jsonObj)
                .setListenerJSONObject(new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String responseMessage = response.getString("message");
                            Snackbar.make(view, responseMessage, Snackbar.LENGTH_LONG)
                                    .show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .executeRequest();
    }
}
