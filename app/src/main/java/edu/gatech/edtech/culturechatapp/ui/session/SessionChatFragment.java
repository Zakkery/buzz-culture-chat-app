package edu.gatech.edtech.culturechatapp.ui.session;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Transport;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Manager;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import edu.gatech.edtech.culturechatapp.ApplicationSetup;
import edu.gatech.edtech.culturechatapp.R;
import edu.gatech.edtech.culturechatapp.ServerRequestHandler;

public class SessionChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private Socket mSocket = null;
    View root;

    @Override
    public void onStart() {
        super.onStart();
        boolean sessionClosed = true;
        String sessionId = "";
        String topic = "";

        if (getArguments() != null) {
            sessionId = SessionChatFragmentArgs.fromBundle(getArguments()).getSessionId();
            sessionClosed = SessionChatFragmentArgs.fromBundle(getArguments()).getClosed();
            topic = SessionChatFragmentArgs.fromBundle(getArguments()).getTopic();
        }

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(topic);
        FloatingActionButton fabAdd = getActivity().findViewById(R.id.fabAdd);
        fabAdd.hide();

        // always hide save button here
        FloatingActionButton fabConfirm = getActivity().findViewById(R.id.fabConfirm);
        fabConfirm.hide();

        // disable chat button and text input if session is not current
        if (sessionClosed) {
            root.findViewById(R.id.session_chat_response_layout).setVisibility(View.GONE);
            root.findViewById(R.id.session_chat_inactive_layout).setVisibility(View.VISIBLE);
        } else {
            root.findViewById(R.id.session_chat_response_layout).setVisibility(View.VISIBLE);
            root.findViewById(R.id.session_chat_inactive_layout).setVisibility(View.GONE);
        }

        // find message recyclerView
        recyclerView = root.findViewById(R.id.session_chat_messages_recyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        final Activity appActivity = getActivity();

        if (sessionClosed) {
            // connect to the server regularly - we are just getting history
            new ServerRequestHandler()
                    .setMethod(Request.Method.GET)
                    .setActivity(getActivity())
                    .setLayout(R.id.drawer_layout)
                    .setEndpoint("/student/session/" + sessionId)
                    .setAuthHeader(ApplicationSetup.userToken)
                    .setListenerJSONObject(response -> {
                        List<ChatAdapter.MessageListInfo> messageList = new ArrayList<>();
                        try {
                            JSONObject mentorInfo = response.getJSONObject("mentor");
                            String mentorName = mentorInfo.getString("name");
                            JSONObject studentInfo = response.getJSONObject("student");
                            String studentName = studentInfo.getString("name");
                            JSONArray messageObjectList = response.getJSONArray("messages");
                            for (int i = 0; i < messageObjectList.length(); i++) {
                                // get from, text, and sent_date
                                JSONObject messageObject = messageObjectList.getJSONObject(i);

                                String messageFrom = messageObject.getString("from");
                                String messageText = messageObject.getString("body");
                                String sentAtString = messageObject.getString("sent_at");

                                Date sentAt = ApplicationSetup.dateFromMongoString(sentAtString);
                                String messageSentAt = ApplicationSetup.stringFromDateChat(sentAt);
                                String senderName = ApplicationSetup.userName;
                                if (!messageFrom.equals(ApplicationSetup.userId)) {
                                    if (ApplicationSetup.userRole.equals("student")) {
                                        senderName = mentorName;
                                    } else {
                                        senderName = studentName;
                                    }
                                }
                                messageList.add(new ChatAdapter.MessageListInfo(messageFrom, senderName, messageText, messageSentAt));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mAdapter = new ChatAdapter(messageList, appActivity);
                        recyclerView.setAdapter(mAdapter);

                        root.findViewById(R.id.session_chat_scroll_view).postDelayed(() -> {
                            //replace this line to scroll up or down
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }, 100L);
                    })
                    .executeRequest();
        } else {
            // session is ongoing - connect to the socket
            try {
                root.findViewById(R.id.session_chat_response_layout).setVisibility(View.GONE);
                mSocket = IO.socket("https://acculturation-chat.herokuapp.com");

                mSocket.io().on(Manager.EVENT_TRANSPORT, args -> {
                    Transport transport = (Transport)args[0];

                    transport.on(Transport.EVENT_REQUEST_HEADERS, args1 -> {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args1[0];
                        // modify request headers
                        headers.put("authorization", Arrays.asList(ApplicationSetup.userToken));
                    });
                });

                String finalSessionId = sessionId;
                List<ChatAdapter.MessageListInfo> messageList = new ArrayList<>();
                AtomicReference<String> mentorName = new AtomicReference<>();
                AtomicReference<String> studentName = new AtomicReference<>();

                messageList.clear();
                mAdapter = new ChatAdapter(messageList, appActivity);
                recyclerView.setAdapter(mAdapter);

                mSocket.on(Socket.EVENT_CONNECT, args -> appActivity.runOnUiThread(()-> {
                    //connected - allow user to see messages and typing field
                    root.findViewById(R.id.session_chat_response_layout).setVisibility(View.VISIBLE);
                    //join the appropriate room
                    JSONObject startChatParams = new JSONObject();
                    try {
                        startChatParams.put("sessionId", finalSessionId);
                        mSocket.emit("start_chat", startChatParams);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })).on("error", args -> appActivity.runOnUiThread(()-> {
                    System.out.println(args);
                })).on("start_chat", args -> appActivity.runOnUiThread(() -> {
                    JSONObject response = (JSONObject) args[0];
                    try {
                        JSONObject realData = response.getJSONObject("data");

                        JSONObject mentorInfo = realData.getJSONObject("mentor");
                        mentorName.set(mentorInfo.getString("name"));

                        JSONObject studentInfo = realData.getJSONObject("student");
                        studentName.set(studentInfo.getString("name"));

                        messageList.clear();

                        JSONArray messageObjectList = realData.getJSONArray("messages");
                        for (int i = 0; i < messageObjectList.length(); i++) {
                            // get from, text, and sent_date
                            JSONObject messageObject = messageObjectList.getJSONObject(i);

                            String messageFrom = messageObject.getString("from");
                            String messageText = messageObject.getString("body");
                            String sentAtString = messageObject.getString("sent_at");

                            Date sentAt = ApplicationSetup.dateFromMongoString(sentAtString);
                            String messageSentAt = ApplicationSetup.stringFromDateChat(sentAt);
                            String senderName = ApplicationSetup.userName;
                            if (!messageFrom.equals(ApplicationSetup.userId)) {
                                if (ApplicationSetup.userRole.equals("student")) {
                                    senderName = mentorName.get();
                                } else {
                                    senderName = studentName.get();
                                }
                            }
                            messageList.add(new ChatAdapter.MessageListInfo(messageFrom, senderName, messageText, messageSentAt));
                        }
                        mAdapter.notifyDataSetChanged();
                        root.findViewById(R.id.session_chat_scroll_view).postDelayed(() -> {
                            //replace this line to scroll up or down
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }, 100L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })).on("message", args -> appActivity.runOnUiThread(() -> {
                    // we get new message!
                    JSONObject response = (JSONObject) args[0];
                    try {
                        JSONObject messageData = response.getJSONObject("data");
                        String messageFrom = messageData.getString("from");
                        String messageText = messageData.getString("body");
                        String sentAtString = messageData.getString("sent_at");

                        Date sentAt = ApplicationSetup.dateFromMongoString(sentAtString);
                        String messageSentAt = ApplicationSetup.stringFromDateChat(sentAt);
                        String senderName = ApplicationSetup.userName;

                        if (!messageFrom.equals(ApplicationSetup.userId)) {
                            if (ApplicationSetup.userRole.equals("student")) {
                                senderName = mentorName.get();
                            } else {
                                senderName = studentName.get();
                            }
                        }
                        messageList.add(new ChatAdapter.MessageListInfo(messageFrom, senderName, messageText, messageSentAt));
                        mAdapter.notifyDataSetChanged();
                        root.findViewById(R.id.session_chat_scroll_view).postDelayed(() -> {
                            //replace this line to scroll up or down
                            ((ScrollView) root.findViewById(R.id.session_chat_scroll_view)).fullScroll(ScrollView.FOCUS_DOWN);
                        }, 100L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })).on("exit_chat", args -> appActivity.runOnUiThread(() -> {
                    // we are done here - hide bottom bar - add message that this chat is now archived
                    root.findViewById(R.id.session_chat_response_layout).setVisibility(View.GONE);
                    root.findViewById(R.id.session_chat_inactive_layout).setVisibility(View.VISIBLE);
                }));
                mSocket.connect();

                root.findViewById(R.id.session_chat_send_message_button).setOnClickListener(view -> {
                    // on send click - send message
                    EditText messageTextView = root.findViewById(R.id.session_chat_message_input_text);
                    String messageText = messageTextView.getText().toString();
                    if (!messageText.equals("")) {
                        try {
                            JSONObject messageRequestJSON = new JSONObject();
                            messageRequestJSON.put("text", messageText);
                            mSocket.emit("message", messageRequestJSON);
                            messageTextView.setText("");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });

                root.findViewById(R.id.session_chat_finish_button).setOnClickListener(view -> {
                    // on exit click - finalize the chat
                    EditText messageTextView = root.findViewById(R.id.session_chat_message_input_text);
                    mSocket.emit("finalize_chat");
                    messageTextView.setText("");
                    root.findViewById(R.id.session_chat_response_layout).setVisibility(View.GONE);
                    root.findViewById(R.id.session_chat_inactive_layout).setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.root = inflater.inflate(R.layout.session_chat_form, container, false);
        return this.root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSocket != null && mSocket.connected()) {
            mSocket.disconnect();
        }
    }
}