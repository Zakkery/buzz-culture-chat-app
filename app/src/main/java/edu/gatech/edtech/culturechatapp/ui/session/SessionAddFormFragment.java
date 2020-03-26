package edu.gatech.edtech.culturechatapp.ui.session;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;
import edu.gatech.edtech.culturechatapp.ui.mentor.MentorAdapter;
import edu.gatech.edtech.culturechatapp.ui.module.ModuleAdapter;
import edu.gatech.edtech.culturechatapp.ui.student.StudentFormFragmentArgs;

public class SessionAddFormFragment extends Fragment {
    String mentorId = "";
    String mentorName = "";
    ArrayList<ModuleAdapter.ModuleListInfo> availableModules = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Calendar chosenDate = Calendar.getInstance();
        chosenDate.add(Calendar.HOUR_OF_DAY, 1);

        View root = inflater.inflate(R.layout.session_add_form, container, false);

        if (getArguments() != null) {
            this.availableModules = SessionAddFormFragmentArgs.fromBundle(getArguments()).getPossibleTopics();
            this.mentorId = SessionAddFormFragmentArgs.fromBundle(getArguments()).getMentorId();
            this.mentorName = SessionAddFormFragmentArgs.fromBundle(getArguments()).getMentorName();
        }

        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        fabAdd.hide();

        final EditText mentorNameTextView = root.findViewById(R.id.session_add_form_mentor_choice_text);
        final Spinner topicDropdown = root.findViewById(R.id.session_add_form_topic_choice_dropdown);
        final TextView otherTopicTextView = root.findViewById(R.id.session_other_topic_label);
        final EditText otherTopicEditText = root.findViewById(R.id.session_add_form_topic_other_text);
        EditText pickedDateTime = root.findViewById(R.id.session_add_form_start_datetime_text);
        ImageButton datePickButton = root.findViewById(R.id.session_add_form_date_time_set);

        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd 'at' K:mm a");
        String formattedChosenStartDate = formatter.format(chosenDate.getTime());
        pickedDateTime.setText(formattedChosenStartDate);

        mentorNameTextView.setText(this.mentorName + " (Peer mentor)");
        mentorNameTextView.setEnabled(false);

        List<String> spinnerOptions = new ArrayList<>();

        for (int i = 0; i < this.availableModules.size(); i++) {
            ModuleAdapter.ModuleListInfo moduleInfo = this.availableModules.get(i);
            spinnerOptions.add(moduleInfo.moduleDisplayText);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        topicDropdown.setAdapter(adapter);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Schedule a new session");


        topicDropdown.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (availableModules.get(position).moduleId.equals("other")) {
                    // enable typing
                    otherTopicEditText.setText("");
                    otherTopicTextView.setVisibility(View.VISIBLE);
                    otherTopicEditText.setVisibility(View.VISIBLE);
                } else {
                    otherTopicTextView.setVisibility(View.GONE);
                    otherTopicEditText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        datePickButton.setOnClickListener(view -> {
            new DatePickerDialog(view.getContext(), (view1, year, monthOfYear, dayOfMonth) -> {
                chosenDate.set(year, monthOfYear, dayOfMonth);
                new TimePickerDialog(view1.getContext(), (view11, hourOfDay, minute) -> {
                    chosenDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    chosenDate.set(Calendar.MINUTE, minute);
                    String formattedStartDate = formatter.format(chosenDate.getTime());
                    pickedDateTime.setText(formattedStartDate);
                }, chosenDate.get(Calendar.HOUR_OF_DAY), chosenDate.get(Calendar.MINUTE), false).show();
            }, chosenDate.get(Calendar.YEAR), chosenDate.get(Calendar.MONTH), chosenDate.get(Calendar.DATE)).show();
        });

        // enable save button
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.show();
        fabConfirm.setOnClickListener(view -> {
            // we need to save data
            int chosenTopicItem = topicDropdown.getSelectedItemPosition();
            // check if it is "other"
            String topic = availableModules.get(chosenTopicItem).moduleDisplayText;
            if (availableModules.get(chosenTopicItem).moduleId.equals("other")) {
                // enable typing
                topic = otherTopicEditText.getText().toString();
                if (topic.equals("")) {
                    otherTopicEditText.setError("Type in the topic text");
                    return;
                }
            }
            long startDateUnixTimestamp = chosenDate.getTimeInMillis()/1000;

            Map<String, String> params = new HashMap<>();
            params.put("topic", topic);
            params.put("starts_at", Long.toString(startDateUnixTimestamp));
            params.put("mentor", mentorId);
            JSONObject jsonObj = new JSONObject(params);

            new ServerRequestHandler()
                    .setActivity(getActivity())
                    .setLayout(R.id.drawer_layout)
                    .setAuthHeader(ApplicationSetup.userToken)
                    .setMethod(Request.Method.POST)
                    .setEndpoint("/student/sessions")
                    .setJSONData(jsonObj)
                    .setListenerJSONObject(response -> {
                        try {
                            String responseMessage = "Record was updated";
                            if (response.has("message")) {
                                responseMessage = response.getString("message");
                            }
                            Navigation.findNavController(root).navigate(R.id.action_nav_session_add_form_to_nav_sessions);
                            Snackbar.make(view, responseMessage, Snackbar.LENGTH_LONG)
                                    .show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    })
                    .executeRequest();
        });

        return root;
    }

}
