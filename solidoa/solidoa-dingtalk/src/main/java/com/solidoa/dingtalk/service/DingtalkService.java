package com.solidoa.dingtalk.service;

import java.util.Map;

public interface DingtalkService {
    String getAccessToken();
    void syncUsers();
    void syncDepartments();
    void sendWorkNotify(Long userId, String title, String content);
    Map<String, Object> getUserInfoByCode(String code);
}
