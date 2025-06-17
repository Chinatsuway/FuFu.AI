package com.example.myapplication.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private final List<Message> messages;

    // ViewHolder内部类
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }

    // 构造函数
    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载消息项布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // 设置消息内容
        holder.messageText.setText(message.getContent());

        // 动态调整布局参数
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageText.getLayoutParams();

        // 根据消息来源设置对齐方式
        if (message.isUser()) {
            params.gravity = Gravity.END;  // 用户消息右对齐
            holder.messageText.setBackgroundResource(R.drawable.user_message_bg);
        } else {
            params.gravity = Gravity.START; // AI消息左对齐
            holder.messageText.setBackgroundResource(R.drawable.ai_message_bg);
        }

        // 应用布局参数
        holder.messageText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}