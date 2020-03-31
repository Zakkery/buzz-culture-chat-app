package edu.gatech.edtech.culturechatapp.ui.module;

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
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;

import java.util.LinkedList;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class ModulesFragment extends Fragment {

    private View root;
    private Activity appActivity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.root = inflater.inflate(R.layout.fragment_modules, container, false);

        // always hide save button here
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.hide();

        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        // we are in modules fragment - check if we are an admin and enable add button
        if (ApplicationSetup.userRole.contentEquals("admin")) {
            fabAdd.show();
            fabAdd.setOnClickListener(view -> {
                ModulesFragmentDirections.NavModuleToForm action = ModulesFragmentDirections.navModuleToForm("new_module");
                Navigation.findNavController(this.root).navigate(action);
            });
        } else {
            fabAdd.hide();
        }

        this.appActivity = getActivity();

        // request all the modules information
        new ServerRequestHandler()
            .setMethod(Request.Method.GET)
            .setActivity(appActivity)
            .setLayout(R.id.drawer_layout)
            .setEndpoint("/modules")
            .setAuthHeader(ApplicationSetup.userToken)
            .setListenerJSONArray(response -> {
                final List<ModuleAvailableAdapter.ModuleListInfo> availableModules = new LinkedList<>();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        String objectId = response.getJSONObject(i).getString("_id");
                        String shownText = response.getJSONObject(i).getString("full_name");
                        availableModules.add(new ModuleAvailableAdapter.ModuleListInfo(objectId, shownText));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (ApplicationSetup.userRole.equals("student")) {
                    new ServerRequestHandler()
                        .setMethod(Request.Method.GET)
                        .setActivity(appActivity)
                        .setLayout(R.id.drawer_layout)
                        .setEndpoint("/student/modules")
                        .setAuthHeader(ApplicationSetup.userToken)
                        .setListenerJSONArray(response1 -> {
                            // find all the modules in the list that are viewed and move them under the moved section
                            try {
                                if (response1.length() > 0) {
                                    List<String> viewedModuleIds = new LinkedList<>();
                                    for (int i = 0; i < response1.length(); i++) {
                                        viewedModuleIds.add(response1.getString(i));
                                    }
                                    final List<ModuleAvailableAdapter.ModuleListInfo> readModules = new LinkedList<>();
                                    //readModules.add(new ModuleAvailableAdapter.ModuleListInfo("_read", "Viewed Modules"));
                                    for(int i = 0; i < availableModules.size(); i++) {
                                        ModuleAvailableAdapter.ModuleListInfo module = availableModules.get(i);
                                        if (viewedModuleIds.contains(module.moduleId)) {
                                            readModules.add(module);
                                            availableModules.remove(i);
                                            i--;
                                        }
                                    }
                                    getActivity().runOnUiThread(()->{
                                        this.populateViewedModules(readModules);
                                        this.populateAvailableModules(availableModules);
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).executeRequest();
                } else {
                    getActivity().runOnUiThread(()->{
                        this.populateViewedModules(new LinkedList<>());
                        this.populateAvailableModules(availableModules);
                    });
                }
            })
            .executeRequest();

        return root;
    }

    public void populateAvailableModules(List<ModuleAvailableAdapter.ModuleListInfo> availableModules) {
        ConstraintLayout constrainedAvailableModulesLayout = getActivity().findViewById(R.id.module_available_layout);
        if (availableModules.size() < 1) {
            constrainedAvailableModulesLayout.setVisibility(View.GONE);
        } else {
            constrainedAvailableModulesLayout.setVisibility(View.VISIBLE);
            RecyclerView availableModulesRecyclerView = this.root.findViewById(R.id.module_list_available);
            availableModulesRecyclerView.setHasFixedSize(true);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.appActivity);
            availableModulesRecyclerView.setLayoutManager(layoutManager);

            RecyclerView.Adapter mAdapter = new ModuleAvailableAdapter(availableModules, this.appActivity);
            availableModulesRecyclerView.setAdapter(mAdapter);
        }
    }

    public void populateViewedModules(List<ModuleAvailableAdapter.ModuleListInfo> availableModules) {
        ConstraintLayout constrainedAvailableModulesLayout = getActivity().findViewById(R.id.module_watched_layout);
        if (availableModules.size() < 1) {
            constrainedAvailableModulesLayout.setVisibility(View.GONE);
        } else {
            constrainedAvailableModulesLayout.setVisibility(View.VISIBLE);
            RecyclerView availableModulesRecyclerView = this.root.findViewById(R.id.module_list_watched);
            availableModulesRecyclerView.setHasFixedSize(true);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.appActivity);
            availableModulesRecyclerView.setLayoutManager(layoutManager);

            RecyclerView.Adapter mAdapter = new ModuleAvailableAdapter(availableModules, this.appActivity);
            availableModulesRecyclerView.setAdapter(mAdapter);
        }
    }
}
