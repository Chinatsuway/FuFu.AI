// MainActivity.java
package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.ChatAdapter;
import com.example.myapplication.model.Message;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // 配置参数
    private static final String APP_ID = "2025457933";
    private static final String APP_KEY = "KGwXteyDCcJJYaCS";
    private static final String API_URL = "https://api-ai.vivo.com.cn/vivogpt/completions";
    private static final int CALENDAR_PERMISSION_CODE = 1001;
    private static final String CALENDAR_SCOPE = "scope.addPhoneCalendar";

    // UI组件
    private RecyclerView chatRecyclerView;
    private EditText inputEditText;
    private Button sendButton;
    private Button btnAnalyzeSchedule;
    private LinearLayout buttonsLayout;

    // 数据相关
    private final List<Message> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

    }

    // 初始化视图组件
    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputEditText = findViewById(R.id.inputEditText);
        sendButton = findViewById(R.id.sendButton);
        btnAnalyzeSchedule = findViewById(R.id.btnAnalyzeSchedule);
        buttonsLayout = findViewById(R.id.buttonsLayout);

        // 初始化聊天列表
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // 设置按钮点击监听
        sendButton.setOnClickListener(v -> handleUserInput());
        btnAnalyzeSchedule.setOnClickListener(v -> handleScheduleAnalysis());
    }

    // 处理用户输入
    private void handleUserInput() {
        String input = inputEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(input)) {
            addUserMessage(input);
            processNaturalLanguage(input);
            inputEditText.setText("");
        }
    }

    // 处理日程分析请求
    private void handleScheduleAnalysis() {
        checkCalendarPermissionWithAuth(() -> {
            String eventsData = getRecentCalendarEvents();
            sendScheduleAnalysisRequest(eventsData);
        });
    }

    // region 自然语言处理核心逻辑
    private void processNaturalLanguage(String text) {
        new Thread(() -> {
            try {
                JSONObject requestBody = buildBaseRequest().put("prompt", text);
                String response = sendVivoRequest(requestBody.toString());
                processAIResponse(response);
            } catch (Exception e) {
                showError("请求失败: " + e.getMessage());
            }
        }).start();
    }

    private JSONObject buildBaseRequest() throws JSONException {
        return new JSONObject()
                .put("model", "vivo-BlueLM-TB-Pro")
                .put("sessionId", UUID.randomUUID().toString())
                .put("extra", createExtraParams());
    }

    private JSONObject createExtraParams() throws JSONException {
        return new JSONObject()
                .put("temperature", 0.9)
                .put("top_p", 0.7)
                .put("max_new_tokens", 1024);
    }
    // endregion

    // region 日历操作核心逻辑
    private static class ScheduleEvent {
        String title;
        long startTime; // 使用Unix时间戳（秒）
        long endTime;
        boolean isRepeat;
    }

    // 获取近期日程
    private String getRecentCalendarEvents() {

    }

    // 发送分析请求
    private void sendScheduleAnalysisRequest(String eventsData) {
        try {
            JSONObject requestBody = buildBaseRequest()
                    .put("prompt", "分析并优化以下日程：\n" + eventsData);
            String response = sendVivoRequest(requestBody.toString());
            parseScheduleResponse(response);
        } catch (Exception e) {
            showError("日程分析失败：" + e.getMessage());
        }
    }

    // 解析AI响应
    private void parseScheduleResponse(String response) {
        runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(response);
                if (json.getInt("code") == 0) {
                    String content = json.getJSONObject("data").getString("content");
                    showScheduleOptions(content);
                }
            } catch (JSONException e) {
                showError("响应解析失败");
            }
        });
    }

    // 显示可添加的日程按钮
    private void showScheduleOptions(String content) {
        buttonsLayout.removeAllViews();
        String[] suggestions = content.split("\n");
        for (String suggestion : suggestions) {
            if (suggestion.contains("@")) {
                String[] parts = suggestion.split("@");
                if (parts.length == 2) {
                    createScheduleButton(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }

    // 创建可点击的添加按钮
    private void createScheduleButton(String title, String timeRange) {
        Button button = new Button(this);
        button.setText(String.format("添加：%s %s", title, timeRange));
        button.setOnClickListener(v -> {
            ScheduleEvent event = parseScheduleEvent(title, timeRange);
            if (event != null) {
                addCalendarEvent(event);
            }
        });
        buttonsLayout.addView(button);
    }

    // 解析日程事件
    private ScheduleEvent parseScheduleEvent(String title, String timeRange) {

    }

    // 添加日程到日历
    private void addCalendarEvent(ScheduleEvent event) {

    }
    // endregion

    // region 权限管理
    private void checkCalendarPermissionWithAuth(Runnable onSuccess) {

    }
    // endregion

    // region 辅助方法
    private void addUserMessage(String message) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, true));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        });
    }

    private void addAiMessage(String message) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, false));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        });
    }

    private void processAIResponse(String response) {
        runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(response);
                if (json.getInt("code") == 0) {
                    String content = json.getJSONObject("data").getString("content");
                    addAiMessage(content);
                }
            } catch (JSONException e) {
                showError("响应解析失败");
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    // endregion

    // region 网络请求
    private String sendVivoRequest(String jsonBody) throws Exception {

    }
    // endregion
}