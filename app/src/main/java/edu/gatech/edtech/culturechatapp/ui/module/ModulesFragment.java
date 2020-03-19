package edu.gatech.edtech.culturechatapp.ui.module;

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
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;

import java.util.LinkedList;
import java.util.List;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class ModulesFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_modules, container, false);

        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        // we are in modules fragment - check if we are an admin and enable add button
        if (ApplicationSetup.userRole.contentEquals("admin")) {
            fabAdd.show();
            fabAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ModulesFragmentDirections.NavModuleToForm action = ModulesFragmentDirections.navModuleToForm("new_module");
                    Navigation.findNavController(root).navigate(action);
                }
            });
        } else {
            fabAdd.hide();
        }

        // always hide save button here
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.hide();

        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view_modules);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        //String[] myDataset = new String[] {"Test", "totally"};
        final Activity appActivity = getActivity();

        // request all the modules information
        new ServerRequestHandler()
                .setMethod(Request.Method.GET)
                .setActivity(getActivity())
                .setLayout(R.id.drawer_layout)
                .setEndpoint("/modules")
                .setAuthHeader(ApplicationSetup.userToken)
                .setListenerJSONArray(new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<ModuleAdapter.ModuleListInfo> myDataset = new LinkedList<>();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                String objectId = response.getJSONObject(i).getString("_id");
                                String shownText = response.getJSONObject(i).getString("full_name");
                                myDataset.add(new ModuleAdapter.ModuleListInfo(objectId, shownText));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (ApplicationSetup.userRole.equals("student")) {
                            new ServerRequestHandler()
                                    .setMethod(Request.Method.GET)
                                    .setActivity(getActivity())
                                    .setLayout(R.id.drawer_layout)
                                    .setEndpoint("/student/modules")
                                    .setAuthHeader(ApplicationSetup.userToken)
                                    .setListenerJSONArray(new Response.Listener<JSONArray>() {
                                        @Override
                                        public void onResponse(JSONArray response) {
                                            // find all the modules in the list that are viewed and move them under the moved section
                                            try {
                                                if (response.length() > 0) {
                                                    List<String> viewedModuleIds = new LinkedList<>();
                                                    for (int i = 0; i < response.length(); i++) {
                                                        viewedModuleIds.add(response.getString(i));
                                                    }
                                                    final List<ModuleAdapter.ModuleListInfo> readModules = new LinkedList<>();
                                                    readModules.add(new ModuleAdapter.ModuleListInfo("_read", "Viewed Modules"));
                                                    for(int i = 0; i < myDataset.size(); i++) {
                                                        ModuleAdapter.ModuleListInfo module = myDataset.get(i);
                                                        if (viewedModuleIds.contains(module.moduleId)) {
                                                            readModules.add(module);
                                                            myDataset.remove(i);
                                                            i--;
                                                        }
                                                    }
                                                    myDataset.addAll(readModules);
                                                    mAdapter = new ModuleAdapter(myDataset, appActivity);
                                                    recyclerView.setAdapter(mAdapter);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).executeRequest();
                        } else {
                            mAdapter = new ModuleAdapter(myDataset, appActivity);
                            recyclerView.setAdapter(mAdapter);
                        }
                    }
                })
                .executeRequest();

        return root;
    }
}
