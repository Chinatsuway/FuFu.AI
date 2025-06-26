/**
 * TodayFragment 类用于显示今日的日程安排，同时提供日程管理和 AI 建议功能。
 */
public class TodayFragment extends Fragment implements MainActivity.OnEventAddedListener,
        SwipeRefreshLayout.OnRefreshListener, EventAdapter.OnItemClickListener {
    private static final String TAG = "TodayFragment";
    private LinearLayout eventsContainer; // 事件容器布局
    private SwipeRefreshLayout swipeRefreshLayout; // 下拉刷新布局
    private TextView dateTextView; // 显示日期的文本视图

    private RecyclerView eventsRecyclerView; // 用于显示事件列表的 RecyclerView
    private EventAdapter eventAdapter; // RecyclerView 的适配器
    private List<Event> eventsList = new ArrayList<>(); // 存储事件的列表

    private ExecutorService executorService; // 线程池，用于执行异步任务
    private static final int AI_ADVICE_REQUEST_CODE = 1001; // AI 建议请求的代码

    private TextView tasksRemaining; // 显示剩余任务数量的文本视图
    private TextView tasksCompleted; // 显示已完成任务数量的文本视图
    private CircularProgressView progressCircle; // 显示任务完成进度的圆形进度条
    private TextView progressText; // 显示任务完成百分比的文本视图
    private TextView aiAdviceText; // 显示 AI 建议的文本视图
    private ImageButton refreshAdviceBtn; // 刷新 AI 建议的按钮
    private ImageView addEventBtn; // 添加事件的按钮

    private static final String PREFS_ADVICE = "advice_prefs"; // 存储 AI 建议的共享偏好文件名
    private static final String KEY_LAST_ADVICE = "last_advice"; // 存储最后一次 AI 建议的键
    private static final String KEY_LAST_ADVICE_DATE = "last_advice_date"; // 存储最后一次 AI 建议日期的键
    private static final String KEY_LAST_ADVICE_DATA = "last_advice_data"; // 存储最后一次 AI 建议原始数据的键

    /**
     * 核心函数：创建并返回该 Fragment 的视图。
     * 在该函数中进行视图的初始化，包括 RecyclerView、按钮点击事件的设置等。
     *
     * @param inflater           用于将布局文件转换为视图的 LayoutInflater
     * @param container          父视图组
     * @param savedInstanceState 保存的实例状态
     * @return 创建的视图
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 膨胀 fragment_today 布局文件
        View view = inflater.inflate(R.layout.fragment_today, container, false);
        // 初始化线程池，用于执行异步任务
        executorService = Executors.newSingleThreadExecutor();
        // 初始化新组件
        tasksRemaining = view.findViewById(R.id.tasks_remaining); // 查找显示剩余任务数量的文本视图
        tasksCompleted = view.findViewById(R.id.tasks_completed); // 查找显示已完成任务数量的文本视图
        progressCircle = view.findViewById(R.id.progress_circle); // 查找显示任务完成进度的圆形进度条
        progressText = view.findViewById(R.id.progress_text); // 查找显示任务完成百分比的文本视图
        aiAdviceText = view.findViewById(R.id.ai_advice_text); // 查找显示 AI 建议的文本视图
        refreshAdviceBtn = view.findViewById(R.id.refresh_advice_btn); // 查找刷新 AI 建议的按钮
        addEventBtn = view.findViewById(R.id.add_event_btn); // 查找添加事件的按钮

        // 初始化视图
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view); // 查找用于显示事件列表的 RecyclerView
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout); // 查找下拉刷新布局
        dateTextView = view.findViewById(R.id.today_date); // 查找显示日期的文本视图

        // 设置 RecyclerView
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // 设置 RecyclerView 的布局管理器
        eventAdapter = new EventAdapter(eventsList, this); // 创建 RecyclerView 的适配器
        eventsRecyclerView.setAdapter(eventAdapter); // 为 RecyclerView 设置适配器

        // 设置添加事件按钮点击事件
        addEventBtn.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                // 显示事件编辑 Fragment
                ((MainActivity) getActivity()).showEventEditorFragment(
                        LocalDate.now(),
                        TodayFragment.class.getSimpleName() // 添加来源标记
                );
            }
        });

        // 设置刷新建议按钮点击事件
        refreshAdviceBtn.setOnClickListener(v -> refreshAdvice());

        // 添加滑动删除支持
        setupItemTouchHelper();

        swipeRefreshLayout.setOnRefreshListener(this); // 设置下拉刷新监听器
        updateDate(); // 更新日期显示

        loadCachedAdvice(); // 加载缓存的 AI 建议
        // 注意：这里不再直接调用 refreshEvents()，改为在 onViewCreated 中调用

        return view;
    }

    /**
     * 加载缓存的 AI 建议，如果缓存的建议日期与当前日期不同，则重新获取建议。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadCachedAdvice() {
        // 获取存储 AI 建议的共享偏好
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ADVICE, Context.MODE_PRIVATE);
        // 获取缓存的建议
        String cachedAdvice = prefs.getString(KEY_LAST_ADVICE, "");
        // 获取最后一次建议的日期
        String lastDate = prefs.getString(KEY_LAST_ADVICE_DATE, "");

        if (!cachedAdvice.isEmpty()) {
            // 如果缓存的建议不为空，则显示缓存的建议
            aiAdviceText.setText(cachedAdvice);
        }

        // 检查是否需要刷新
        LocalDate today = LocalDate.now();
        if (!today.toString().equals(lastDate)) {
            // 如果日期不同，则重新获取 AI 建议
            fetchAIAdvice();
        }
    }

    /**
     * 核心函数：异步获取 AI 建议。
     * 首先获取最近 7 天的日程数据，然后构建 AI 请求提示，发送请求并更新 UI。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void fetchAIAdvice() {
        // 设置提示文本
        aiAdviceText.setText("正在分析您的日程，请稍候...");

        // 在线程池中执行异步任务
        executorService.execute(() -> {
            try {
                // 1. 获取最近 7 天的日程数据
                String scheduleData = getRecentScheduleData();

                // 2. 构建 AI 请求提示
                String prompt = "请根据我最近 7 天的日程安排为我提供一些建议。以下是我的日程数据：\n\n" + scheduleData;

                // 3. 发送 AI 请求
                String advice = getAIAdvice(prompt);

                // 4. 更新 UI 并永久化存储结果
                requireActivity().runOnUiThread(() -> {
                    // 设置 AI 建议文本
                    aiAdviceText.setText(advice);
                    // 保存建议和日程数据
                    saveAdvice(advice, scheduleData);
                });
            } catch (Exception e) {
                // 处理异常，更新 UI 显示错误信息
                requireActivity().runOnUiThread(() -> {
                    aiAdviceText.setText("获取建议失败: " + e.getMessage());
                });
                Log.e("TodayFragment", "获取 AI 建议失败", e);
            }
        });
    }

    /**
     * 发送 AI 请求，获取 AI 建议。
     *
     * @param prompt AI 请求的提示信息
     * @return AI 给出的建议
     */
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

    /**
     * 获取最近 7 天的日程数据，如果缓存数据未过期则使用缓存数据，否则重新生成。
     *
     * @return 最近 7 天的日程数据
     */
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

        // 缓存不存在或过期，创建新数据
        StringBuilder data = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<Event> events = loadEventsForDate(date);

            data.append(date.format(dateFormatter)).append(":\n");

            if (events.isEmpty()) {
                data.append("  无日程\n");
            } else {
                for (Event event : events) {
                    data.append("  ")
                            .append(event.getStartTime()).append("-").append(event.getEndTime())
                            .append(": ").append(event.getDescription())
                            .append("\n");
                }
            }
            data.append("\n");
        }

        return data.toString();
    }

    /**
     * 保存 AI 建议和日程数据到共享偏好中。
     *
     * @param advice       AI 建议
     * @param scheduleData 日程数据
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveAdvice(String advice, String scheduleData) {
        // 获取存储 AI 建议的共享偏好
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ADVICE, Context.MODE_PRIVATE);
        // 保存建议、日程数据和日期
        prefs.edit()
                .putString(KEY_LAST_ADVICE, advice)
                .putString(KEY_LAST_ADVICE_DATA, scheduleData)
                .putString(KEY_LAST_ADVICE_DATE, LocalDate.now().toString())
                .apply();
    }

    /**
     * 检查是否需要刷新 AI 建议，如果需要则根据情况重新生成建议或获取新建议。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void refreshAdviceIfNeeded() {
        // 获取存储 AI 建议的共享偏好
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ADVICE, Context.MODE_PRIVATE);
        // 获取最后一次建议的日期
        String lastDate = prefs.getString(KEY_LAST_ADVICE_DATE, "");
        LocalDate today = LocalDate.now();

        if (!today.toString().equals(lastDate)) {
            // 如果数据存在，使用缓存数据重新生成建议（避免重复获取数据）
            String cachedData = prefs.getString(KEY_LAST_ADVICE_DATA, "");
            if (!cachedData.isEmpty()) {
                generateAdviceFromCachedData(cachedData);
            } else {
                fetchAIAdvice();
            }
        }
    }

    /**
     * 设置每日刷新 AI 建议的闹钟，在每天凌晨 00:05 触发。
     *
     * @param context 上下文
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void scheduleDailyRefresh(Context context) {
        // 获取 AlarmManager 服务
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // 创建广播意图
        Intent intent = new Intent(context, DailyRefreshReceiver.class);
        intent.setAction("com.example.calendarapp.ACTION_DAILY_REFRESH");

        // 创建 PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                AI_ADVICE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 取消之前的闹钟
        alarmManager.cancel(pendingIntent);

        // 设置明天凌晨的触发时间
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime tomorrowMidnight = now.plusDays(1)
                .truncatedTo(ChronoUnit.DAYS)
                .withHour(0)
                .withMinute(5); // 00:05 避免在午夜高峰

        long triggerAtMillis = tomorrowMidnight.toInstant().toEpochMilli();

        // 使用精确的闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }

        Log.d("TodayFragment", "已设置每日刷新: " + tomorrowMidnight);
    }

    /**
     * 使用缓存的日程数据重新生成 AI 建议。
     *
     * @param scheduleData 缓存的日程数据
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void generateAdviceFromCachedData(String scheduleData) {
        // 设置提示文本
        aiAdviceText.setText("正在重新生成建议...");

        // 在线程池中执行异步任务
        executorService.execute(() -> {
            try {
                // 使用缓存数据生成新建议
                String prompt = "请根据我最近 7 天的日程安排为我提供一些建议。以下是我的日程数据：\n\n" + scheduleData;
                String advice = getAIAdvice(prompt);

                // 更新 UI
                requireActivity().runOnUiThread(() -> {
                    aiAdviceText.setText(advice);
                    // 更新建议但保留原始数据
                    SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ADVICE, Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString(KEY_LAST_ADVICE, advice)
                            .putString(KEY_LAST_ADVICE_DATE, LocalDate.now().toString())
                            .apply();
                });
            } catch (Exception e) {
                // 处理异常，更新 UI 显示错误信息
                requireActivity().runOnUiThread(() -> {
                    aiAdviceText.setText("重新生成建议失败: " + e.getMessage());
                });
                Log.e("TodayFragment", "重新生成建议失败", e);
            }
        });
    }

    /**
     * 刷新 AI 建议，调用 fetchAIAdvice 方法重新获取建议。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void refreshAdvice() {
        fetchAIAdvice();
    }

    /**
     * 在视图创建完成后调用，确保视图完全初始化后再刷新数据。
     *
     * @param view               创建的视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 确保视图完全初始化后再刷新数据
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            refreshEvents();
        }
    }

    /**
     * 设置 RecyclerView 的滑动删除功能。
     */
    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Event event = eventAdapter.getEventAt(position);
                deleteEvent(event);
                eventAdapter.removeEvent(position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(eventsRecyclerView);
    }

    /**
     * 处理 RecyclerView 中事件的删除点击事件。
     *
     * @param position 事件在列表中的位置
     */
    @Override
    public void onDeleteClick(int position) {
        Event event = eventAdapter.getEventAt(position);
        deleteEvent(event);
        eventAdapter.removeEvent(position);
    }

    /**
     * 从存储中删除指定的事件，并通知日历界面更新。
     *
     * @param event 要删除的事件
     */
    private void deleteEvent(Event event) {
        // 从存储中删除事件
        SharedPreferences prefs = requireActivity().getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
        String dateKey = event.getDate().toString();

        Gson gson = GsonUtils.getGson();
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();

        String eventJson = prefs.getString(dateKey, "[]");
        List<Event> events = gson.fromJson(eventJson, type);

        // 通过 ID 查找并删除事件
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
            Event e = iterator.next();
            if (e.getId().equals(event.getId())) {
                iterator.remove();
                break;
            }
        }

        // 保存更新后的事件列表
        String updatedJson = gson.toJson(events);
        prefs.edit().putString(dateKey, updatedJson).apply();

        // 通知日历界面更新
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).notifyCalendarUpdate();
        }

        Toast.makeText(getContext(), "事件已删除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 当 Fragment 恢复可见时调用，注册监听器，刷新事件列表并检查刷新标志。
     */
    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onResume() {
        super.onResume();

        // 1. 注册监听器
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnEventAddedListener(this);
        }

        // 2. 刷新事件列表
        refreshEvents();

        // 3. 检查刷新标志
        checkRefreshFlag();
    }

    /**
     * 检查刷新标志，如果需要则刷新 AI 建议。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkRefreshFlag() {
        // 获取存储 AI 建议的共享偏好
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_ADVICE, Context.MODE_PRIVATE);
        // 获取刷新标志
        boolean refreshFlag = prefs.getBoolean("refresh_advice_flag", false);

        if (refreshFlag) {
            // 清除标志
            prefs.edit().putBoolean("refresh_advice_flag", false).apply();

            // 刷新建议
            refreshAdviceIfNeeded();
        } else {
            // 检查是否需要基于日期的刷新
            refreshAdviceIfNeeded();
        }
    }

    /**
     * 当 Fragment 暂停时调用，取消监听器注册。
     */
    @Override
    public void onPause() {
        super.onPause();
        // 取消监听器注册
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnEventAddedListener(null);
        }
    }

    /**
     * 处理事件添加的回调，当添加的事件属于今日时，刷新 UI。
     *
     * @param event 添加的事件
     */
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
                Log.d(TAG, "事件属于今日，刷新 UI");
                requireActivity().runOnUiThread(() -> {
                    refreshEvents();
                    Toast.makeText(getContext(), "今日日程已更新", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    /**
     * 处理下拉刷新事件，调用 refreshEvents 方法刷新今日日程。
     */
    @Override
    public void onRefresh() {
        Log.d(TAG, "用户手动刷新今日日程");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            refreshEvents();
        }
    }

    /**
     * 更新日期显示，显示当前日期。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateDate() {
        LocalDate today = LocalDate.now();
        dateTextView.setText(today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
    }

    /**
     * 核心函数：刷新今日日程，加载今日的事件并更新 RecyclerView 的数据。
     * 同时更新任务数据汇总和进度显示。
     */
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

    /**
     * 更新本周任务数据汇总，包括剩余任务数量、已完成任务数量和任务完成进度。
     * 同时更新本周日期显示。
     */
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateDataSummary() {
        // 获取本周的起始日期（周日）和结束日期（周六）
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        // 计算本周任务数据
        int totalTasks = 0;
        int completedTasks = 0;
        LocalTime now = LocalTime.now();

        // 遍历本周所有日期
        for (LocalDate date = startOfWeek; !date.isAfter(endOfWeek); date = date.plusDays(1)) {
            List<Event> events = loadEventsForDate(date);
            totalTasks += events.size();

            if (date.isBefore(today)) {
                // 过去日期的所有任务都视为已完成
                completedTasks += events.size();
            } else if (date.isEqual(today)) {
                // 今天的任务根据结束时间判断
                for (Event event : events) {
                    if (event.getEndTimeAsLocalTime() != null && event.getEndTimeAsLocalTime().isBefore(now)) {
                        completedTasks++;
                    }
                }
            }
            // 未来日期的任务不计入已完成
        }

        int remainingTasks = totalTasks - completedTasks;
        int progress = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;

        // 更新 UI
        tasksRemaining.setText("剩余任务：" + remainingTasks);
        tasksCompleted.setText("结束任务：" + completedTasks);
        progressCircle.setProgress(progress);
        progressText.setText(progress + "%");

        // 更新本周日期显示
        updateWeekDisplay(startOfWeek, endOfWeek);
    }

    /**
     * 更新本周日期显示，显示本周的日期范围。
     *
     * @param startOfWeek 本周的起始日期
     * @param endOfWeek   本周的结束日期
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateWeekDisplay(LocalDate startOfWeek, LocalDate endOfWeek) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        String weekRange = startOfWeek.format(formatter) + " - " + endOfWeek.format(formatter);

        // 找到本周显示控件并更新
        View view = getView();
        if (view != null) {
            TextView weekRangeView = view.findViewById(R.id.week_range);
            if (weekRangeView != null) {
                weekRangeView.setText(weekRange);
            }
        }
    }

    /**
     * 加载指定日期的事件列表，过滤掉无效事件。
     *
     * @param date 指定的日期
     * @return 指定日期的有效事件列表
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<Event> loadEventsForDate(LocalDate date) {
        if (getActivity() == null) {
            return new ArrayList<>();
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
        String eventJson = prefs.getString(date.toString(), "[]");

        // 使用自定义的 Gson 实例
        Gson gson = GsonUtils.getGson();
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();

        // 过滤无效事件
        List<Event> events = gson.fromJson(eventJson, type);
        List<Event> validEvents = new ArrayList<>();

        for (Event event : events) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (event.getDate() != null && event.getStartTimeAsLocalTime() != null && event.getEndTimeAsLocalTime() != null) {
                    validEvents.add(event);
                }
            }
        }

        return validEvents;
    }
}