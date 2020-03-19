package edu.gatech.edtech.culturechatapp.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;
import edu.gatech.edtech.culturechatapp.ui.mentor.MentorAdapter;
import edu.gatech.edtech.culturechatapp.ui.module.ModuleFormFragment;
import edu.gatech.edtech.culturechatapp.ui.module.ModuleFormFragmentArgs;

public class StudentFormFragment extends Fragment {
    String studentId = "";
    String studentName = "";
    String studentEmail = "";
    String studentMentorId = "";
    String recordType = "";
    List<MentorAdapter.MentorListInfo> possibleMentors = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.student_form, container, false);

        if (getArguments() != null) {
            this.studentName = StudentFormFragmentArgs.fromBundle(getArguments()).getName();
            this.studentEmail = StudentFormFragmentArgs.fromBundle(getArguments()).getEmail();
            this.studentMentorId = StudentFormFragmentArgs.fromBundle(getArguments()).getMentor();
            this.studentId = StudentFormFragmentArgs.fromBundle(getArguments()).getId();
            this.possibleMentors = StudentFormFragmentArgs.fromBundle(getArguments()).getPossibleMentors();
            this.recordType = StudentFormFragmentArgs.fromBundle(getArguments()).getRecordType();
        }

        final TextView studentNameTextView = root.findViewById(R.id.student_form_name_text);
        final TextView studentEmailTextView = root.findViewById(R.id.student_form_email_text);
        final Spinner studentMentorDropDown = root.findViewById(R.id.student_form_mentor_dropdown);

        studentEmailTextView.setEnabled(false);

        if (!this.studentName.equals("")) {
            studentNameTextView.setText(this.studentName);
        }

        if (!this.studentEmail.equals("")) {
            studentEmailTextView.setText(this.studentEmail);
        }

        if (this.possibleMentors != null && this.recordType.equals("student")) {
            List<String> spinnerOptions = new ArrayList<>();

            int spinnerOptionNumber = 0;
            for (int i = 0; i < this.possibleMentors.size(); i++) {
                MentorAdapter.MentorListInfo mentorInfo = this.possibleMentors.get(i);
                spinnerOptions.add(mentorInfo.mentorName);
                if (mentorInfo.mentorId.equals(this.studentMentorId)) {
                    spinnerOptionNumber = i;
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            studentMentorDropDown.setAdapter(adapter);
            studentMentorDropDown.setSelection(spinnerOptionNumber);
            root.findViewById(R.id.studentMentorLabel).setVisibility(View.VISIBLE);
            studentMentorDropDown.setVisibility(View.VISIBLE);
            if (studentId.equals("new_student")) {
                studentEmailTextView.setEnabled(true);
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Invite a student");

            } else {
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(this.studentName);
            }
        } else {
            studentMentorDropDown.setVisibility(View.GONE);
            root.findViewById(R.id.studentMentorLabel).setVisibility(View.GONE);
            if (studentId.equals("new_mentor")) {
                studentEmailTextView.setEnabled(true);
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Invite a mentor");
            } else {
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(this.studentName);
            }
        }

        //hide add button - doesn't matter if admin
        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        fabAdd.hide();

        // if admin - enable save button
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        if (ApplicationSetup.userRole.contentEquals("admin")) {
            fabConfirm.show();
            fabConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                // we need to save data
                String studentName = studentNameTextView.getText().toString();
                String studentEmail = studentEmailTextView.getText().toString();

                ServerRequestHandler request = new ServerRequestHandler()
                        .setActivity(getActivity())
                        .setLayout(R.id.drawer_layout)
                        .setAuthHeader(ApplicationSetup.userToken);

                Map<String, String> params = new HashMap<>();
                params.put("name", studentName);

                if (recordType.equals("student")) {
                    int chosenMentorItem = studentMentorDropDown.getSelectedItemPosition();
                    MentorAdapter.MentorListInfo selectedMentor = possibleMentors.get(chosenMentorItem);
                    params.put("assigned_mentor", selectedMentor.mentorId);
                    if (studentId.equals("new_student")) {
                        request.setMethod(Request.Method.POST)
                                .setEndpoint("/admin/students");
                        params.put("email", studentEmail);
                        params.put("role", "student");
                    } else {
                        request.setMethod(Request.Method.PUT)
                                .setEndpoint("/admin/student/" + studentId);
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(studentName);
                    }
                } else if (recordType.equals("mentor")) {
                    if (studentId.equals("new_mentor")) {
                        request.setMethod(Request.Method.POST)
                                .setEndpoint("/admin/students");
                        params.put("email", studentEmail);
                        params.put("role", "mentor");
                        params.put("assigned_mentor", "no_mentor");
                    } else {
                        request.setMethod(Request.Method.PUT)
                                .setEndpoint("/admin/student/" + studentId);
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(studentName);
                    }
                }

                JSONObject jsonObj = new JSONObject(params);
                request.setJSONData(jsonObj)
                    .setListenerJSONObject(new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String responseMessage = "Record was updated";
                                if (response.has("message")) {
                                    responseMessage = response.getString("message");
                                }
                                studentEmailTextView.setEnabled(false);
                                Snackbar.make(view, responseMessage, Snackbar.LENGTH_LONG)
                                        .show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                request.executeRequest();
                }
            });
        } else {
            fabConfirm.hide();
        }

        return root;
    }
}
