package com.solidoa.dingtalk.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 钉钉考勤回调DTO
 */
@Data
public class AttendanceCallbackDTO {

    /** 回调类型: checkin/location */
    private String msgType;

    /** 打卡事件列表 */
    private List<CheckinEvent> checkinList;

    @Data
    public static class CheckinEvent {
        /** 用户钉钉ID */
        private String userId;

        /** 打卡时间（毫秒） */
        private Long checkinTime;

        /** 打卡类型: 上班/下班 */
        private String checkinType;

        /** 地点名称 */
        private String locationName;

        /** 打卡经度 */
        private Double longitude;

        /** 打卡纬度 */
        private Double latitude;

        /** 是否在范围内 */
        private Boolean inRange;

        /** 设备ID */
        private String deviceId;
    }
}