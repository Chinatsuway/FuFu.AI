public class CalendarFragment extends Fragment {

    private TextView monthYearText; // 显示当前年月的文本视图
    private GridLayout calendarGrid; // 日历网格布局
    private YearMonth currentYearMonth; // 当前显示的年月
    private Map<LocalDate, List<Event>> eventsMap = new HashMap<>(); // 存储每个日期对应的事件列表
    private MainActivity mainActivity; // 主活动实例

    private static final String EVENTS_PREFS = "events_prefs"; // 存储事件的SharedPreferences的名称

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 核心函数：在创建Fragment视图时调用
        // 功能：初始化视图组件，设置按钮点击事件，并调用updateCalendar方法更新日历显示
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        mainActivity = (MainActivity) getActivity();

        monthYearText = view.findViewById(R.id.monthYearTV);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentYearMonth = YearMonth.now(); // 获取当前年月
        }

        Button prevButton = view.findViewById(R.id.prevMonthBtn);
        Button nextButton = view.findViewById(R.id.nextMonthBtn);
        Button todayButton = view.findViewById(R.id.todayBtn);
        Button aiButton = view.findViewById(R.id.aiBtn);

        prevButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentYearMonth = currentYearMonth.minusMonths(1); // 切换到上一个月
            }
            updateCalendar(); // 更新日历显示
        });

        nextButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentYearMonth = currentYearMonth.plusMonths(1); // 切换到下一个月
            }
            updateCalendar(); // 更新日历显示
        });

        todayButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentYearMonth = YearMonth.now(); // 切换到当前月
            }
            updateCalendar(); // 更新日历显示
        });

        aiButton.setOnClickListener(v -> {
            mainActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AIFragment())
                    .addToBackStack(null)
                    .commit(); // 切换到AI页面
        });

        updateCalendar(); // 初始化日历显示
        return view;
    }

    void updateCalendar() {
        // 核心函数：更新日历的显示
        // 功能：加载所有事件，设置当前年月的显示，清空日历网格，填充空白单元格和日期单元格，并显示事件
        eventsMap = loadAllEvents(); // 加载所有事件
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            monthYearText.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy年 M月"))); // 设置当前年月的显示
        }
        calendarGrid.removeAllViews(); // 清空日历网格

        // 获取当月第一天
        LocalDate firstOfMonth = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            firstOfMonth = currentYearMonth.atDay(1);
        }
        int firstDayOfWeek = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 获取当月第一天是星期几
        }

        // 转换为日历视图的偏移量 (0=周日, 1=周一, ... 6=周六)
        int calendarOffset = (firstDayOfWeek == 7) ? 0 : firstDayOfWeek;

        // === 修复1：确保总单元格数为42个 ===
        int daysInMonth = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            daysInMonth = currentYearMonth.lengthOfMonth(); // 获取当月的天数
        }
        int totalCells = calendarOffset + daysInMonth;
        int remainingCells = 42 - totalCells; // 7列x6行=42个单元格

        // 填充空白（上月日期）
        for (int i = 0; i < calendarOffset; i++) {
            addEmptyCell(); // 添加空白单元格
        }

        // 填充当月日期
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now(); // 获取当前日期
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentDate = firstOfMonth.plusDays(day - 1); // 获取当前日期
            } else {
                currentDate = null;
            }

            // 创建日期单元格
            View dayView = LayoutInflater.from(getContext())
                    .inflate(R.layout.calendar_day_cell, calendarGrid, false);
            TextView dayNumber = dayView.findViewById(R.id.dayNumber);
            TextView eventsText = dayView.findViewById(R.id.eventsText);

            dayNumber.setText(String.valueOf(day)); // 设置日期数字
            dayNumber.setVisibility(View.VISIBLE);

            // 标记今天
            if (currentDate != null && currentDate.equals(today)) {
                // 使用圆角背景而不是设置整个视图的背景色
                dayView.setBackgroundResource(R.drawable.bg_calendar_cell_today);
            }

            // 标记周末
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
                DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    dayNumber.setTextColor(getResources().getColor(R.color.weekend_text)); // 设置周末日期的文本颜色
                }
            }

            // 显示事件
            if (currentDate != null) {
                List<Event> dayEvents = eventsMap.get(currentDate);
                if (dayEvents != null && !dayEvents.isEmpty()) {
                    showEventsInCell(eventsText, dayEvents, dayView); // 显示当天的事件
                }
            }

            // 添加点击事件
            dayView.setOnClickListener(v -> {
                if (currentDate != null) {
                    mainActivity.showEventEditorFragment(currentDate,
                            CalendarFragment.class.getSimpleName()); // 添加来源标记
                }
            });

            // 添加长按事件
            dayView.setOnLongClickListener(v -> {
                if (currentDate != null) {
                    // 直接通过 MainActivity 导航
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showDayScheduleFragment(currentDate);
                    }
                    return true;
                }
                return false;
            });

            // === 修复2：使用统一的行高规格 ===
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0; // 使用权重控制高度
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // 行权重为1
            dayView.setLayoutParams(params);

            calendarGrid.addView(dayView);
        }

        // === 修复3：填充下月空白单元格 ===
        for (int i = 0; i < remainingCells; i++) {
            addEmptyCell(); // 添加空白单元格
        }

        calendarGrid.requestLayout();
    }

    // 添加空白单元格的辅助方法
    private void addEmptyCell() {
        // 功能：创建一个空白单元格并添加到日历网格中
        View emptyView = LayoutInflater.from(getContext())
                .inflate(R.layout.calendar_day_cell, calendarGrid, false);
        TextView dayNumber = emptyView.findViewById(R.id.dayNumber);
        dayNumber.setText("");
        dayNumber.setVisibility(View.INVISIBLE);
        emptyView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.empty_cell_color));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0; // 使用权重控制高度
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // 行权重为1
        emptyView.setLayoutParams(params);

        calendarGrid.addView(emptyView);
    }

    // 在单元格中显示事件的辅助方法
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
        // 功能：从SharedPreferences中加载所有事件，并过滤无效事件
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
}