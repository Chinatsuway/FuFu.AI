
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Map<LocalDate, List<Event>> parseEvents(String aiResponse) {
        Map<LocalDate, List<Event>> eventsMap = new HashMap<>();
        if (aiResponse == null || aiResponse.isEmpty()) {
            return eventsMap;
        }

        // 日期格式器
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        Pattern datePattern = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日)");

        // 事件解析模式
        Pattern eventPattern = Pattern.compile(
                "(\\d{1,2})[:：](\\d{2})\\s*[-~—]\\s*(\\d{1,2})[:：](\\d{2})\\s*[:：]?\\s*(.+)"
        );

        String[] lines = aiResponse.split("\n");
        LocalDate currentDate = null;
        List<Event> currentEvents = null;

        for (String line : lines) {
            // 检查日期行
            Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find()) {
                try {
                    // 解析日期
                    String dateStr = dateMatcher.group(1);
                    currentDate = LocalDate.parse(dateStr, dateFormatter);

                    // 为新日期创建事件列表
                    currentEvents = new ArrayList<>();
                    eventsMap.put(currentDate, currentEvents);

                    // 跳过当前行继续处理
                    continue;
                } catch (Exception e) {
                    Log.e("AIFragment", "日期解析错误: " + line, e);
                }
            }

            // 如果当前日期已设置，尝试解析事件
            if (currentDate != null && currentEvents != null) {
                Matcher eventMatcher = eventPattern.matcher(line);
                if (eventMatcher.find()) {
                    try {
                        // 解析时间
                        int startHour = Integer.parseInt(Objects.requireNonNull(eventMatcher.group(1)));
                        int startMin = Integer.parseInt(Objects.requireNonNull(eventMatcher.group(2)));
                        int endHour = Integer.parseInt(Objects.requireNonNull(eventMatcher.group(3)));
                        int endMin = Integer.parseInt(Objects.requireNonNull(eventMatcher.group(4)));
                        String description = Objects.requireNonNull(eventMatcher.group(5)).trim();

                        // 创建事件
                        LocalTime startTime = LocalTime.of(startHour, startMin);
                        LocalTime endTime = LocalTime.of(endHour, endMin);
                        Event event = new Event(currentDate, startTime, endTime, description);

                        // 添加到当前日期的事件列表
                        currentEvents.add(event);
                    } catch (Exception e) {
                        Log.e("AIFragment", "事件解析错误: " + line, e);
                    }
                }
            }
        }

        return eventsMap;
    }
