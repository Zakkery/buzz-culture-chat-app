package edu.gatech.edtech.culturechatapp.ui.mentor;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;
import edu.gatech.edtech.culturechatapp.ui.student.StudentAdapter;
import edu.gatech.edtech.culturechatapp.ui.student.StudentFragmentDirections;

public class MentorFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_students, container, false);

        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        // we are in modules fragment - check if we are an admin and enable add button
        if (ApplicationSetup.userRole.contentEquals("admin")) {
            fabAdd.show();
            fabAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<MentorAdapter.MentorListInfo> possibleMentorList = new ArrayList<>();
                    possibleMentorList.add(new MentorAdapter.MentorListInfo("no_mentor", "NO MENTOR ASSIGNED"));
                    MentorFragmentDirections.ActionNavMentorsToNavStudentForm action = MentorFragmentDirections.actionNavMentorsToNavStudentForm("new_mentor", "no_mentor", possibleMentorList);
                    action.setRecordType("mentor");
                    Navigation.findNavController(root).navigate(action);
                }
            });
        } else {
            fabAdd.hide();
        }

        // always hide save button here
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.hide();

        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view_students);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        //String[] myDataset = new String[] {"Test", "totally"};
        final Activity appActivity = getActivity();

        // request all the student information
        new ServerRequestHandler()
                .setMethod(Request.Method.GET)
                .setActivity(getActivity())
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/admin/students/mentor")
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONArray(new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<MentorAdapter.MentorListInfo> mentorDataset = new LinkedList<>();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject mentorObject = response.getJSONObject(i);
                                String objectId = mentorObject.getString("_id");
                                String name = mentorObject.getString("name");
                                String email = mentorObject.getString("email");
                                boolean confirmed = mentorObject.getBoolean("confirmed");
                                mentorDataset.add(new MentorAdapter.MentorListInfo(objectId, name, email, confirmed));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        mAdapter = new MentorAdapter(mentorDataset, appActivity);
                        recyclerView.setAdapter(mAdapter);
                    }
                })
                .executeRequest();

        return root;
    }
}
