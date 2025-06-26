public class AIFragment extends Fragment {

    // 用于用户输入的编辑框
    private EditText userInputET; 
    // 用于添加消息视图的线性布局
    private LinearLayout chatContainer; 
    // 聊天内容的滚动视图
    private ScrollView chatScrollView; 
    // 发送请求的按钮
    private Button sendButton; 
    // 主活动的引用
    private MainActivity mainActivity; 
    // 解析后的事件列表
    private List<Event> parsedEvents = new ArrayList<>(); 
    // 消息列表，包含用户和AI的消息
    private List<Map<String, String>> messagesList = new ArrayList<>(); 
    // 当前会话的ID
    private String currentSessionId; 
    // 选中的事件，键为事件ID，值为事件对象
    private Map<String, Event> selectedEvents = new HashMap<>(); 
    // 事件视图，键为事件ID，值为事件对应的视图
    private Map<String, View> eventViews = new HashMap<>(); 

    /**
     * 核心函数：创建Fragment的视图
     * @param inflater 用于填充布局的LayoutInflater
     * @param container 父视图组
     * @param savedInstanceState 保存的状态
     * @return 创建的视图
     */
    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 填充布局
        View view = inflater.inflate(R.layout.fragment_ai, container, false); 
        // 获取主活动的引用
        mainActivity = (MainActivity) getActivity(); 

        // 初始化视图
        userInputET = view.findViewById(R.id.userInputET);
        chatScrollView = view.findViewById(R.id.chatScrollView);
        chatContainer = view.findViewById(R.id.chatContainer);
        sendButton = view.findViewById(R.id.sendRequestBtn);

        // 设置默认提示词
        userInputET.setText("输入要求以让AI安排日程");

        // 设置发送按钮的点击事件
        sendButton.setOnClickListener(v -> sendAIRequest());

        // 初始化新会话
        initNewSession();

        return view;
    }

    /**
     * 初始化新会话，清空消息列表，生成新的会话ID，并添加系统提示消息
     */
    private void initNewSession() {
        // 清空消息列表
        messagesList.clear(); 
        // 生成新的会话ID
        currentSessionId = UUID.randomUUID().toString(); 

        // 添加系统提示消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个日程安排助手，请根据用户请求生成日程表，使用格式：HH:mm-HH:mm: 事件描述");
        messagesList.add(systemMessage);
    }

    /**
     * 调用BlueHeart AI获取建议，发送HTTP请求并处理响应
     * @param messages 消息列表
     * @return AI的回复内容
     * @throws IOException 网络请求异常
     * @throws NoSuchAlgorithmException 加密算法异常
     * @throws InvalidKeyException 密钥异常
     * @throws JSONException JSON解析异常
     */
    public String callBlueHeartAIForAdvice(List<Map<String, String>> messages)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, JSONException {

        // 应用ID
        String appId = "2025510478"; 
        // 应用密钥
        String appKey = "VLJsSSuMkjNDWLeV"; 
        // AI接口的URL
        String url = "https://api-ai.vivo.com.cn/vivogpt/completions"; 
        // 请求ID
        String requestId = UUID.randomUUID().toString(); 

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        JSONArray messagesArray = new JSONArray();

        // 将消息列表添加到请求体中
        for (Map<String, String> message : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", message.get("role"));
            msgObj.put("content", message.get("content"));
            messagesArray.put(msgObj);
        }

        requestBody.put("messages", messagesArray);
        requestBody.put("model", "vivo-BlueLM-TB-Pro");
        requestBody.put("sessionId", UUID.randomUUID().toString()); // 使用新的会话ID

        // 生成签名头
        Map<String, String> headers = generateAuthHeaders(appId, appKey, "POST", "/vivogpt/completions",
                "requestId=" + requestId);

        // 发送请求
        HttpURLConnection connection = (HttpURLConnection) new URL(url + "?requestId=" + requestId).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        // 设置请求头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 获取响应
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 解析响应内容
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getInt("code") == 0) {
                    return jsonResponse.getJSONObject("data").getString("content");
                } else {
                    String errorMsg = jsonResponse.getString("msg");
                    throw new IOException("AI服务错误: " + errorMsg);
                }
            }
        } else {
            // 读取错误流
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                throw new IOException("HTTP错误: " + responseCode + "\n" + errorResponse);
            }
        }
    }

    /**
     * 将消息添加到聊天界面
     * @param role 消息的角色（用户或AI）
     * @param content 消息的内容
     */
    private void addMessageToChat(String role, String content) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View messageView;

        if ("user".equals(role)) {
            // 用户消息布局
            messageView = inflater.inflate(R.layout.user_message_layout, chatContainer, false);
            TextView userMessage = messageView.findViewById(R.id.userMessageTV);
            userMessage.setText(content);
        } else {
            // AI回复布局
            messageView = inflater.inflate(R.layout.ai_response_layout, chatContainer, false);
            TextView aiMessage = messageView.findViewById(R.id.aiMessageTV);
            LinearLayout eventsContainer = messageView.findViewById(R.id.eventsContainer);
            Button addBtn = messageView.findViewById(R.id.addToCalendarBtn);

            // 尝试解析事件
            List<Event> events;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                events = parseEvents(content);
            } else {
                events = new ArrayList<>();
            }

            if (!events.isEmpty()) {
                // 隐藏原始文本，显示事件列表
                aiMessage.setVisibility(View.GONE);
                eventsContainer.setVisibility(View.VISIBLE);
                addBtn.setVisibility(View.VISIBLE);

                // 添加事件到容器
                addEventsToContainer(eventsContainer, events);

                // 设置添加按钮点击事件
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    addBtn.setOnClickListener(v -> addSelectedEventsToCalendar(events));
                }
            } else {
                // 没有解析出事件，显示原始文本
                aiMessage.setVisibility(View.VISIBLE);
                eventsContainer.setVisibility(View.GONE);
                addBtn.setVisibility(View.GONE);
                aiMessage.setText(content);
            }
        }

        // 将消息视图添加到聊天容器
        chatContainer.addView(messageView);
        // 滚动到聊天界面底部
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * 将事件添加到事件容器中
     * @param container 事件容器
     * @param events 事件列表
     */
    private void addEventsToContainer(LinearLayout container, List<Event> events) {
        // 清空容器和视图映射
        container.removeAllViews();
        eventViews.clear();
        selectedEvents.clear();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Event event : events) {
            // 填充事件布局
            View eventView = inflater.inflate(R.layout.event_item_layout, container, false);

            // 获取事件的时间和描述视图
            TextView timeTV = eventView.findViewById(R.id.eventTimeTV);
            TextView descTV = eventView.findViewById(R.id.eventDescTV);
            ImageView selectBtn = eventView.findViewById(R.id.selectBtn);
            ImageView editBtn = eventView.findViewById(R.id.editBtn);

            // 设置事件信息
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                timeTV.setText(event.getStartTime() + " - " + event.getEndTime());
            }
            descTV.setText(event.getDescription());

            // 设置选择按钮点击事件
            selectBtn.setOnClickListener(v -> {
                if (selectedEvents.containsKey(event.getId())) {
                    // 取消选择
                    selectBtn.setImageResource(R.drawable.ic_checkbox_unchecked);
                    selectedEvents.remove(event.getId());
                } else {
                    // 选择事件
                    selectBtn.setImageResource(R.drawable.ic_checkbox_checked);
                    selectedEvents.put(event.getId(), event);
                }
            });

            // 设置编辑按钮点击事件
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                editBtn.setOnClickListener(v -> editEvent(event, eventView));
            }

            // 保存视图引用
            eventViews.put(event.getId(), eventView);

            // 将事件视图添加到容器
            container.addView(eventView);
        }
    }

    /**
     * 编辑事件信息
     * @param event 要编辑的事件
     * @param eventView 事件对应的视图
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void editEvent(Event event, View eventView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("编辑事件");

        // 创建编辑视图
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.event_edit_dialog, null);
        EditText startTimeET = dialogView.findViewById(R.id.editStartTime);
        EditText endTimeET = dialogView.findViewById(R.id.editEndTime);
        EditText descET = dialogView.findViewById(R.id.editDescription);

        // 设置当前值
        startTimeET.setText(event.getStartTime());
        endTimeET.setText(event.getEndTime());
        descET.setText(event.getDescription());

        builder.setView(dialogView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            try {
                // 解析新时间
                LocalTime newStartTime = LocalTime.parse(startTimeET.getText().toString());
                LocalTime newEndTime = LocalTime.parse(endTimeET.getText().toString());
                String newDesc = descET.getText().toString().trim();

                if (newDesc.isEmpty()) {
                    Toast.makeText(getContext(), "事件内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newEndTime.isBefore(newStartTime)) {
                    Toast.makeText(getContext(), "结束时间不能早于开始时间", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 更新事件
                event.setStartTime(newStartTime);
                event.setEndTime(newEndTime);
                event.setDescription(newDesc);

                // 更新UI
                TextView timeTV = eventView.findViewById(R.id.eventTimeTV);
                TextView descTV = eventView.findViewById(R.id.eventDescTV);

                timeTV.setText(event.getStartTime() + " - " + event.getEndTime());
                descTV.setText(event.getDescription());

                Toast.makeText(getContext(), "事件已更新", Toast.LENGTH_SHORT).show();
            } catch (DateTimeParseException e) {
                Toast.makeText(getContext(), "时间格式不正确，请使用HH:mm格式", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 将选中的事件添加到日历中
     * @param allEvents 所有事件列表
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void addSelectedEventsToCalendar(List<Event> allEvents) {
        if (selectedEvents.isEmpty()) {
            Toast.makeText(getContext(), "请至少选择一个事件", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取要添加的事件列表
        List<Event> eventsToAdd = new ArrayList<>(selectedEvents.values());

        // 保存事件
        int addedCount = 0;
        for (Event event : eventsToAdd) {
            try {
                saveEventToStorage(event);
                addedCount++;
            } catch (Exception e) {
                Log.e("AIFragment", "添加事件失败", e);
            }
        }

        if (addedCount > 0) {
            Toast.makeText(getContext(), "已添加 " + addedCount + " 个事件到日历", Toast.LENGTH_SHORT).show();

            // 刷新日历
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).notifyCalendarUpdate();
            }
        }

        // 重置选择状态
        selectedEvents.clear();
        for (View view : eventViews.values()) {
            ImageView selectBtn = view.findViewById(R.id.selectBtn);
            selectBtn.setImageResource(R.drawable.ic_checkbox_unchecked);
        }
    }

    /**
     * 核心函数：发送AI请求
     */
    @SuppressLint("SetTextI18n")
    private void sendAIRequest() {
        // 获取用户输入的内容
        String prompt = userInputET.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(getContext(), "请输入请求内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加用户消息到UI
        addMessageToChat("user", prompt);

        // 添加格式提示（仅首次请求）
        if (messagesList.size() == 1) { // 只有系统消息
            prompt += "\n请使用以下格式回复：\n08:00-08:30: 事件1\n09:00-10:30: 事件2\n14:00-15:30: 事件3\n";
        }

        // 创建用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesList.add(userMessage);

        // 修剪消息历史
        trimMessagesHistory();

        // 执行异步任务发送请求
        new AIRequestTask().execute();
        // 清空输入框
        userInputET.setText(""); 
    }

    /**
     * 获取解析后的事件列表
     * @return 解析后的事件列表
     */
    public List<Event> getParsedEvents() {
        return parsedEvents;
    }

    /**
     * 设置解析后的事件列表
     * @param parsedEvents 解析后的事件列表
     */
    public void setParsedEvents(List<Event> parsedEvents) {
        this.parsedEvents = parsedEvents;
    }

    /**
     * 异步任务类，用于发送AI请求
     */
    private class AIRequestTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 调用AI接口获取回复
                return callBlueHeartAI();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // 添加AI回复到UI
            addMessageToChat("assistant", result);

            if (!result.startsWith("Error:")) {
                // 添加AI回复到消息列表
                Map<String, String> assistantMessage = new HashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", result);
                messagesList.add(assistantMessage);
                // 修剪消息历史
                trimMessagesHistory();
            }
        }
    }

    /**
     * 解析AI响应中的事件信息
     * @param aiResponse AI的响应内容
     * @return 解析后的事件列表
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<Event> parseEvents(String aiResponse) {
        Log.d("AIFragment", "开始解析AI响应: " + aiResponse);

        List<Event> events = new ArrayList<>();
        String[] lines = aiResponse.split("\n");
        int eventCount = 0;
        LocalDate date = LocalDate.now();

        // 尝试从响应中提取日期
        Pattern datePattern = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{2}-\\d{2})");
        Matcher dateMatcher = datePattern.matcher(aiResponse);
        if (dateMatcher.find()) {
            try {
                String dateStr = dateMatcher.group();
                // 处理不同日期格式
                if (dateStr.contains("年")) {
                    date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
                } else {
                    date = LocalDate.parse(dateStr);
                }
                Log.d("AIFragment", "从响应中解析到日期: " + date);
            } catch (Exception e) {
                Log.e("AIFragment", "日期解析错误: " + e.getMessage());
                Toast.makeText(getContext(), "日期解析错误，使用默认日期", Toast.LENGTH_SHORT).show();
            }
        }

        // 事件解析模式 - 支持多种时间格式
        Pattern eventPattern = Pattern.compile(
                "(\\d{1,2})[:：](\\d{2})\\s*[-~—]\\s*(\\d{1,2})[:：](\\d{2})\\s*[:：]?\\s*(.+)"
        );

        for (String line : lines) {
            Matcher matcher = eventPattern.matcher(line);
            if (matcher.find()) {
                try {
                    // 解析时间
                    int startHour = Integer.parseInt(matcher.group(1));
                    int startMin = Integer.parseInt(matcher.group(2));
                    int endHour = Integer.parseInt(matcher.group(3));
                    int endMin = Integer.parseInt(matcher.group(4));
                    String description = matcher.group(5).trim();

                    // 创建时间对象
                    LocalTime startTime = LocalTime.of(startHour, startMin);
                    LocalTime endTime = LocalTime.of(endHour, endMin);

                    // 创建事件
                    Event event = new Event(date, startTime, endTime, description);
                    events.add(event);
                    eventCount++;

                    Log.d("AIFragment", "解析到事件: " + event);

                } catch (Exception e) {
                    Log.e("AIFragment", "事件解析错误: " + line, e);
                }
            } else {
                // 尝试其他格式：没有冒号的时间格式 (如 0800-0930)
                Pattern altPattern = Pattern.compile("(\\d{2})(\\d{2})\\s*[-~—]\\s*(\\d{2})(\\d{2})\\s*[:：]?\\s*(.+)");
                Matcher altMatcher = altPattern.matcher(line);
                if (altMatcher.find()) {
                    try {
                        int startHour = Integer.parseInt(altMatcher.group(1));
                        int startMin = Integer.parseInt(altMatcher.group(2));
                        int endHour = Integer.parseInt(altMatcher.group(3));
                        int endMin = Integer.parseInt(altMatcher.group(4));
                        String description = altMatcher.group(5).trim();

                        LocalTime startTime = LocalTime.of(startHour, startMin);
                        LocalTime endTime = LocalTime.of(endHour, endMin);

                        Event event = new Event(date, startTime, endTime, description);
                        events.add(event);
                        eventCount++;

                        Log.d("AIFragment", "解析到事件(替代格式): " + event);
                    } catch (Exception e) {
                        Log.e("AIFragment", "替代格式事件解析错误: " + line, e);
                    }
                }
            }
        }

        Log.d("AIFragment", "成功解析 " + events.size() + " 个事件");
        return events;
    }

    /**
     * 调用BlueHeart AI获取回复
     * @return AI的回复内容
     * @throws IOException 网络请求异常
     * @throws NoSuchAlgorithmException 加密算法异常
     * @throws InvalidKeyException 密钥异常
     * @throws JSONException JSON解析异常
     */
    private String callBlueHeartAI() throws IOException, NoSuchAlgorithmException, InvalidKeyException, JSONException {
        // 应用ID
        String appId = "2025510478"; 
        // 应用密钥
        String appKey = "VLJsSSuMkjNDWLeV"; 
        // AI接口的URL
        String url = "https://api-ai.vivo.com.cn/vivogpt/completions"; 
        // 请求ID
        String requestId = UUID.randomUUID().toString(); 

        // 构建包含对话历史的请求体
        JSONObject requestBody = new JSONObject();

        // 添加对话历史
        JSONArray messagesArray = new JSONArray();
        for (Map<String, String> message : messagesList) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", message.get("role"));
            msgObj.put("content", message.get("content"));
            messagesArray.put(msgObj);
        }
        requestBody.put("messages", messagesArray);

        // 添加其他参数
        requestBody.put("model", "vivo-BlueLM-TB-Pro");
        requestBody.put("sessionId", currentSessionId);

        // 生成签名头
        Map<String, String> headers = generateAuthHeaders(appId, appKey, "POST", "/vivogpt/completions",
                "requestId=" + requestId);

        // 发送请求
        HttpURLConnection connection = (HttpURLConnection) new URL(url + "?requestId=" + requestId).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        // 设置请求头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 获取响应
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 解析响应内容
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getInt("code") == 0) {
                    return jsonResponse.getJSONObject("data").getString("content");
                } else {
                    String errorMsg = jsonResponse.getString("msg");
                    Log.e("AIFragment", "AI响应错误: " + errorMsg);
                    return "AI服务错误: " + errorMsg;
                }
            }
        } else {
            // 读取错误流获取详细错误信息
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                String errorMsg = "HTTP错误: " + responseCode + "\n" + errorResponse;
                Log.e("AIFragment", errorMsg);
                return errorMsg;
            }
        }
    }

    /**
     * 从JSON响应中提取内容
     * @param jsonResponse JSON响应字符串
     * @return 提取的内容
     */
    private String extractContentFromResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if (jsonObject.has("data")) {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data.has("content")) {
                    return data.getString("content");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 生成认证请求头
     * @param appId 应用ID
     * @param appKey 应用密钥
     * @param method 请求方法
     * @param uri 请求的URI
     * @param queryParams 查询参数
     * @return 包含认证信息的请求头
     * @throws NoSuchAlgorithmException 加密算法异常
     * @throws InvalidKeyException 密钥异常
     * @throws IOException 输入输出异常
     */
    private Map<String, String> generateAuthHeaders(String appId, String appKey, String method,
                                                    String uri, String queryParams) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        // 生成随机字符串
        String nonce = generateRandomString(8); 
        // 获取当前时间戳
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000); 

        // 构造签名字符串
        String signingString = method.toUpperCase() + "\n" +
                uri + "\n" +
                queryParams + "\n" +
                appId + "\n" +
                timestamp + "\n" +
                "x-ai-gateway-app-id:" + appId + "\n" +
                "x-ai-gateway-timestamp:" + timestamp + "\n" +
                "x-ai-gateway-nonce:" + nonce;

        // 计算签名
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(appKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        String signature = android.util.Base64.encodeToString(sha256_HMAC.doFinal(signingString.getBytes(StandardCharsets.UTF_8)), android.util.Base64.NO_WRAP);

        // 返回请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("X-AI-GATEWAY-APP-ID", appId);
        headers.put("X-AI-GATEWAY-TIMESTAMP", timestamp);
        headers.put("X-AI-GATEWAY-NONCE", nonce);
        headers.put("X-AI-GATEWAY-SIGNED-HEADERS", "x-ai-gateway-app-id;x-ai-gateway-timestamp;x-ai-gateway-nonce");
        headers.put("X-AI-GATEWAY-SIGNATURE", signature);

        return headers;
    }

    /**
     * 生成指定长度的随机字符串
     * @param length 字符串长度
     * @return 随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 将事件添加到日历中
     * @param events 事件列表
     */
    // 直接保存事件到 SharedPreferences
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void addEventsToCalendar(List<Event> events) {
        if (events.isEmpty()) {
            Toast.makeText(getContext(), "未找到可添加的事件", Toast.LENGTH_SHORT).show();
            return;
        }

        int addedCount = 0;

        for (Event event : events) {
            try {
                // 确保日期是有效的
                if (event.getDate() == null) {
                    Log.e("AIFragment", "事件日期为空: " + event);
                    continue;
                }

                // 保存事件
                saveEventToStorage(event);
                addedCount++;

                Log.d("AIFragment", "事件已添加到日历: " + event);
            } catch (Exception e) {
                Log.e("AIFragment", "添加事件失败: " + event, e);
            }
        }

        if (addedCount > 0) {
            Toast.makeText(getContext(), "已添加 " + addedCount + " 个事件到日历", Toast.LENGTH_SHORT).show();

            // 强制刷新日历视图
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).notifyCalendarUpdate();
            }
        } else {
            Toast.makeText(getContext(), "没有事件被添加", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将事件保存到SharedPreferences中
     * @param event 要保存的事件
     */
    private void saveEventToStorage(Event event) {
        try {
            if (getActivity() == null) return;

            // 获取SharedPreferences实例
            SharedPreferences prefs = getActivity().getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
            // 以日期作为键
            String dateKey = event.getDate().toString(); 

            // 使用自定义的Gson实例
            Gson gson = GsonUtils.getGson();
            Type type = new TypeToken<ArrayList<Event>>(){}.getType();

            // 获取现有事件
            String eventJson = prefs.getString(dateKey, "[]");
            List<Event> events = gson.fromJson(eventJson, type);
            if (events == null) events = new ArrayList<>();

            // 添加新事件
            events.add(event);

            // 保存回SharedPreferences
            String updatedJson = gson.toJson(events);
            prefs.edit().putString(dateKey, updatedJson).apply();

            Log.d("AIFragment", "事件已保存到存储: " + event);

            // 通知刷新UI
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).notifyEventAdded();
            }
        } catch (Exception e) {
            Log.e("AIFragment", "保存事件到存储失败: " + event, e);
            Toast.makeText(getContext(), "保存事件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 修剪消息历史，防止消息列表过长
     */
    private void trimMessagesHistory() {
        Gson gson = new Gson();
        int maxLength = 6000; // 保留安全边界

        while (gson.toJson(messagesList).length() > maxLength && messagesList.size() > 2) {
            // 移除最早的一对对话（用户+AI）
            messagesList.remove(0); // 用户消息
            messagesList.remove(0); // AI回复
        }
    }
}