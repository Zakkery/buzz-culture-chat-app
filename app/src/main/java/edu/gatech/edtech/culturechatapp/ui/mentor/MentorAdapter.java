package edu.gatech.edtech.culturechatapp.ui.mentor;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import edu.gatech.edtech.culturechatapp.ui.module.ModulesFragmentDirections;
import edu.gatech.edtech.culturechatapp.ui.student.StudentAdapter;
import edu.gatech.edtech.culturechatapp.ui.student.StudentFragmentDirections;

public class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.MentorViewHolder> {
    private List<MentorListInfo> listDataset;
    private Activity appActivity;

    public static class MentorViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public RelativeLayout rowLayout;
        public ImageButton rowDeleteButton;
        public ImageView pendingImage;

        public MentorViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.student_row_text);
            rowLayout = itemView.findViewById(R.id.student_row_layout);
            rowDeleteButton = itemView.findViewById(R.id.student_row_delete);
            pendingImage = itemView.findViewById(R.id.student_row_pending);
        }
    }

    public static class MentorListInfo {
        public String mentorId;
        public String mentorName;
        public String mentorEmail;
        public boolean confirmed;

        public MentorListInfo(String mentorId, String mentorName) {
            this.mentorId = mentorId;
            this.mentorName = mentorName;
        }

        public MentorListInfo(String mentorId, String mentorName, String mentorEmail, boolean confirmed) {
            this.mentorId = mentorId;
            this.mentorName = mentorName;
            this.mentorEmail = mentorEmail;
            this.confirmed = confirmed;
        }
    }

    // This adapter takes list of ModuleListInfo records
    public MentorAdapter(List<MentorListInfo> modulesList, Activity appActivity) {
        this.listDataset = modulesList;
        this.appActivity = appActivity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MentorAdapter.MentorViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.student_view_row, parent, false);
        return new MentorViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MentorAdapter.MentorViewHolder holder, int position) {
        String studentName = this.listDataset.get(position).mentorName;
        String studentEmail = this.listDataset.get(position).mentorEmail;
        final String studentId = this.listDataset.get(position).mentorId;
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
                                final ArrayList<MentorListInfo> possibleMentorList = new ArrayList<>();
                                possibleMentorList.add(new MentorAdapter.MentorListInfo("no_mentor", "NO MENTOR ASSIGNED"));

                                String studentName = response.getString("name");
                                String studentEmail = response.getString("email");

                                MentorFragmentDirections.ActionNavMentorsToNavStudentForm action = MentorFragmentDirections.actionNavMentorsToNavStudentForm(studentId, "no_mentor", possibleMentorList);
                                action.setEmail(studentEmail);
                                action.setName(studentName);
                                action.setRecordType("mentor");
                                Navigation.findNavController(view).navigate(action);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
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
                                        if (listDataset.get(listPosition).mentorId.equals(studentId))
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