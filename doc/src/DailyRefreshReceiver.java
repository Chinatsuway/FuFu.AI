/**
 * 每日刷新广播接收器类，用于处理每日刷新广播和开机广播。
 */
public class DailyRefreshReceiver extends BroadcastReceiver {

    /**
     * 核心函数：当接收到广播时调用此方法。
     * 此方法会根据接收到的广播类型执行不同的操作，包括每日刷新建议和重新设置闹钟。
     *
     * @param context 上下文对象，用于访问系统服务和资源。
     * @param intent  接收到的广播意图，包含广播的相关信息。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        // 记录日志，表明收到了每日刷新广播
        Log.d("DailyRefresh", "收到每日刷新广播");

        // 1. 检查是否是每日刷新广播
        if ("com.example.calendarapp.ACTION_DAILY_REFRESH".equals(intent.getAction())) {
            // 如果是每日刷新广播，调用更新建议的方法
            updateAdviceForNewDay(context);
        }
        // 3. 如果是开机广播，重新设置闹钟
        else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 调用 TodayFragment 的静态方法重新设置每日刷新闹钟
            TodayFragment.scheduleDailyRefresh(context);
        }
    }

    /**
     * 核心函数：为新的一天更新建议。
     * 此方法会检查是否有缓存的建议数据，如果有则使用缓存数据生成新建议，
     * 如果没有则设置一个标志，让 Fragment 来处理。
     *
     * @param context 上下文对象，用于访问共享偏好设置。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateAdviceForNewDay(Context context) {
        // 获取名为 "advice_prefs" 的共享偏好设置对象，用于存储和读取数据
        SharedPreferences prefs = context.getSharedPreferences("advice_prefs", Context.MODE_PRIVATE);
        // 从共享偏好设置中获取上次的建议数据，如果没有则返回空字符串
        String cachedData = prefs.getString("last_advice_data", "");

        if (!cachedData.isEmpty()) {
            // 如果缓存数据不为空，调用生成新建议的方法
            generateNewAdvice(context, cachedData);
        } else {
            // 如果没有缓存数据，设置一个标志，让 Fragment 处理刷新建议的操作
            prefs.edit().putBoolean("refresh_advice_flag", true).apply();
        }
    }

    /**
     * 生成新建议的方法。
     * 在实际应用中，这里应该使用后台服务处理，为简化示例，只设置一个标志。
     *
     * @param context      上下文对象，用于访问共享偏好设置。
     * @param scheduleData 缓存的日程数据，用于生成新建议。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void generateNewAdvice(Context context, String scheduleData) {
        // 获取名为 "advice_prefs" 的共享偏好设置对象
        SharedPreferences prefs = context.getSharedPreferences("advice_prefs", Context.MODE_PRIVATE);
        // 设置一个标志，让 Fragment 处理刷新建议的操作
        prefs.edit().putBoolean("refresh_advice_flag", true).apply();
    }
}