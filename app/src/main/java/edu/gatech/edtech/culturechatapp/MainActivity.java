package edu.gatech.edtech.culturechatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void loginUser(View v) {
        TextView loginEmailInputTextView = this.findViewById(R.id.login_email_input);
        TextView loginPasswordInputTextView = this.findViewById(R.id.login_password_input);
        final String userEmail = loginEmailInputTextView.getText().toString();
        final String password = loginPasswordInputTextView.getText().toString();

        Map<String, String> params = new HashMap<String, String>();
        params.put("email", userEmail);
        params.put("password", password);
        JSONObject jsonObj = new JSONObject(params);

        new ServerRequestHandler()
            .setMethod(Request.Method.POST)
            .setActivity(this)
            .setLayout(R.id.login_layout)
            .setEndpoint("/login")
            .setJSONData(jsonObj)
            .setListener(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        // save token and role and switch to the appropriate interface
                        ApplicationSetup.userRole = response.getString("role");
                        ApplicationSetup.userToken = response.getString("token");
                        switch (ApplicationSetup.userRole) {
                            case "admin":
                                System.out.println("THis is admin section!");
                                /*Intent intent = new Intent(MainActivity.this, ActivityModules.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);*/
                                break;
                            case "mentor":
                                break;
                            case "student":
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            })
            .executeRequest();
    }
}
