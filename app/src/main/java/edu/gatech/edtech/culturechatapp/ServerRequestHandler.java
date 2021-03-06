package edu.gatech.edtech.culturechatapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class ServerRequestHandler {
    private static String serverURL = "https://acculturation-chat.herokuapp.com";
    private int method = 0;
    private Activity context = null;
    private int layout = 0;
    private String endpoint = "";
    private String authToken = "";
    private JSONObject data = null;
    private Response.Listener<JSONObject> listenerWrap = null;
    private Response.ErrorListener errorListener = null;

    private String constructURL(String endpoint) {
        return ServerRequestHandler.serverURL + endpoint;
    }

    public ServerRequestHandler setMethod(int method) {
        this.method = method;
        return this;
    }

    public ServerRequestHandler setActivity(Activity context) {
        this.context = context;
        return this;
    }

    public ServerRequestHandler setLayout(int layoutId) {
        this.layout = layoutId;
        return this;
    }

    public ServerRequestHandler setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ServerRequestHandler setJSONData(JSONObject data) {
        this.data = data;
        return this;
    }

    public ServerRequestHandler setAuthHeader(String token) {
        this.authToken = token;
        return this;
    }

    public ServerRequestHandler setListenerJSONObject(final Response.Listener<JSONObject> listener) {
        final int layoutID = this.layout;
        final Activity appContext = this.context;
        this.listenerWrap = response -> {
            try {
                if (!response.getBoolean("success")) {
                    // get message text
                    String messageText = response.getString("message");
                    Snackbar errorMessageSnackbar = null;
                    if (messageText.equals("Unauthorized")) {
                        logoutUser();
                        errorMessageSnackbar = Snackbar.make(
                                appContext.findViewById(layoutID),
                                "Please login again", Snackbar.LENGTH_LONG
                        );
                    } else {
                        // Error has occured - display it in the snackbar
                        errorMessageSnackbar = Snackbar.make(
                                appContext.findViewById(layoutID),
                                messageText, Snackbar.LENGTH_LONG
                        );
                    }
                    InputMethodManager imm = (InputMethodManager) appContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    View view = appContext.getCurrentFocus();

                    if (view == null) {
                        view = new View(appContext);
                    }

                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    errorMessageSnackbar.show();
                } else {
                    JSONObject data = response.getJSONObject("data");
                    listener.onResponse(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        return this;
    }

    public void logoutUser() {
        //on the logout - switch to the login page and delete all info from app
        SharedPreferences pref = this.context.getSharedPreferences("login_pref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("role");
        editor.remove("token");
        editor.remove("name");
        editor.remove("email");
        editor.apply();
        ApplicationSetup.userRole = null;
        ApplicationSetup.userToken = null;
        ApplicationSetup.userEmail = null;
        ApplicationSetup.userName = null;

        Intent intent = new Intent(this.context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        this.context.finish();
        this.context.startActivity(intent);
    }

    public ServerRequestHandler setListenerJSONArray(final Response.Listener<JSONArray> listener) {
        final int layoutID = this.layout;
        final Activity appContext = this.context;
        this.listenerWrap = response -> {
            try {
                if (!response.getBoolean("success")) {
                    // get message text
                    String messageText = response.getString("message");
                    Snackbar errorMessageSnackbar = null;
                    if (messageText.equals("Unauthorized")) {
                        logoutUser();
                        errorMessageSnackbar = Snackbar.make(
                                appContext.findViewById(layoutID),
                                "Please login again", Snackbar.LENGTH_LONG
                        );
                    } else {
                        // Error has occured - display it in the snackbar
                        errorMessageSnackbar = Snackbar.make(
                                appContext.findViewById(layoutID),
                                messageText, Snackbar.LENGTH_LONG
                        );
                    }
                    InputMethodManager imm = (InputMethodManager) appContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    View view = appContext.getCurrentFocus();

                    if (view == null) {
                        view = new View(appContext);
                    }

                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    errorMessageSnackbar.show();
                } else {
                    JSONArray data = response.getJSONArray("data");
                    listener.onResponse(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        return this;
    }


    public ServerRequestHandler() {
        this.errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if (error instanceof ServerError && response != null) {
                    try {
                        String res = new String(response.data,
                                HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                        // Now you can use any deserializer to make sense of data
                        JSONObject obj = new JSONObject(res);
                        System.out.println(res);
                    } catch (UnsupportedEncodingException | JSONException e1) {
                        // Couldn't properly decode data to string
                        e1.printStackTrace();
                    }
                }
            }
        };
    }

    public void executeRequest() {
        JsonObjectRequest actualRequestObject = null;
        // we need to set up a header
        if (!this.authToken.equals("")) {
            final String authHeader = this.authToken;
            actualRequestObject =  new JsonObjectRequest(this.method, this.constructURL(this.endpoint), this.data, this.listenerWrap, this.errorListener){
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String>  params = new HashMap<>();
                    params.put("authorization", authHeader);
                    return params;
                }
            };
        } else {
            // just a simple request
            actualRequestObject =  new JsonObjectRequest(this.method, this.constructURL(this.endpoint), this.data, this.listenerWrap, this.errorListener);
        }

        RequestQueueSingleton.getInstance(this.context).addToRequestQueue(actualRequestObject);
    }
}
