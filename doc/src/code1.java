    public String callBlueHeartAIForAdvice(List<Map<String, String>> messages)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, JSONException {

        // 应用ID
        String appId = "2025510478"; 
        // 应用密钥
        String appKey = UserAppKey; 
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