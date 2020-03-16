package edu.gatech.edtech.culturechatapp;

import android.app.Activity;
import android.content.Context;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


public class ServerRequestHandler {
    private static String serverURL = "https://acculturation-chat.herokuapp.com";
    private int method = 0;
    private Activity context = null;
    private int layout = 0;
    private String endpoint = "";
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

    public ServerRequestHandler setListener(final Response.Listener<JSONObject> listener) {
        final int layoutID = this.layout;
        final Activity appContext = this.context;
        this.listenerWrap =  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (!response.getBoolean("success")) {
                        // get message text
                        String messageText = response.getString("message");

                        // Error has occured - display it in the snackbar
                        Snackbar errorMessageSnackbar = Snackbar.make(
                                appContext.findViewById(layoutID),
                                messageText, Snackbar.LENGTH_LONG
                        );
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

    void executeRequest() {
        JsonObjectRequest actualRequestObject =  new JsonObjectRequest(this.method, this.constructURL(this.endpoint), this.data, this.listenerWrap, this.errorListener);

        RequestQueueSingleton.getInstance(this.context).addToRequestQueue(actualRequestObject);
    }
}
