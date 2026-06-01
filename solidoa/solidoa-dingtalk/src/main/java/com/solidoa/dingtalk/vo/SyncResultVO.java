package com.solidoa.dingtalk.vo;

import lombok.Data;

/**
 * 同步结果VO
 */
@Data
public class SyncResultVO {

    /** 是否成功 */
    private Boolean success;

    /** 同步记录数 */
    private Integer count;

    /** 消息 */
    private String message;

    /** 错误码 */
    private String errorCode;

    public static SyncResultVO success(Integer count) {
        SyncResultVO result = new SyncResultVO();
        result.setSuccess(true);
        result.setCount(count);
        result.setMessage("同步成功");
        return result;
    }

    public static SyncResultVO failed(String message) {
        SyncResultVO result = new SyncResultVO();
        result.setSuccess(false);
        result.setCount(0);
        result.setMessage(message);
        return result;
    }
}