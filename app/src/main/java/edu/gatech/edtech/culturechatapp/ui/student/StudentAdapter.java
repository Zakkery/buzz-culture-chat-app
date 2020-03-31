package edu.gatech.edtech.culturechatapp.ui.student;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;
import edu.gatech.edtech.culturechatapp.ui.mentor.MentorAdapter;
import edu.gatech.edtech.culturechatapp.ui.module.ModulesFragmentDirections;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<StudentListInfo> listDataset;
    private Activity appActivity;

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public ConstraintLayout rowLayout;
        public ImageButton rowDeleteButton;
        public ImageView pendingImage;
        public StudentViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.student_row_text);
            rowLayout = itemView.findViewById(R.id.student_row_layout);
            rowDeleteButton = itemView.findViewById(R.id.student_row_delete);
            pendingImage = itemView.findViewById(R.id.student_row_pending);
        }
    }

    public static class StudentListInfo {
        public String studentId;
        public String studentName;
        public String studentEmail;
        public boolean confirmed;

        public StudentListInfo(
                String studentId, String studentName, String studentEmail, boolean confirmed) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.studentEmail = studentEmail;
            this.confirmed = confirmed;
        }
    }

    // This adapter takes list of StudentListInfo records
    public StudentAdapter(List<StudentListInfo> studentsList, Activity appActivity) {
        this.listDataset = studentsList;
        this.appActivity = appActivity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public StudentAdapter.StudentViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.student_view_row, parent, false);
        return new StudentViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(StudentViewHolder holder, int position) {
        String studentName = this.listDataset.get(position).studentName;
        String studentEmail = this.listDataset.get(position).studentEmail;
        final String studentId = this.listDataset.get(position).studentId;
        final Activity fragmentAppActivity = this.appActivity;

        String studentDisplayText = studentName + " (" + studentEmail + ")";
        holder.textView.setText(studentDisplayText);

        if (!this.listDataset.get(position).confirmed) {
            holder.pendingImage.setVisibility(View.VISIBLE);
        }

        // Set callback on click with the id
        holder.rowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                // request GET all mentors
                new ServerRequestHandler()
                    .setMethod(Request.Method.GET)
                    .setActivity(fragmentAppActivity)
                    .setLayout(R.id.drawer_layout)
                    .setEndpoint("/admin/students/mentor")
                    .setAuthHeader(ApplicationSetup.userToken)
                    .setListenerJSONArray(new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            final ArrayList<MentorAdapter.MentorListInfo> possibleMentorList = new ArrayList<>();
                            possibleMentorList.add(new MentorAdapter.MentorListInfo("no_mentor", "NO MENTOR ASSIGNED"));
                            if (response.length() > 0) {
                                for (int i = 0; i < response.length(); i++) {
                                    try {
                                        JSONObject mentorObject = response.getJSONObject(i);
                                        String mentorId = mentorObject.getString("_id");
                                        String mentorName = mentorObject.getString("name");
                                        MentorAdapter.MentorListInfo mentorToAdd = new MentorAdapter.MentorListInfo(mentorId, mentorName);
                                        possibleMentorList.add(mentorToAdd);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            // GET student information
                            new ServerRequestHandler()
                                .setMethod(Request.Method.GET)
                                .setActivity(fragmentAppActivity)
                                .setLayout(R.id.drawer_layout)
                                .setEndpoint("/admin/student/" + studentId)
                                .setAuthHeader(ApplicationSetup.userToken)
                                .setListenerJSONObject(new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            String studentMentorId = "no_mentor";
                                            if (response.has("assigned_mentor")) {
                                                studentMentorId = response.getJSONObject("assigned_mentor").getString("_id");
                                            }
                                            String studentName = response.getString("name");
                                            String studentEmail = response.getString("email");

                                            StudentFragmentDirections.NavStudentToForm action = StudentFragmentDirections.navStudentToForm(studentId, studentMentorId, possibleMentorList);
                                            action.setEmail(studentEmail);
                                            action.setName(studentName);
                                            Navigation.findNavController(view).navigate(action);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                            .executeRequest();
                        }
                    })
                    .executeRequest();
            }
        });

        // Add delete button listener
        holder.rowDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new ServerRequestHandler()
                        .setMethod(Request.Method.DELETE)
                        .setActivity(fragmentAppActivity)
                        .setLayout(R.id.drawer_layout)
                        .setEndpoint("/admin/student/" + studentId)
                        .setAuthHeader(ApplicationSetup.userToken)
                        .setListenerJSONObject(new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String successMessage = response.getString("message");
                                    Snackbar.make(fragmentAppActivity.findViewById(R.id.drawer_layout), successMessage, Snackbar.LENGTH_LONG)
                                            .show();
                                    // find item that was deleted
                                    int listPosition = 0;
                                    for (; listPosition < listDataset.size(); listPosition++) {
                                        if (listDataset.get(listPosition).studentId.equals(studentId))
                                            break;
                                    }
                                    listDataset.remove(listPosition);
                                    notifyItemRemoved(listPosition);
                                    notifyDataSetChanged();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .executeRequest();
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.listDataset.size();
    }
}