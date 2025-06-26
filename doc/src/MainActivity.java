public class MainActivity extends AppCompatActivity {

    // 底部导航栏视图
    private BottomNavigationView bottomNavigationView;

    // 事件添加监听器接口
    public interface OnEventAddedListener {
        // 当事件添加时调用
        void onEventAdded(Event event);
    }

    // 事件添加监听器实例
    private OnEventAddedListener eventAddedListener;

    // 设置事件添加监听器
    public void setOnEventAddedListener(OnEventAddedListener listener) {
        this.eventAddedListener = listener;
    }

    /**
     * 通知日历页面更新
     * 核心函数：该函数用于在事件添加等操作后更新日历页面的显示
     */
    public void notifyCalendarUpdate() {
        // 通过FragmentManager找到当前显示的Fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // 判断当前Fragment是否为CalendarFragment
        if (fragment instanceof CalendarFragment) {
            // 调用CalendarFragment的updateCalendar方法更新日历
            ((CalendarFragment) fragment).updateCalendar();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局文件
        setContentView(R.layout.activity_main);

        // 找到底部导航栏视图
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // 设置底部导航栏的选中监听器
        bottomNavigationView.setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        // 如果是首次创建Activity（非恢复状态）
        if (savedInstanceState == null) {
            // 开启Fragment事务
            getSupportFragmentManager().beginTransaction()
                    // 替换Fragment容器中的Fragment为TodayFragment（今日日程页面）
                    .replace(R.id.fragment_container, new TodayFragment())
                    // 提交事务
                    .commit();
        }
    }

    // 底部导航栏选中监听器
    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            item -> {
                // 选中的Fragment
                Fragment selectedFragment = null;
                // 获取选中菜单项的ID
                int itemId = item.getItemId();

                // 根据菜单项ID选择要显示的Fragment
                if (itemId == R.id.nav_calendar) {
                    selectedFragment = new CalendarFragment();
                } else if (itemId == R.id.nav_today) {
                    selectedFragment = new TodayFragment();
                } else if (itemId == R.id.nav_ai) {
                    selectedFragment = new AIFragment();
                }

                // 如果选中的Fragment不为空
                if (selectedFragment != null) {
                    // 开启Fragment事务
                    getSupportFragmentManager().beginTransaction()
                            // 替换Fragment容器中的Fragment为选中的Fragment
                            .replace(R.id.fragment_container, selectedFragment)
                            // 提交事务
                            .commit();
                    return true;
                }
                return false;
            };

    /**
     * 显示日历页面
     * 核心函数：用于切换到日历页面并更新底部导航栏选中状态
     */
    public void showCalendarFragment() {
        // 开启Fragment事务
        getSupportFragmentManager().beginTransaction()
                // 替换Fragment容器中的Fragment为CalendarFragment
                .replace(R.id.fragment_container, new CalendarFragment())
                // 提交事务
                .commit();

        // 更新底部导航栏的选中状态为日历项
        bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
    }

    /**
     * 显示今日日程页面
     * 核心函数：用于切换到今日日程页面并更新底部导航栏选中状态
     */
    public void showTodayFragment() {
        // 开启Fragment事务
        getSupportFragmentManager().beginTransaction()
                // 替换Fragment容器中的Fragment为TodayFragment
                .replace(R.id.fragment_container, new TodayFragment())
                // 提交事务
                .commit();

        // 更新底部导航栏的选中状态为今日日程项
        bottomNavigationView.setSelectedItemId(R.id.nav_today);
    }

    /**
     * 显示事件编辑页面，带有日期和来源信息
     * 核心函数：用于打开事件编辑页面，并传递日期和来源信息
     * @param date 事件的日期
     * @param source 事件的来源标记
     */
    public void showEventEditorFragment(LocalDate date, String source) {
        // 创建一个Bundle对象，用于传递参数
        Bundle args = new Bundle();
        // 将日期转换为字符串并放入Bundle中
        args.putString("date", date.toString());
        // 将来源标记放入Bundle中
        args.putString("source", source);

        // 创建EventEditorFragment实例
        EventEditorFragment fragment = new EventEditorFragment();
        // 将Bundle参数设置给Fragment
        fragment.setArguments(args);

        // 开启Fragment事务
        getSupportFragmentManager().beginTransaction()
                // 替换Fragment容器中的Fragment为EventEditorFragment
                .replace(R.id.fragment_container, fragment)
                // 将该事务添加到返回栈中
                .addToBackStack(null)
                // 提交事务
                .commit();
    }

    /**
     * 显示事件编辑页面，只带有日期信息，默认来源为"calendar"
     * 核心函数：用于打开事件编辑页面，并传递日期信息，默认来源为日历
     * @param date 事件的日期
     */
    public void showEventEditorFragment(LocalDate date) {
        // 调用带有日期和来源信息的showEventEditorFragment方法
        showEventEditorFragment(date, "calendar");
    }

    /**
     * 向日历中添加事件
     * 核心函数：用于将事件添加到日历页面和今日日程页面，并直接保存事件到存储
     * @param event 要添加的事件
     */
    public void addEventToCalendar(Event event) {
        // 打印日志，记录添加事件的信息
        Log.d("MainActivity", "添加事件: " + event);

        // 通过FragmentManager找到当前显示的Fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // 判断当前Fragment是否为CalendarFragment
        if (fragment instanceof CalendarFragment) {
            // 打印日志，记录通知CalendarFragment添加事件
            Log.d("MainActivity", "通知CalendarFragment添加事件");
            // 调用CalendarFragment的addEvent方法添加事件
            ((CalendarFragment) fragment).addEvent(event);
        }

        // 如果事件添加监听器不为空
        if (eventAddedListener != null) {
            // 打印日志，记录通知TodayFragment添加事件
            Log.d("MainActivity", "通知TodayFragment添加事件");
            // 调用监听器的onEventAdded方法通知今日日程页面添加事件
            eventAddedListener.onEventAdded(event);
        }

        // 直接保存事件到存储
        saveEventDirectly(event);
    }

    /**
     * 直接将事件保存到SharedPreferences中
     * @param event 要保存的事件
     */
    private void saveEventDirectly(Event event) {
        try {
            // 获取SharedPreferences实例，用于存储事件信息
            SharedPreferences prefs = getSharedPreferences("events_prefs", Context.MODE_PRIVATE);
            // 获取事件日期的字符串表示，作为存储的键
            String dateKey = event.getDate().toString();

            // 获取自定义的Gson实例
            Gson gson = GsonUtils.getGson();
            // 获取事件列表的类型
            Type type = new TypeToken<ArrayList<Event>>(){}.getType();

            // 从SharedPreferences中获取该日期的事件列表的JSON字符串
            String eventJson = prefs.getString(dateKey, "[]");
            // 将JSON字符串转换为事件列表
            List<Event> events = gson.fromJson(eventJson, type);

            // 向事件列表中添加新事件
            events.add(event);

            // 将更新后的事件列表转换为JSON字符串
            String updatedJson = gson.toJson(events);
            // 将更新后的JSON字符串保存到SharedPreferences中
            prefs.edit().putString(dateKey, updatedJson).apply();

            // 打印日志，记录事件已保存到存储
            Log.d("MainActivity", "事件已直接保存到存储: " + event);
        } catch (Exception e) {
            // 打印日志，记录直接保存事件失败的信息
            Log.e("MainActivity", "直接保存事件失败", e);
        }
    }

    /**
     * 通知所有页面刷新
     * 核心函数：用于在事件添加等操作后通知日历页面和今日日程页面刷新显示
     */
    public void notifyEventAdded() {
        // 通知日历页面更新
        notifyCalendarUpdate();

        // 通过FragmentManager找到当前显示的Fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // 判断当前Fragment是否为TodayFragment，并且Android版本是否支持LocalDate
        if (fragment instanceof TodayFragment && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 调用TodayFragment的refreshEvents方法刷新今日日程
            ((TodayFragment) fragment).refreshEvents();
        }
    }

    /**
     * 显示指定日期的日程安排页面
     * 核心函数：用于打开指定日期的日程安排页面，并传递日期信息
     * @param date 指定的日期
     */
    public void showDayScheduleFragment(LocalDate date) {
        // 创建一个Bundle对象，用于传递参数
        Bundle args = new Bundle();
        // 将日期转换为字符串并放入Bundle中
        args.putString("date", date.toString());

        // 创建DayScheduleFragment实例
        DayScheduleFragment fragment = new DayScheduleFragment();
        // 将Bundle参数设置给Fragment
        fragment.setArguments(args);

        // 开启Fragment事务
        getSupportFragmentManager().beginTransaction()
                // 替换Fragment容器中的Fragment为DayScheduleFragment
                .replace(R.id.fragment_container, fragment)
                // 将该事务添加到返回栈中
                .addToBackStack("day_schedule")
                // 提交事务
                .commit();
    }
}