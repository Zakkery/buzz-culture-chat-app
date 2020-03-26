package edu.gatech.edtech.culturechatapp.ui.session;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;
import edu.gatech.edtech.culturechatapp.ui.mentor.MentorAdapter;
import edu.gatech.edtech.culturechatapp.ui.module.ModuleAdapter;
import edu.gatech.edtech.culturechatapp.ui.student.StudentAdapter;
import edu.gatech.edtech.culturechatapp.ui.student.StudentFragmentDirections;

public class SessionFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_sessions, container, false);

        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        // we are in session fragment - check if we are a student and enable add button
        if (ApplicationSetup.userRole.contentEquals("student")) {
            fabAdd.show();
            fabAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ServerRequestHandler()
                            .setMethod(Request.Method.GET)
                            .setActivity(getActivity())
                            .setLayout(R.id.drawer_layout)
                            .setEndpoint("/modules")
                            .setAuthHeader(ApplicationSetup.userToken)
                            .setListenerJSONArray(response -> {
                                ArrayList<ModuleAdapter.ModuleListInfo> availableModules = new ArrayList<>();
                                try {
                                    if (response.length() > 0) {
                                        for (int i = 0; i < response.length(); i++) {
                                            JSONObject moduleData = response.getJSONObject(i);
                                            String moduleId = moduleData.getString("_id");
                                            String topic = moduleData.getString("full_name");
                                            availableModules.add(new ModuleAdapter.ModuleListInfo(moduleId, topic));
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                availableModules.add(new ModuleAdapter.ModuleListInfo("other", "Other"));
                                // now that we have all of those we need to get student's mentor
                                new ServerRequestHandler()
                                        .setMethod(Request.Method.GET)
                                        .setActivity(getActivity())
                                        .setLayout(R.id.drawer_layout)
                                        .setEndpoint("/student/mentor")
                                        .setAuthHeader(ApplicationSetup.userToken)
                                        .setListenerJSONObject(resp -> {
                                            try {
                                                if (resp.has("_id")) {
                                                    String mentorId = resp.getString("_id");
                                                    String mentorName = resp.getString("name");
                                                    // now we can open up a form
                                                    SessionFragmentDirections.SessionsToSessionAddForm action = SessionFragmentDirections.sessionsToSessionAddForm(availableModules, mentorName, mentorId);
                                                    Navigation.findNavController(root).navigate(action);
                                                } else {
                                                    Snackbar.make(view, "No mentor assigned. Please ask administration to assign you a mentor", Snackbar.LENGTH_LONG)
                                                            .show();
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        })
                                        .executeRequest();
                            })
                            .executeRequest();
                }
            });
        } else {
            fabAdd.hide();
        }

        // always hide save button here
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.hide();

        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view_sessions);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        //String[] myDataset = new String[] {"Test", "totally"};
        final Activity appActivity = getActivity();

        // request all the student information
        new ServerRequestHandler()
                .setMethod(Request.Method.GET)
                .setActivity(getActivity())
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/student/sessions")
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONArray(response -> {
                    List<SessionAdapter.SessionListInfo> sessionDatasetPrevious = new ArrayList<>();
                    List<SessionAdapter.SessionListInfo> sessionDatasetUpcoming = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject sessionObject = response.getJSONObject(i);
                            String objectId = sessionObject.getString("_id");
                            // get student object
                            JSONObject studentObject = sessionObject.getJSONObject("student");
                            String studentName = studentObject.getString("name");
                            // get mentor object
                            JSONObject mentorObject = sessionObject.getJSONObject("mentor");
                            String mentorName = mentorObject.getString("name");

                            // get start date
                            String startDateString = sessionObject.getString("starts_at");
                            Date startDate = ApplicationSetup.dateFromMongoString(startDateString);

                            // get end date if exists
                            Date endDate = null;
                            if (sessionObject.has("ends_at")) {
                                String endDateString = sessionObject.getString("ends_at");
                                endDate = ApplicationSetup.dateFromMongoString(endDateString);
                            }

                            String topic = sessionObject.getString("topic");
                            boolean approved = sessionObject.getBoolean("approved");
                            if (endDate == null) {
                                sessionDatasetUpcoming.add(new SessionAdapter.SessionListInfo(objectId, studentName, mentorName, topic, startDate, endDate, approved));
                            } else {
                                sessionDatasetPrevious.add(new SessionAdapter.SessionListInfo(objectId, studentName, mentorName, topic, startDate, endDate, approved));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // sort based on start date
                    sessionDatasetUpcoming.sort((p1, p2) -> p1.starts_at.compareTo(p2.starts_at));
                    sessionDatasetPrevious.sort((p1, p2) -> p1.starts_at.compareTo(p2.starts_at));

                    if (sessionDatasetPrevious.size() > 0) {
                        sessionDatasetUpcoming.add(new SessionAdapter.SessionListInfo("previous_sessions", null, null, "Previous Sessions", null, null, true));
                        sessionDatasetUpcoming.addAll(sessionDatasetPrevious);
                    }

                    mAdapter = new SessionAdapter(sessionDatasetUpcoming, appActivity);
                    recyclerView.setAdapter(mAdapter);
                })
                .executeRequest();

        return root;
    }
}
