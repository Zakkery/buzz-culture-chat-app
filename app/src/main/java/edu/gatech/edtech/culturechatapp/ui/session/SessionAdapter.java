package edu.gatech.edtech.culturechatapp.ui.session;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class SessionAdapter extends  RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {
    private List<SessionAdapter.SessionListInfo> listDataset;
    private Activity appActivity;

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public ConstraintLayout rowLayout;
        public ImageButton rowDeleteButton;
        public ImageView confirmImage;
        public ImageView pendingImage;
        public SessionViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.session_row_text);
            rowLayout = itemView.findViewById(R.id.session_row_layout);
            rowDeleteButton = itemView.findViewById(R.id.session_row_delete);
            pendingImage = itemView.findViewById(R.id.session_row_pending);
            confirmImage = itemView.findViewById(R.id.session_row_confirm);
        }
    }

    public static class SessionListInfo {
        public String sessionId;
        public String studentName;
        public String mentorName;
        public String topic;
        public Date starts_at;
        public Date ends_at;
        public boolean approved;

        public SessionListInfo(String sessionId, String studentName,
                               String mentorName, String topic, Date starts_at, Date ends_at,
                               boolean approved) {
            this.sessionId = sessionId;
            this.studentName = studentName;
            this.mentorName = mentorName;
            this.topic = topic;
            this.starts_at = starts_at;
            this.ends_at = ends_at;
            this.approved = approved;
        }
    }

    // This adapter takes list of StudentListInfo records
    public SessionAdapter(List< SessionAdapter.SessionListInfo > studentsList, Activity appActivity) {
        this.listDataset = studentsList;
        this.appActivity = appActivity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SessionAdapter.SessionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.session_view_row, parent, false);
        return new SessionAdapter.SessionViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SessionAdapter.SessionViewHolder holder, int position) {
        SessionAdapter.SessionListInfo sessionObject = this.listDataset.get(position);
        final String sessionId = sessionObject.sessionId;
        final Activity fragmentAppActivity = this.appActivity;

        if (sessionObject.sessionId == "previous_sessions") {
            // special case for the divider
            holder.textView.setText(sessionObject.topic);
            holder.pendingImage.setVisibility(View.GONE);
            holder.rowDeleteButton.setVisibility(View.GONE);
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd 'at' K:mm a");
        String startDateText = formatter.format(sessionObject.starts_at);

        String sessionDisplayText = startDateText + " with " + sessionObject.mentorName + " (" + sessionObject.topic + ")";
        holder.textView.setText(sessionDisplayText);

        holder.rowDeleteButton.setVisibility(View.GONE);
        holder.pendingImage.setVisibility(View.GONE);
        holder.confirmImage.setVisibility(View.GONE);

        if (!sessionObject.approved || sessionObject.ends_at != null) {
            holder.rowDeleteButton.setVisibility(View.VISIBLE);
        }
        if (!sessionObject.approved && ApplicationSetup.userRole.equals("student")) {
            holder.pendingImage.setVisibility(View.VISIBLE);
        }
        if (!sessionObject.approved && ApplicationSetup.userRole.equals("mentor")) {
            holder.confirmImage.setVisibility(View.VISIBLE);
        }

        // Set callback on click with the id
        holder.rowLayout.setOnClickListener(view -> {
            // if session is old - show chat history
            // if session is upcoming - and not within next 1 minute - tell them to wait
            if (sessionObject.ends_at != null) {
                // show history
                SessionFragmentDirections.SessionToChat action = SessionFragmentDirections.sessionToChat(sessionId);
                action.setTopic(sessionObject.topic);
                Navigation.findNavController(view).navigate(action);
            } else {
                if (sessionObject.starts_at.getTime() - (new Date()).getTime() < 3 * 60 * 1000) {
                    // about to start
                    // this session is not closed
                    SessionFragmentDirections.SessionToChat action = SessionFragmentDirections.sessionToChat(sessionId);
                    action.setTopic(sessionObject.topic);
                    action.setClosed(false);
                    Navigation.findNavController(view).navigate(action);
                } else {
                    // too far away from start
                    Snackbar.make(fragmentAppActivity.findViewById(R.id.drawer_layout), "Session is more than 3 minutes away from start", Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        holder.confirmImage.setOnClickListener(view-> {
            // on click send approval and hide approve button
            new ServerRequestHandler()
                .setMethod(Request.Method.PUT)
                .setActivity(fragmentAppActivity)
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/student/session/confirm/" + sessionId)
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONObject(response -> {
                    try{
                        Snackbar.make(fragmentAppActivity.findViewById(R.id.drawer_layout), "Session has been confirmed", Snackbar.LENGTH_LONG)
                                .show();
                        holder.confirmImage.setVisibility(View.GONE);
                        holder.rowDeleteButton.setVisibility(View.GONE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                    .executeRequest();

        });

        // Add delete button listener
        holder.rowDeleteButton.setOnClickListener(view -> new ServerRequestHandler()
                .setMethod(Request.Method.DELETE)
                .setActivity(fragmentAppActivity)
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/student/session/" + sessionId)
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONObject(response -> {
                    try {
                        String successMessage = response.getString("message");
                        Snackbar.make(fragmentAppActivity.findViewById(R.id.drawer_layout), successMessage, Snackbar.LENGTH_LONG)
                                .show();
                        // find item that was deleted
                        int listPosition = 0;
                        for (; listPosition < listDataset.size(); listPosition++) {
                            if (listDataset.get(listPosition).sessionId.equals(sessionId))
                                break;
                        }

                        listDataset.remove(listPosition);
                        notifyItemRemoved(listPosition);
                        // check if there is no more old sessions
                        boolean oldSessionsPresent = false;
                        for (SessionListInfo sess : listDataset) {
                            if (sess.ends_at != null) {
                                oldSessionsPresent = true;
                                break;
                            }
                        }

                        if (!oldSessionsPresent) {
                            listPosition = 0;
                            for (; listPosition < listDataset.size(); listPosition++) {
                                if (listDataset.get(listPosition).sessionId.equals("previous_sessions"))
                                    break;
                            }
                            listDataset.remove(listPosition);
                            notifyItemRemoved(listPosition);
                        }
                        notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .executeRequest());
    }

    @Override
    public int getItemCount() {
        return this.listDataset.size();
    }
}
