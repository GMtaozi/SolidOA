package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_dingtalk_config")
public class DingtalkConfig {
    private Long id;
    private String appKey;
    private String appSecret;       // 存储 AES 加密后的密文
    private String agentId;
    private String corpId;
    private String callbackUrl;
    private String callbackToken;
    private String callbackAesKey;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
