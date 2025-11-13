package com.example.hikenativeapp.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hikenativeapp.R;
import com.example.hikenativeapp.data.chatbot.ChatMessage;
import com.example.hikenativeapp.util.MarkdownUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private List<ChatMessage> messages = new ArrayList<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof BotMessageViewHolder) {
            ((BotMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<ChatMessage> newMessages) {
        Log.d(TAG, "Updating chat messages. Total: " + newMessages.size());

        // Calculate differences
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return messages.size();
            }

            @Override
            public int getNewListSize() {
                return newMessages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                ChatMessage oldMsg = messages.get(oldItemPosition);
                ChatMessage newMsg = newMessages.get(newItemPosition);

                // Compare by ID if available, otherwise by timestamp and content
                if (oldMsg.getId() != null && newMsg.getId() != null) {
                    return oldMsg.getId().equals(newMsg.getId());
                }

                return oldMsg.getTimestampLong() == newMsg.getTimestampLong() &&
                        oldMsg.isUser() == newMsg.isUser();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ChatMessage oldMsg = messages.get(oldItemPosition);
                ChatMessage newMsg = newMessages.get(newItemPosition);

                String oldContent = oldMsg.isUser() ? oldMsg.getMessage() : oldMsg.getResponse();
                String newContent = newMsg.isUser() ? newMsg.getMessage() : newMsg.getResponse();

                return oldContent != null && oldContent.equals(newContent);
            }
        });

        // Update the list
        this.messages = new ArrayList<>(newMessages);

        // Apply the updates
        diffResult.dispatchUpdatesTo(this);

        // Debug: Print all messages
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg.isUser()) {
                Log.d(TAG, "  " + i + ". User: " + msg.getMessage());
            } else {
                Log.d(TAG, "  " + i + ". Bot: " + msg.getResponse());
            }
        }
    }

    // ViewHolder for user messages
    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timeText;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time);
        }

        public void bind(ChatMessage message) {
            String text = message.getMessage();
            Log.d(TAG, "Binding user message: " + text);

            messageText.setText(text);
            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            } else {
                timeText.setText("");
            }
        }
    }

    // ViewHolder for bot messages
    class BotMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timeText;

        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time);
        }

        public void bind(ChatMessage message) {
            String text = message.getResponse();
            Log.d(TAG, "Binding bot message: " + text);

            // Use MarkdownUtil to render markdown
            MarkdownUtil.setMarkdown(messageText, text);

            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            } else {
                timeText.setText("");
            }
        }
    }
}
