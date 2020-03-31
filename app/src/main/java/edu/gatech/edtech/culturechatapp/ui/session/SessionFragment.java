package edu.gatech.edtech.culturechatapp.ui.session;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;
import edu.gatech.edtech.culturechatapp.ui.module.ModuleAvailableAdapter;

public class SessionFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private View root;
    private Activity appActivity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.root = inflater.inflate(R.layout.fragment_sessions, container, false);

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
                                ArrayList<ModuleAvailableAdapter.ModuleListInfo> availableModules = new ArrayList<>();
                                try {
                                    if (response.length() > 0) {
                                        for (int i = 0; i < response.length(); i++) {
                                            JSONObject moduleData = response.getJSONObject(i);
                                            String moduleId = moduleData.getString("_id");
                                            String topic = moduleData.getString("full_name");
                                            availableModules.add(new ModuleAvailableAdapter.ModuleListInfo(moduleId, topic));
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                availableModules.add(new ModuleAvailableAdapter.ModuleListInfo("other", "Other"));
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

        this.appActivity = getActivity();

        // request all the student information
        new ServerRequestHandler()
                .setMethod(Request.Method.GET)
                .setActivity(getActivity())
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/student/sessions")
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONArray(response -> {
                    List<SessionAdapter.SessionListInfo> previousSessions = new ArrayList<>();
                    List<SessionAdapter.SessionListInfo> upcomingSessions = new ArrayList<>();
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
                                upcomingSessions.add(new SessionAdapter.SessionListInfo(objectId, studentName, mentorName, topic, startDate, endDate, approved));
                            } else {
                                previousSessions.add(new SessionAdapter.SessionListInfo(objectId, studentName, mentorName, topic, startDate, endDate, approved));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // sort based on start date
                    upcomingSessions.sort((p1, p2) -> p1.starts_at.compareTo(p2.starts_at));
                    previousSessions.sort((p1, p2) -> p1.starts_at.compareTo(p2.starts_at));
                    this.populatePreviousSessions(previousSessions);
                    this.populateUpcomingSessions(upcomingSessions);
                })
                .executeRequest();

        return root;
    }

    private void populatePreviousSessions(List<SessionAdapter.SessionListInfo> previousSessions) {
        ConstraintLayout constraintPreviousSessionsLayout = getActivity().findViewById(R.id.session_previous_layout);
        if (previousSessions.size() < 1) {
            constraintPreviousSessionsLayout.setVisibility(View.GONE);
        } else {
            constraintPreviousSessionsLayout.setVisibility(View.VISIBLE);
            RecyclerView availableModulesRecyclerView = this.root.findViewById(R.id.recycler_previous_sessions);
            availableModulesRecyclerView.setHasFixedSize(true);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.appActivity);
            availableModulesRecyclerView.setLayoutManager(layoutManager);

            RecyclerView.Adapter mAdapter = new SessionAdapter(previousSessions, this.appActivity);
            availableModulesRecyclerView.setAdapter(mAdapter);
        }
    }

    private void populateUpcomingSessions(List<SessionAdapter.SessionListInfo> upcomingSessions) {
        ConstraintLayout constraintUpcomingSessionsLayout = getActivity().findViewById(R.id.sessions_upcoming_layout);
        if (upcomingSessions.size() < 1) {
            constraintUpcomingSessionsLayout.setVisibility(View.GONE);
        } else {
            constraintUpcomingSessionsLayout.setVisibility(View.VISIBLE);
            RecyclerView availableModulesRecyclerView = this.root.findViewById(R.id.recycler_upcoming_sessions);
            availableModulesRecyclerView.setHasFixedSize(true);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.appActivity);
            availableModulesRecyclerView.setLayoutManager(layoutManager);

            RecyclerView.Adapter mAdapter = new SessionAdapter(upcomingSessions, this.appActivity);
            availableModulesRecyclerView.setAdapter(mAdapter);
        }
    }
}
