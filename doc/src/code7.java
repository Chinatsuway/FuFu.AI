
    @Override
    public void onEventAdded(Event event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate eventDate = event.getDate();
            LocalDate today = LocalDate.now();

            Log.d(TAG, "收到事件添加通知: " + event.getDescription());
            Log.d(TAG, "事件日期: " + eventDate);
            Log.d(TAG, "今日日期: " + today);
            Log.d(TAG, "日期是否相等: " + eventDate.isEqual(today));

            if (eventDate.isEqual(today)) {
                Log.d(TAG, "事件属于今日，刷新UI");
                requireActivity().runOnUiThread(() -> {
                    refreshEvents();
                    Toast.makeText(getContext(), "今日日程已更新", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "用户手动刷新今日日程");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            refreshEvents();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateDate() {
        LocalDate today = LocalDate.now();
        dateTextView.setText(today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    void refreshEvents() {
        Log.d(TAG, "开始刷新今日日程");

        // 检查必要的视图和上下文是否初始化
        if (eventsRecyclerView == null || swipeRefreshLayout == null || getContext() == null) {
            Log.e(TAG, "视图未初始化，无法刷新");
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            return;
        }

        LocalDate today = LocalDate.now();
        List<Event> events = loadEventsForDate(today);
        events.sort(Comparator.comparing(Event::getStartTime)); // 使用新的 getter

        Log.d(TAG, "找到 " + events.size() + " 个今日事件");

        // 更新适配器数据
        eventsList.clear();
        eventsList.addAll(events);
        eventAdapter.notifyDataSetChanged();

        // 如果没有事件，显示空视图提示
        if (events.isEmpty()) {
            // 创建并设置空视图
            TextView emptyView = new TextView(getContext());
            emptyView.setText("今日无日程安排");
            emptyView.setContentDescription("今日无日程安排");
            emptyView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            emptyView.setTextSize(18);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(0, 32, 0, 32);

            // 添加到 RecyclerView 的父容器中
            if (eventsRecyclerView.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) eventsRecyclerView.getParent();
                // 移除之前的空视图（如果存在）
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChildAt(i) instanceof TextView) {
                        parent.removeViewAt(i);
                        break;
                    }
                }
                parent.addView(emptyView);
            }
        } else {
            // 移除空视图（如果存在）
            if (eventsRecyclerView.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) eventsRecyclerView.getParent();
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChildAt(i) instanceof TextView) {
                        parent.removeViewAt(i);
                        break;
                    }
                }
            }
        }

        updateDataSummary();
        // 停止刷新动画
        swipeRefreshLayout.setRefreshing(false);
        Log.d(TAG, "今日日程刷新完成");
    }
