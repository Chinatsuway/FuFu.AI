
    private String getAIAdvice(String prompt) {
        try {
            // 创建消息列表
            AIFragment aiFragment = new AIFragment();
            List<Map<String, String>> messages = new ArrayList<>();

            // 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的日程管理助手，请根据用户提供的最近7天日程数据，提供专业、简洁、实用的建议。建议应包含时间管理、效率提升、健康提醒等方面。");
            messages.add(systemMessage);

            // 用户请求
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            // 发送请求 - 这里使用AIFragment中的请求逻辑
            return aiFragment.callBlueHeartAIForAdvice(messages);
        } catch (Exception e) {
            return "获取建议时出错: " + e.getMessage();
        }
    }
