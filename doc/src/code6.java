    private String getAIAdvice(String prompt) {
        try {
            // 创建消息列表
            AIFragment aiFragment = new AIFragment();
            List<Map<String, String>> messages = new ArrayList<>();

            // 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的日程管理助手，请根据用户提供的最近 7 天日程数据，提供专业、简洁、实用的建议。建议应包含时间管理、效率提升、健康提醒等方面。");
            messages.add(systemMessage);

            // 用户请求
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            // 发送请求 - 这里使用 AIFragment 中的请求逻辑
            return aiFragment.callBlueHeartAIForAdvice(messages);
        } catch (Exception e) {
            // 处理异常，返回错误信息
            return "获取建议时出错: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getRecentScheduleData() {
        // 尝试从缓存加载
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ADVICE, Context.MODE_PRIVATE);
        String cachedData = prefs.getString(KEY_LAST_ADVICE_DATA, null);

        if (cachedData != null) {
            // 检查缓存数据是否过期（超过 1 天）
            String lastDate = prefs.getString(KEY_LAST_ADVICE_DATE, "");
            LocalDate today = LocalDate.now();

            if (today.toString().equals(lastDate)) {
                return cachedData;
            }
        }
    }