    private void showEventsInCell(TextView eventsText, List<Event> dayEvents, View dayView) {
        // 功能：在日期单元格中显示当天的事件，最多显示指定数量的事件，并添加更多事件提示和无障碍支持
        StringBuilder eventsBuilder = new StringBuilder();
        int maxEventsToShow = 1; // 最多显示2个事件
        int maxCharsPerEvent = 3; // 每个事件最多显示15个字符

        for (int i = 0; i < Math.min(dayEvents.size(), maxEventsToShow); i++) {
            Event event = dayEvents.get(i);

            // 截断长文本
            String eventDesc = event.getDescription();
            if (eventDesc.length() > maxCharsPerEvent) {
                eventDesc = eventDesc.substring(0, maxCharsPerEvent) + "…";
            }

            // 添加时间
            String eventStr = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                eventStr = event.getStartTime() + " " + eventDesc;
            }
            eventsBuilder.append("• ").append(eventStr).append("\n");
        }

        // 添加更多事件提示
        if (dayEvents.size() > maxEventsToShow) {
            eventsBuilder.append("+")
                    .append(dayEvents.size() - maxEventsToShow)
                    .append("更多");
        }

        eventsText.setText(eventsBuilder.toString());

        // 添加无障碍支持
        StringBuilder fullDescription = new StringBuilder("包含" + dayEvents.size() + "个事件");
        for (Event e : dayEvents) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fullDescription.append("，").append(e.getStartTime()).append(e.getDescription());
            }
        }
        dayView.setContentDescription(fullDescription.toString());
    }

    public void addEvent(Event event) {
        // 功能：将新事件添加到日历中，并更新日历显示
        // 使用自定义的 Gson 实例
        Gson gson = GsonUtils.getGson();

        SharedPreferences prefs = requireActivity().getSharedPreferences(EVENTS_PREFS, Context.MODE_PRIVATE);
        String eventJson = prefs.getString(event.getDate().toString(), "[]");
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();
        List<Event> events = gson.fromJson(eventJson, type);

        // 添加新事件
        events.add(event);

        // 保存回SharedPreferences
        String updatedJson = gson.toJson(events);
        prefs.edit().putString(event.getDate().toString(), updatedJson).apply();

        updateCalendar(); // 更新日历显示
        Toast.makeText(getContext(), "事件已添加", Toast.LENGTH_SHORT).show();
    }

    private Map<LocalDate, List<Event>> loadAllEvents() {
        // 功能：加载所有事件，并过滤无效事件
        Map<LocalDate, List<Event>> eventsMap = new HashMap<>();
        SharedPreferences prefs = requireActivity().getSharedPreferences(EVENTS_PREFS, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        Gson gson = GsonUtils.getGson();
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                LocalDate date = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    date = LocalDate.parse(entry.getKey());
                }
                List<Event> events = gson.fromJson(entry.getValue().toString(), type);

                // 过滤无效事件
                List<Event> validEvents = new ArrayList<>();
                for (Event event : events) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (event.getDate() != null && event.getStartTime() != null && event.getEndTime() != null) {
                            validEvents.add(event);
                        }
                    }
                }

                eventsMap.put(date, validEvents);
            } catch (Exception e) {
                Log.e(TAG, "加载事件错误: " + entry.getKey(), e);
            }
        }
        return eventsMap;
    }