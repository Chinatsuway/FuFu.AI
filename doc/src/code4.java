    // 在单元格中显示事件的辅助方法
    private void showEventsInCell(TextView eventsText, List<Event> dayEvents, View dayView) {
        StringBuilder eventsBuilder = new StringBuilder();
        int maxEventsToShow = 1; // 最多显示1个事件
        int maxCharsPerEvent = 2;

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
            eventsBuilder.append(eventStr);
        }

        // 添加更多事件提示
        if (dayEvents.size() > maxEventsToShow) {
            eventsBuilder.append("\n+")
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