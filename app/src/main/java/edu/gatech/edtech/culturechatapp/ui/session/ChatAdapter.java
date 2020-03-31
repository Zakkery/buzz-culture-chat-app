package edu.gatech.edtech.culturechatapp.ui.session;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
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

public class ChatAdapter extends RecyclerView.Adapter {
    private static final int VIEW_MESSAGE_SENT = 1;
    private static final int VIEW_MESSAGE_RECEIVED = 2;

    private List<ChatAdapter.MessageListInfo> messageList;
    private Activity appActivity;

    public static class MessageListInfo {
        public String sentByUserId;
        public String senderName;
        public String messageText;
        public String sentAt;

        public MessageListInfo(String sentByUserId, String senderName,
                               String messageText, String sentAt) {
            this.sentByUserId = sentByUserId;
            this.senderName = senderName;
            this.messageText = messageText;
            this.sentAt = sentAt;
        }
    }

    public ChatAdapter(List< ChatAdapter.MessageListInfo > messageList, Activity appActivity) {
        this.messageList = messageList;
        this.appActivity = appActivity;
    }

    public void setMessageList(List<ChatAdapter.MessageListInfo> messageList) {
        this.messageList = messageList;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        MessageListInfo actualMessage = messageList.get(position);
        if (actualMessage.sentByUserId.equals(ApplicationSetup.userId)) {
            return VIEW_MESSAGE_SENT;
        } else {
            return VIEW_MESSAGE_RECEIVED;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_MESSAGE_SENT) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.session_chat_sent_message, parent, false);
            return new SentMessageViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.session_chat_received_message, parent, false);
            return new ReceivedMessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageListInfo actualMessage = messageList.get(position);

        // inflate appropriate message holder - if sender_id is same as user's then inflate sentMessageViewHolder
        // otherwise, ReceivedMessageViewHolder
        if (actualMessage.sentByUserId.equals(ApplicationSetup.userId)) {
            ((SentMessageViewHolder) holder).bind(actualMessage);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(actualMessage);
        }
    }

    @Override
    public int getItemCount() {
        return this.messageList.size();
    }

    private class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageView, timeView;

        SentMessageViewHolder(View itemView) {
            super(itemView);

            timeView = itemView.findViewById(R.id.text_message_time);
            messageView = itemView.findViewById(R.id.text_message_body);
        }

        void bind(MessageListInfo message) {
            messageView.setText(message.messageText);
            timeView.setText(message.sentAt);
        }
    }

    private class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageView, timeView, fromView;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);

            timeView = itemView.findViewById(R.id.text_message_time);
            messageView = itemView.findViewById(R.id.text_message_body);
            fromView = itemView.findViewById(R.id.text_message_name);
        }

        void bind(MessageListInfo message) {
            messageView.setText(message.messageText);
            timeView.setText(message.sentAt);
            fromView.setText(message.senderName);
        }
    }
}
