public class GsonUtils {

    /**
     * 核心函数：获取一个配置好的 Gson 实例。
     * 该实例已经注册了 LocalDate 和 LocalTime 的适配器，用于处理这两种类型的序列化和反序列化。
     * @return 配置好的 Gson 实例
     */
    public static Gson getGson() {
        // 创建一个 GsonBuilder 实例，用于构建自定义的 Gson 配置
        GsonBuilder gsonBuilder = new GsonBuilder();

        // 注册 LocalDate 适配器
        // 检查当前 Android 系统版本是否支持 Java 8 的日期时间 API（即版本号大于等于 O）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 注册 LocalDate 类型的适配器，用于处理 LocalDate 类型的序列化和反序列化
            gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        }

        // 注册 LocalTime 适配器
        // 检查当前 Android 系统版本是否支持 Java 8 的日期时间 API（即版本号大于等于 O）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 注册 LocalTime 类型的适配器，用于处理 LocalTime 类型的序列化和反序列化
            gsonBuilder.registerTypeAdapter(LocalTime.class, new LocalTimeAdapter());
        }

        // 使用配置好的 GsonBuilder 创建 Gson 实例
        return gsonBuilder.create();
    }

    /**
     * LocalDate 类型的适配器，用于将 LocalDate 对象序列化为 JSON 字符串，以及将 JSON 字符串反序列化为 LocalDate 对象。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        // 定义日期格式化器，使用 ISO 标准的本地日期格式
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        /**
         * 将 LocalDate 对象序列化为 JSON 元素。
         * @param src 要序列化的 LocalDate 对象
         * @param typeOfSrc 源对象的类型
         * @param context 序列化上下文
         * @return 序列化后的 JSON 元素
         */
        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            // 使用格式化器将 LocalDate 对象格式化为字符串，并创建一个 JsonPrimitive 对象
            return new JsonPrimitive(formatter.format(src));
        }

        /**
         * 将 JSON 元素反序列化为 LocalDate 对象。
         * @param json 要反序列化的 JSON 元素
         * @param typeOfT 目标对象的类型
         * @param context 反序列化上下文
         * @return 反序列化后的 LocalDate 对象，如果解析失败则返回 null
         * @throws JsonParseException 如果解析 JSON 元素时发生错误
         */
        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                // 使用格式化器将 JSON 元素的字符串值解析为 LocalDate 对象
                return LocalDate.parse(json.getAsString(), formatter);
            } catch (Exception e) {
                // 记录日期解析错误信息
                Log.e("GsonUtils", "日期解析错误: " + json.toString(), e);
                return null;
            }
        }
    }

    /**
     * LocalTime 类型的适配器，用于将 LocalTime 对象序列化为 JSON 字符串，以及将 JSON 字符串反序列化为 LocalTime 对象。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static class LocalTimeAdapter implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        // 定义时间格式化器，使用 ISO 标准的本地时间格式
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;

        /**
         * 将 LocalTime 对象序列化为 JSON 元素。
         * @param src 要序列化的 LocalTime 对象
         * @param typeOfSrc 源对象的类型
         * @param context 序列化上下文
         * @return 序列化后的 JSON 元素
         */
        @Override
        public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
            // 使用格式化器将 LocalTime 对象格式化为字符串，并创建一个 JsonPrimitive 对象
            return new JsonPrimitive(formatter.format(src));
        }

        /**
         * 将 JSON 元素反序列化为 LocalTime 对象。
         * @param json 要反序列化的 JSON 元素
         * @param typeOfT 目标对象的类型
         * @param context 反序列化上下文
         * @return 反序列化后的 LocalTime 对象，如果解析失败则返回 null
         * @throws JsonParseException 如果解析 JSON 元素时发生错误
         */
        @Override
        public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                // 使用格式化器将 JSON 元素的字符串值解析为 LocalTime 对象
                return LocalTime.parse(json.getAsString(), formatter);
            } catch (Exception e) {
                // 记录时间解析错误信息
                Log.e("GsonUtils", "时间解析错误: " + json.toString(), e);
                return null;
            }
        }
    }
}