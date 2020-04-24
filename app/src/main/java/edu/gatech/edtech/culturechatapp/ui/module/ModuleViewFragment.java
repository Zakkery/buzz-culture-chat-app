package edu.gatech.edtech.culturechatapp.ui.module;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class ModuleViewFragment extends Fragment {
    String shortTitle = "";
    String fullTitle = "";
    String description = "";
    String moduleId = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.module_view, container, false);

        if (getArguments() != null) {
            this.shortTitle = ModuleFormFragmentArgs.fromBundle(getArguments()).getShortTitle();
            this.fullTitle = ModuleFormFragmentArgs.fromBundle(getArguments()).getLongTitle();
            this.description = ModuleFormFragmentArgs.fromBundle(getArguments()).getDescription();
            this.moduleId = ModuleFormFragmentArgs.fromBundle(getArguments()).getId();
        }

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(this.fullTitle);

        final TextView descriptionTextView = root.findViewById(R.id.module_view_description_text);

        if (!this.description.equals("")) {
            descriptionTextView.setText(this.description);
        }

        //hide add button - doesn't matter if admin
        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        fabAdd.hide();

        // if admin - enable save button
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.hide();
        return root;
    }
}
