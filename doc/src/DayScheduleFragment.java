// 这是一个用于显示某一天日程安排的Fragment类
public class DayScheduleFragment extends Fragment implements EventAdapter.OnItemClickListener {
    private static final String TAG = "DayScheduleFragment";

    // 用于显示事件列表的RecyclerView
    private RecyclerView eventsRecyclerView;
    // 事件列表的适配器
    private EventAdapter eventAdapter;
    // 存储事件的列表
    private List<Event> eventsList = new ArrayList<>();
    // 当前选中的日期
    private LocalDate selectedDate;

    /**
     * 核心函数：创建Fragment的视图。
     * 此函数在Fragment创建视图时被调用，负责初始化界面组件，设置日期标题，
     * 初始化RecyclerView及其适配器，添加滑动删除功能，设置返回和添加事件按钮的点击监听器，
     * 并加载当天的事件。
     *
     * @param inflater           用于将布局文件转换为视图的LayoutInflater对象
     * @param container          视图的父容器
     * @param savedInstanceState 保存的实例状态
     * @return 返回创建好的视图
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用布局文件fragment_day_schedule创建视图
        View view = inflater.inflate(R.layout.fragment_day_schedule, container, false);

        // 获取传递的日期，如果没有传递则使用当前日期
        if (getArguments() != null) {
            selectedDate = LocalDate.parse(getArguments().getString("date"));
        } else {
            selectedDate = LocalDate.now();
        }

        // 设置日期标题，将日期格式化为"yyyy年MM月dd日"的形式
        TextView dateTextView = view.findViewById(R.id.date_title);
        dateTextView.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));

        // 初始化RecyclerView，设置布局管理器为线性布局
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 创建事件适配器，并将事件列表和点击监听器传递给它
        eventAdapter = new EventAdapter(eventsList, this);
        // 将适配器设置给RecyclerView
        eventsRecyclerView.setAdapter(eventAdapter);

        // 为返回按钮设置点击监听器，点击后返回到日历界面
        ImageView backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).showCalendarFragment();
            }
        });

        // 为添加事件按钮设置点击监听器，点击后显示事件编辑界面
        ImageView addButton = view.findViewById(R.id.add_event_button);
        addButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showEventEditorFragment(
                        selectedDate,
                        DayScheduleFragment.class.getSimpleName() // 添加来源标记
                );
            }
        });

        // 设置RecyclerView的滑动删除功能
        setupItemTouchHelper();

        // 加载当天的事件并刷新界面
        refreshEvents();

        return view;
    }

    /**
     * 核心函数：刷新当天的事件列表。
     * 此函数会加载当天的事件，对事件按开始时间排序，更新事件列表并通知适配器数据已改变。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void refreshEvents() {
        // 如果没有选中日期，则直接返回
        if (selectedDate == null) return;

        // 加载当天的事件
        List<Event> events = loadEventsForDate(selectedDate);
        // 对事件按开始时间排序
        events.sort(Comparator.comparing(Event::getStartTime));

        // 清空当前事件列表
        eventsList.clear();
        // 将加载的事件添加到事件列表中
        eventsList.addAll(events);
        // 通知适配器数据已改变
        eventAdapter.notifyDataSetChanged();
    }

    /**
     * 设置RecyclerView的滑动删除功能。
     * 此函数创建一个ItemTouchHelper，实现向左滑动删除事件的功能。
     */
    private void setupItemTouchHelper() {
        // 创建一个简单的ItemTouchHelper回调，只允许向左滑动
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // 不支持移动操作，返回false
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 获取被滑动的事件的位置
                int position = viewHolder.getAdapterPosition();
                // 获取该位置的事件
                Event event = eventAdapter.getEventAt(position);
                // 删除该事件
                deleteEvent(event);
                // 从适配器中移除该事件
                eventAdapter.removeEvent(position);
            }
        };

        // 创建ItemTouchHelper并将其附加到RecyclerView上
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(eventsRecyclerView);
    }

    /**
     * 当点击事件的删除按钮时调用。
     * 此函数会删除指定位置的事件。
     *
     * @param position 事件在列表中的位置
     */
    @Override
    public void onDeleteClick(int position) {
        // 获取指定位置的事件
        Event event = eventAdapter.getEventAt(position);
        // 删除该事件
        deleteEvent(event);
        // 从适配器中移除该事件
        eventAdapter.removeEvent(position);
    }

    /**
     * 从存储中删除指定的事件。
     * 此函数会从SharedPreferences中删除指定的事件，并通知日历界面更新。
     *
     * @param event 要删除的事件
     */
    private void deleteEvent(Event event) {
        // 获取SharedPreferences实例
        SharedPreferences prefs = requireActivity().getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
        // 获取事件日期对应的键
        String dateKey = event.getDate().toString();

        // 获取Gson实例
        Gson gson = GsonUtils.getGson();
        // 获取事件列表的类型
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();

        // 从SharedPreferences中获取该日期的事件列表的JSON字符串
        String eventJson = prefs.getString(dateKey, "[]");
        // 将JSON字符串转换为事件列表
        List<Event> events = gson.fromJson(eventJson, type);

        // 通过ID查找并删除事件
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
            Event e = iterator.next();
            if (e.getId().equals(event.getId())) {
                iterator.remove();
                break;
            }
        }

        // 将更新后的事件列表转换为JSON字符串
        String updatedJson = gson.toJson(events);
        // 将更新后的JSON字符串保存到SharedPreferences中
        prefs.edit().putString(dateKey, updatedJson).apply();

        // 通知日历界面更新
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).notifyCalendarUpdate();
        }

        // 显示删除成功的提示信息
        Toast.makeText(getContext(), "事件已删除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 加载指定日期的有效事件列表。
     * 此函数会从SharedPreferences中获取指定日期的事件列表，并过滤掉无效的事件。
     *
     * @param date 要加载事件的日期
     * @return 返回指定日期的有效事件列表
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<Event> loadEventsForDate(LocalDate date) {
        // 如果当前Activity为空，则返回一个空列表
        if (getActivity() == null) {
            return new ArrayList<>();
        }

        // 获取SharedPreferences实例
        SharedPreferences prefs = requireActivity().getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
        // 从SharedPreferences中获取该日期的事件列表的JSON字符串
        String eventJson = prefs.getString(date.toString(), "[]");

        // 获取Gson实例
        Gson gson = GsonUtils.getGson();
        // 获取事件列表的类型
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();

        // 将JSON字符串转换为事件列表
        List<Event> events = gson.fromJson(eventJson, type);
        // 用于存储有效的事件
        List<Event> validEvents = new ArrayList<>();

        // 过滤无效事件
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