package edu.gatech.edtech.culturechatapp.ui.module;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class ModuleAvailableAdapter extends RecyclerView.Adapter<ModuleAvailableAdapter.ModuleViewHolder> {
    private List<ModuleListInfo> listDataset;
    private Activity appActivity;

    public static class ModuleViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;
        public ConstraintLayout rowLayout;
        public ImageButton rowDeleteButton;
        public ModuleViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.module_row_text);
            rowLayout = itemView.findViewById(R.id.module_row_layout);
            rowDeleteButton = itemView.findViewById(R.id.module_row_delete);
        }
    }

    public static class ModuleListInfo {
        public String moduleId;
        public String moduleDisplayText;

        public ModuleListInfo(String moduleId, String moduleDisplayText) {
            this.moduleId = moduleId;
            this.moduleDisplayText = moduleDisplayText;
        }
    }

    // This adapter takes list of ModuleListInfo records
    public ModuleAvailableAdapter(List<ModuleListInfo> modulesList, Activity appActivity) {
        this.listDataset = modulesList;
        this.appActivity = appActivity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ModuleAvailableAdapter.ModuleViewHolder onCreateViewHolder(ViewGroup parent,
                                                                      int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.module_view_row, parent, false);
        return new ModuleViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ModuleViewHolder holder, int position) {
        String moduleDisplayText = this.listDataset.get(position).moduleDisplayText;
        final String moduleId = this.listDataset.get(position).moduleId;
        final Activity fragmentAppActivity = this.appActivity;
        holder.textView.setText(moduleDisplayText);
        // Set callback on click with the id
        holder.rowLayout.setOnClickListener(view -> {
            // request GET the actual module info
            new ServerRequestHandler()
                .setMethod(Request.Method.GET)
                .setActivity(fragmentAppActivity)
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/module/" + moduleId)
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONObject(response -> {
                    ModulesFragmentDirections.NavModuleToForm action = ModulesFragmentDirections.navModuleToForm(moduleId);
                    try {
                        action.setDescription(response.getString("description"));
                        action.setLongTitle(response.getString("full_name"));
                        action.setShortTitle(response.getString("short_name"));
                        action.setId(moduleId);
                        Navigation.findNavController(view).navigate(action);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .executeRequest();
        });
        holder.rowDeleteButton.setVisibility(View.GONE);
        if (ApplicationSetup.userRole.equals("admin")) {
            // Add delete button listener
            holder.rowDeleteButton.setVisibility(View.VISIBLE);
            holder.rowDeleteButton.setOnClickListener(view -> new ServerRequestHandler()
                    .setMethod(Request.Method.DELETE)
                    .setActivity(fragmentAppActivity)
                    .setLayout(R.id.drawer_layout)
                    .setEndpoint("/admin/module/" + moduleId)
                    .setAuthHeader(ApplicationSetup.userToken)
                    .setListenerJSONObject(response -> {
                        try {
                            String successMessage = response.getString("message");
                            Snackbar.make(fragmentAppActivity.findViewById(R.id.drawer_layout), successMessage, Snackbar.LENGTH_LONG)
                                    .show();
                            // find item that was deleted
                            int listPoisition = 0;
                            for (; listPoisition < listDataset.size(); listPoisition++) {
                                if (listDataset.get(listPoisition).moduleId.equals(moduleId))
                                    break;
                            }
                            listDataset.remove(listPoisition);
                            notifyItemRemoved(listPoisition);
                            notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    })
                    .executeRequest());
        }
    }

    @Override
    public int getItemCount() {
        return this.listDataset.size();
    }
}