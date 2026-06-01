package com.solidoa.dingtalk.controller;

import com.solidoa.common.result.Result;
import com.solidoa.dingtalk.entity.DingtalkConfig;
import com.solidoa.dingtalk.mapper.DingtalkConfigMapper;
import com.solidoa.dingtalk.service.DingtalkCodeService;
import com.solidoa.dingtalk.service.DingtalkCallbackService;
import com.solidoa.dingtalk.service.DingtalkService;
import com.solidoa.dingtalk.service.DingTalkAttendanceSyncService;
import com.solidoa.dingtalk.dto.AttendanceCallbackDTO;
import com.solidoa.dingtalk.vo.AttendanceStatisticsVO;
import com.solidoa.dingtalk.vo.SyncResultVO;
import com.solidoa.dingtalk.util.DingtalkSignatureUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/dingtalk")
@Slf4j
@Tag(name = "钉钉集成", description = "钉钉集成相关接口")
public class DingtalkController {

    @Autowired
    private DingtalkService dingtalkService;

    @Autowired
    private DingtalkConfigMapper configMapper;

    @Autowired
    private DingtalkCodeService dingtalkCodeService;

    @Autowired
    private DingtalkCallbackService callbackService;

    @Autowired
    private DingTalkAttendanceSyncService attendanceSyncService;

    // 内存缓存 state（生产环境建议用 Redis）
    private final Map<String, String> stateCache = new ConcurrentHashMap<>();

    /**
     * 钉钉免登授权回调
     * @param code 钉钉授权码
     * @param state 防 CSRF 状态码
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                        @RequestParam(required = false) String state,
                        HttpServletResponse response) throws IOException {
        log.info("钉钉授权回调，code: {}", maskCode(code));

        // 1. 验证 state 参数（可选但推荐）
        if (state != null && !stateCache.containsKey(state)) {
            log.warn("无效的 state 参数，疑似 CSRF 攻击");
            response.sendRedirect("solidoa://error?reason=invalid_state");
            return;
        }

        // 2. Code 防重放校验
        if (dingtalkCodeService.isCodeUsed(code)) {
            log.warn("Code 已被使用，疑似重放攻击: {}", maskCode(code));
            response.sendRedirect("solidoa://error?reason=code_reused");
            return;
        }

        // 3. 消费 code（标记为已使用）
        dingtalkCodeService.markCodeUsed(code);

        // 4. 获取用户信息
        Map<String, Object> userInfo = dingtalkService.getUserInfoByCode(code);

        // 5. 生成安全重定向 URL（不暴露敏感信息）
        String redirectUrl = "solidoa://login?dingtalk_userid=" + userInfo.get("userId");
        response.sendRedirect(redirectUrl);

        log.info("钉钉免登成功，用户: {}", userInfo.get("name"));
    }

    /**
     * 获取钉钉 OAuth 跳转 URL（用于生成带 state 的授权链接）
     */
    @GetMapping("/oauth/url")
    public Result<String> getOAuthUrl(@RequestParam String redirectUri) {
        String state = UUID.randomUUID().toString();
        stateCache.put(state, redirectUri);

        // 清理过期 state（保留最近10个）
        if (stateCache.size() > 10) {
            stateCache.clear();
        }

        DingtalkConfig config = configMapper.selectActiveConfig();
        String oauthUrl = String.format(
            "https://oapi.dingtalk.com/connect/qrconnect?app_key=%s&response_type=code&scope=snsapi_login&state=%s&redirect_uri=%s",
            config.getAppKey(), state, redirectUri
        );

        return Result.success(oauthUrl);
    }

    /**
     * 钉钉回调验证（用于接收钉钉推送事件）
     */
    @GetMapping("/check")
    public String check(@RequestParam String signature, @RequestParam Long timestamp,
                       @RequestParam String nonce, @RequestParam String echostr) {
        DingtalkConfig config = configMapper.selectActiveConfig();

        // 验证签名
        if (!DingtalkSignatureUtil.verifySignature(
                config.getCallbackToken(), timestamp.toString(), nonce, echostr, signature)) {
            log.warn("钉钉回调签名验证失败");
            return "signature_verify_failed";
        }

        log.info("钉钉回调验证成功");
        return echostr;
    }

    /**
     * 钉钉回调事件接收（异步处理，避免超时）
     */
    @PostMapping("/callback")
    public String callbackEvent(@RequestParam String signature, @RequestParam Long timestamp,
                               @RequestParam String nonce, @RequestBody String encryptedMsg) {
        log.info("收到钉钉回调事件");

        DingtalkConfig config = configMapper.selectActiveConfig();

        // 验证签名
        if (!DingtalkSignatureUtil.verifySignature(
                config.getCallbackToken(), timestamp.toString(), nonce, "", signature)) {
            log.warn("钉钉事件回调签名验证失败");
            return "signature_verify_failed";
        }

        // 异步处理（立即返回 success 给钉钉）
        new Thread(() -> {
            try {
                // TODO: 解密消息并处理
                log.info("异步处理回调消息");
                // callbackService.processCallbackEvent(eventType, eventData);
            } catch (Exception e) {
                log.error("异步处理回调消息失败", e);
            }
        }).start();

        return "success";
    }

    /**
     * 手动触发用户同步
     */
    @PostMapping("/sync/users")
    public Result<Void> syncUsers() {
        dingtalkService.syncUsers();
        return Result.success();
    }

    /**
     * 手动触发部门同步
     */
    @PostMapping("/sync/departments")
    @Operation(summary = "手动触发部门同步")
    public Result<Void> syncDepartments() {
        dingtalkService.syncDepartments();
        return Result.success();
    }

    // ==================== 钉钉考勤同步接口 ====================

    /**
     * 同步打卡记录
     */
    @GetMapping("/attendance/sync/clock")
    @Operation(summary = "同步打卡记录")
    public Result<SyncResultVO> syncClockRecords(
        @RequestParam Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(attendanceSyncService.syncClockRecords(userId, startDate, endDate));
    }

    /**
     * 批量同步打卡记录
     */
    @GetMapping("/attendance/sync/batch/clock")
    @Operation(summary = "批量同步打卡记录")
    public Result<SyncResultVO> syncBatchClockRecords(
        @RequestParam List<Long> userIds,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.success(attendanceSyncService.syncBatchClockRecords(userIds, date));
    }

    /**
     * 同步加班记录
     */
    @GetMapping("/attendance/sync/overtime")
    @Operation(summary = "同步加班记录")
    public Result<SyncResultVO> syncOvertimeRecords(
        @RequestParam Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return Result.success(attendanceSyncService.syncOvertimeRecords(userId, startTime, endTime));
    }

    /**
     * 获取考勤统计
     */
    @GetMapping("/attendance/statistics")
    @Operation(summary = "获取考勤统计")
    public Result<AttendanceStatisticsVO> getStatistics(
        @RequestParam Long userId,
        @RequestParam String month
    ) {
        return Result.success(attendanceSyncService.getStatistics(userId, month));
    }

    /**
     * 处理钉钉Webhook回调
     */
    @PostMapping("/attendance/callback")
    @Operation(summary = "处理钉钉考勤Webhook回调")
    public Result<Void> handleAttendanceCallback(@RequestBody AttendanceCallbackDTO callbackData) {
        attendanceSyncService.handleDingTalkCallback(callbackData);
        return Result.success();
    }

    /**
     * 手动触发同步所有打卡记录
     */
    @PostMapping("/attendance/sync/all")
    @Operation(summary = "手动触发同步所有打卡记录")
    public Result<Void> syncAllClockRecords() {
        attendanceSyncService.syncAllClockRecords();
        return Result.success();
    }

    /**
     * 手动检测考勤异常
     */
    @PostMapping("/attendance/detect/anomalies")
    @Operation(summary = "手动检测考勤异常")
    public Result<Void> detectAnomalies(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        attendanceSyncService.detectAnomalies(targetDate);
        return Result.success();
    }

    /**
     * Code 脱敏（安全日志）
     */
    private String maskCode(String code) {
        if (code == null || code.length() < 8) {
            return "***";
        }
        return code.substring(0, 4) + "****" + code.substring(code.length() - 4);
    }
}