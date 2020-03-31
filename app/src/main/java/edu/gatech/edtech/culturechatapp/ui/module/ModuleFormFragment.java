package edu.gatech.edtech.culturechatapp.ui.module;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class ModuleFormFragment extends Fragment {
    String shortTitle = "";
    String fullTitle = "";
    String description = "";
    String moduleId = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.module_form, container, false);

        if (getArguments() != null) {
            this.shortTitle = ModuleFormFragmentArgs.fromBundle(getArguments()).getShortTitle();
            this.fullTitle = ModuleFormFragmentArgs.fromBundle(getArguments()).getLongTitle();
            this.description = ModuleFormFragmentArgs.fromBundle(getArguments()).getDescription();
            this.moduleId = ModuleFormFragmentArgs.fromBundle(getArguments()).getId();
        }
        if (moduleId.equals("new_module")) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("New Module");
        } else {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(this.fullTitle);
        }


        final TextView shortTitleTextView = root.findViewById(R.id.module_form_shortname_text);
        final TextView fullTitleTextView = root.findViewById(R.id.module_form_fullname_text);
        final TextView descriptionTextView = root.findViewById(R.id.module_form_description_text);

        if (!this.shortTitle.equals("")) {
            shortTitleTextView.setText(this.shortTitle);
        }

        if (!this.fullTitle.equals("")) {
            fullTitleTextView.setText(this.fullTitle);
        }

        if (!this.description.equals("")) {
            descriptionTextView.setText(this.description);
        }

        //hide add button - doesn't matter if admin
        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        fabAdd.hide();

        shortTitleTextView.setEnabled(false);
        fullTitleTextView.setEnabled(false);
        descriptionTextView.setEnabled(false);

        // if admin - enable save button
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        if (ApplicationSetup.userRole.contentEquals("admin")) {
            shortTitleTextView.setEnabled(true);
            fullTitleTextView.setEnabled(true);
            descriptionTextView.setEnabled(true);

            fabConfirm.show();
            fabConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {

                    String shortTitle = shortTitleTextView.getText().toString();
                    String longTitle = fullTitleTextView.getText().toString();
                    String description = descriptionTextView.getText().toString();

                    Map<String, String> params = new HashMap<>();
                    params.put("short_name", shortTitle);
                    params.put("full_name", longTitle);
                    params.put("description", description);

                    JSONObject jsonObj = new JSONObject(params);

                    //is it a new object or update?
                    ServerRequestHandler request = new ServerRequestHandler()
                        .setActivity(getActivity())
                        .setLayout(R.id.drawer_layout)
                        .setJSONData(jsonObj)
                        .setEndpoint("/module/" + moduleId)
                        .setAuthHeader(ApplicationSetup.userToken)
                        .setListenerJSONObject(new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Snackbar.make(view, "Module has been updated", Snackbar.LENGTH_LONG)
                                        .show();
                                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(longTitle);
                            }
                        });
                    //new object
                    if (moduleId.equals("new_module")) {
                        request.setMethod(Request.Method.POST)
                                .setEndpoint("/admin/modules");
                    } else {
                        request.setMethod(Request.Method.PUT)
                                .setEndpoint("/admin/module/"+moduleId);
                    }
                    request.executeRequest();
                }
            });
        } else {
            fabConfirm.hide();
            // on view update viewed modules
            new ServerRequestHandler()
                    .setActivity(getActivity())
                    .setMethod(Request.Method.PUT)
                    .setLayout(R.id.drawer_layout)
                    .setEndpoint("/student/module/read/" + moduleId)
                    .setAuthHeader(ApplicationSetup.userToken)
                    .setListenerJSONObject(new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                        }
                    })
                    .executeRequest();
        }

        return root;
    }
}
