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