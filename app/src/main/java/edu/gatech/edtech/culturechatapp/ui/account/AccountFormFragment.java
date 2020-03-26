package edu.gatech.edtech.culturechatapp.ui.account;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class AccountFormFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_account, container, false);

        // always hide add button here
        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        fabAdd.hide();

        // always show save button here
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);

        final TextView studentNameTextView = root.findViewById(R.id.student_name_text);
        TextView studentEmailTextView = root.findViewById(R.id.account_email_text);

        fabConfirm.show();
        fabConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                final String studentName = studentNameTextView.getText().toString();

                Map<String, String> params = new HashMap<>();
                params.put("name", studentName);
                JSONObject jsonObj = new JSONObject(params);

                new ServerRequestHandler()
                        .setActivity(getActivity())
                        .setMethod(Request.Method.PUT)
                        .setLayout(R.id.drawer_layout)
                        .setJSONData(jsonObj)
                        .setEndpoint("/student/account")
                        .setAuthHeader(ApplicationSetup.userToken)
                        .setListenerJSONObject(new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                ApplicationSetup.userName = studentName;
                                NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
                                View headerView = navigationView.getHeaderView(0);
                                TextView userNameText = headerView.findViewById(R.id.user_name_text);
                                userNameText.setText(ApplicationSetup.userName);
                                Snackbar.make(view, "Record has been updated", Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        })
                        .executeRequest();
            }
        });

        studentNameTextView.setText(ApplicationSetup.userName);
        studentEmailTextView.setText(ApplicationSetup.userEmail);

        return root;
    }
}
