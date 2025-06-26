public class EventEditorFragment extends Fragment {

    // 用于输入开始时间的编辑框
    private EditText startTimeEditText;
    // 用于输入结束时间的编辑框
    private EditText endTimeEditText;
    // 用于输入事件描述的编辑框
    private EditText eventDescriptionEditText;
    // 显示日期的文本视图
    private TextView dateTextView;
    // 当前选择的日期
    private LocalDate currentDate;
    // 主活动实例，用于调用主活动的方法
    private MainActivity mainActivity;

    /**
     * 核心函数：创建并返回Fragment的视图
     * 该函数在Fragment创建视图时被调用，用于初始化界面组件，设置默认值，处理传递的日期参数，
     * 并为保存和返回按钮设置点击监听器
     *
     * @param inflater           用于将布局文件转换为视图的LayoutInflater对象
     * @param container          该Fragment的父视图容器
     * @param savedInstanceState 保存的状态信息
     * @return 返回创建的视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 填充布局文件，创建视图
        View view = inflater.inflate(R.layout.fragment_event_editor, container, false);
        // 获取主活动实例
        mainActivity = (MainActivity) getActivity();

        // 找到日期文本视图
        dateTextView = view.findViewById(R.id.dateTV);
        // 找到开始时间编辑框
        startTimeEditText = view.findViewById(R.id.startTimeET);
        // 找到结束时间编辑框
        endTimeEditText = view.findViewById(R.id.endTimeET);
        // 找到事件描述编辑框
        eventDescriptionEditText = view.findViewById(R.id.eventDescriptionET);

        // 设置开始时间编辑框的默认值为09:00
        startTimeEditText.setText("09:00");
        // 设置结束时间编辑框的默认值为10:00
        endTimeEditText.setText("10:00");
        // 设置事件描述编辑框的默认值为复习四六级
        eventDescriptionEditText.setText("复习四六级");

        // 获取传递的日期参数
        Bundle args = getArguments();
        if (args != null && args.containsKey("date")) {
            // 如果Android版本支持，解析传递的日期字符串为LocalDate对象
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentDate = LocalDate.parse(args.getString("date"));
            }
            // 如果Android版本支持，将日期格式化为yyyy年MM月dd日并显示在文本视图中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dateTextView.setText(currentDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
            }
        } else {
            // 如果没有传递日期参数，获取当前日期
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentDate = LocalDate.now();
            }
            // 如果Android版本支持，将当前日期格式化为yyyy年MM月dd日并显示在文本视图中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dateTextView.setText(currentDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
            }
        }

        // 找到保存按钮
        Button saveButton = view.findViewById(R.id.saveEventBtn);
        // 找到返回按钮
        Button backButton = view.findViewById(R.id.backBtn);

        // 如果Android版本支持，为保存按钮设置点击监听器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            saveButton.setOnClickListener(v -> saveEvent());
        }
        // 为返回按钮设置点击监听器，点击后显示日历Fragment
        backButton.setOnClickListener(v -> mainActivity.showCalendarFragment());

        return view;
    }

    /**
     * 核心函数：保存事件到日历
     * 该函数用于验证用户输入的时间和描述，创建事件对象，将事件添加到主活动的日历中，
     * 并根据来源决定返回的界面，最后显示保存成功的消息
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveEvent() {
        try {
            // 解析开始时间编辑框中的文本为LocalTime对象
            LocalTime startTime = LocalTime.parse(startTimeEditText.getText().toString());
            // 解析结束时间编辑框中的文本为LocalTime对象
            LocalTime endTime = LocalTime.parse(endTimeEditText.getText().toString());
            // 获取事件描述编辑框中的文本，并去除前后空格
            String description = eventDescriptionEditText.getText().toString().trim();

            // 验证输入：如果描述为空，显示提示信息并返回
            if (description.isEmpty()) {
                Toast.makeText(getContext(), "事件内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证输入：如果结束时间早于开始时间，显示提示信息并返回
            if (endTime.isBefore(startTime)) {
                Toast.makeText(getContext(), "结束时间不能早于开始时间", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建事件对象
            Event event = new Event(currentDate, startTime, endTime, description);

            // 保存事件到主活动
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).addEventToCalendar(event);
            }

            // 获取来源标记，默认来源为日历
            String source = "calendar";
            if (getArguments() != null && getArguments().containsKey("source")) {
                source = getArguments().getString("source");
            }

            // 根据来源决定返回路径
            if (source.equals(DayScheduleFragment.class.getSimpleName())) {
                // 从日程界面进入，返回原日程界面
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    mainActivity.showCalendarFragment();
                }
            } else if (source.equals(TodayFragment.class.getSimpleName())) {
                // 从今日日程界面进入，返回今日日程界面
                mainActivity.showTodayFragment();
            } else {
                // 从其他界面进入，返回日历界面
                mainActivity.showCalendarFragment();
            }

            // 显示成功消息
            Toast.makeText(getContext(), "事件已添加", Toast.LENGTH_SHORT).show();

            // 调试日志：获取保存的事件JSON字符串并打印日志
            SharedPreferences prefs = requireActivity().getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
            String eventsJson = prefs.getString(currentDate.toString(), "");
            Log.d("EventCheck", "保存的事件: " + eventsJson);

        } catch (DateTimeParseException e) {
            // 处理时间格式解析异常，显示提示信息
            Toast.makeText(getContext(), "时间格式不正确，请使用HH:mm格式（如09:30）", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // 处理其他异常，打印错误日志并显示提示信息
            Log.e("EventEditor", "保存事件时出错", e);
            Toast.makeText(getContext(), "保存事件时出错", Toast.LENGTH_SHORT).show();
        }
    }
}