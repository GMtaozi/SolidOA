package com.solidoa.common.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.solidoa.common.util.LogMaskUtil;

/**
 * Logback 日志脱敏转换器
 * 在日志输出前对消息进行脱敏处理
 */
public class LogMaskFilter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }
        return LogMaskUtil.mask(message);
    }
}
