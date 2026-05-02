package com.yzh.yingshi.constant;

public class PetDetectionConstant {

    private PetDetectionConstant() {}

    /** 告警来源: 宠物检测 */
    public static final String SOURCE_PET_DETECT = "PET_DETECT";

    /** 告警类型: 宠物越界 */
    public static final String ALARM_TYPE_PET_OUT_OF_ZONE = "PET_OUT_OF_ZONE";

    /** 告警类型: 宠物长时间未出现 */
    public static final String ALARM_TYPE_PET_ABSENT = "PET_ABSENT";

    /** 告警类型: 宠物异常活跃 */
    public static final String ALARM_TYPE_PET_ABNORMAL_ACTIVITY = "PET_ABNORMAL_ACTIVITY";

    /** 告警类型: 宠物长时间静止 */
    public static final String ALARM_TYPE_PET_LONG_STILLNESS = "PET_LONG_STILLNESS";

    /** 区域类型: 矩形 */
    public static final String ZONE_TYPE_RECTANGLE = "RECTANGLE";

    /** 区域类型: 多边形 */
    public static final String ZONE_TYPE_POLYGON = "POLYGON";

    /** 默认冷却时间(秒) */
    public static final int DEFAULT_COOLDOWN_SECONDS = 300;

    /** 定时检测间隔(毫秒) - 30秒 */
    public static final long DETECTION_INTERVAL_MS = 30000L;

    /** 宠物在安全区域外 */
    public static final Integer IN_ZONE_NO = 0;

    /** 宠物在安全区域内 */
    public static final Integer IN_ZONE_YES = 1;

    /** 告警未触发 */
    public static final Integer ALARM_NOT_TRIGGERED = 0;

    /** 告警已触发 */
    public static final Integer ALARM_TRIGGERED = 1;
}
