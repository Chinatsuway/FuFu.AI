public class Event {

    // 事件的唯一标识符
    @SerializedName("id")
    private String id;

    // 事件发生的日期
    @SerializedName("date")
    private LocalDate date;

    // 事件开始的时间
    @SerializedName("startTime")
    private LocalTime startTime;

    // 事件结束的时间
    @SerializedName("endTime")
    private LocalTime endTime;

    // 事件的描述信息
    private String description;

    /**
     * 无参构造函数，用于创建一个新的事件对象。
     * 为事件生成一个唯一的 UUID 作为标识符。
     */
    public Event() {
        // 生成一个唯一的 UUID 字符串作为事件的 ID
        this.id = UUID.randomUUID().toString();
    }

    /**
     * 有参构造函数，用于创建一个具有指定日期、开始时间、结束时间和描述的事件对象。
     * 调用无参构造函数生成事件的唯一标识符。
     * 
     * @param date        事件发生的日期
     * @param startTime   事件开始的时间
     * @param endTime     事件结束的时间
     * @param description 事件的描述信息
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Event(LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
        // 调用无参构造函数生成事件的 ID
        this();
        // 设置事件的日期
        this.date = date;
        // 设置事件的开始时间
        this.startTime = startTime;
        // 设置事件的结束时间
        this.endTime = endTime;
        // 设置事件的描述信息
        this.description = description;
    }

    /**
     * 获取事件的唯一标识符。
     * 
     * @return 事件的唯一标识符
     */
    public String getId() {
        return id;
    }

    /**
     * 获取事件发生的日期。
     * 
     * @return 事件发生的日期
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * 设置事件的开始时间。
     * 
     * @param startTime 事件的开始时间
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 设置事件的结束时间。
     * 
     * @param endTime 事件的结束时间
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 设置事件的描述信息。
     * 
     * @param description 事件的描述信息
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取事件开始时间的字符串表示，格式为 "HH:mm"。
     * 如果开始时间为空，则返回 "error"。
     * 
     * @return 事件开始时间的字符串表示或 "error"
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getStartTime() {
        // 检查开始时间是否为空
        if (startTime != null) {
            // 格式化开始时间为 "HH:mm" 格式
            return startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        // 开始时间为空时返回 "error"
        return "error";
    }

    /**
     * 获取事件开始时间的 LocalTime 类型表示。
     * 
     * @return 事件开始时间的 LocalTime 类型表示
     */
    public LocalTime getStartTimeAsLocalTime() {
        return startTime;
    }

    /**
     * 获取事件结束时间的字符串表示，格式为 "HH:mm"。
     * 如果结束时间为空，则返回 "error"。
     * 
     * @return 事件结束时间的字符串表示或 "error"
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getEndTime() {
        // 检查结束时间是否为空
        if (endTime != null) {
            // 格式化结束时间为 "HH:mm" 格式
            return endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        // 结束时间为空时返回 "error"
        return "error";
    }

    /**
     * 获取事件结束时间的 LocalTime 类型表示。
     * 
     * @return 事件结束时间的 LocalTime 类型表示
     */
    public LocalTime getEndTimeAsLocalTime() {
        return endTime;
    }

    /**
     * 获取事件的描述信息。
     * 
     * @return 事件的描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 返回事件的字符串表示，格式为 "开始时间-结束时间: 描述信息"。
     * 
     * @return 事件的字符串表示
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public String toString() {
        // 格式化开始时间和结束时间为 "HH:mm" 格式，并拼接描述信息
        return String.format("%s-%s: %s",
                startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                description);
    }

    /**
     * 核心函数：创建一个新的事件对象。
     * 
     * @param date        事件发生的日期
     * @param startTime   事件开始的时间
     * @param endTime     事件结束的时间
     * @param description 事件的描述信息
     * @return 新创建的事件对象
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Event createEvent(LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
        // 调用有参构造函数创建新的事件对象
        return new Event(date, startTime, endTime, description);
    }
}