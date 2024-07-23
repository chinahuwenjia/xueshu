package com.ruoyi.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WeChatAlertUtil {

    private static final String WEBHOOK_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=80d721bd-172e-4a2a-9146-ea1f185e63c4";

    // 发送文本消息
    public static void sendTextAlert(String content, String[] mentionedList, String[] mentionedMobileList) {
        LocalTime now = LocalTime.now();
        if (!now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(23, 0))) {
            return;
        }
        Map<String, Object> text = new HashMap<>();
        text.put("content", content);
        text.put("mentioned_list", mentionedList);
        text.put("mentioned_mobile_list", mentionedMobileList);

        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "text");
        payload.put("text", text);

        sendAlert(JSONUtil.toJsonStr(payload));
    }

    // 发送Markdown消息
    public static void sendMarkdownAlert(String content) {
        LocalTime now = LocalTime.now();
        if (!now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(23, 0))) {
            return;
        }
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("content", content);

        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);

        sendAlert(JSONUtil.toJsonStr(payload));
    }

    // 发送告警消息到微信机器人
    private static void sendAlert(String payload) {
        HttpResponse response = HttpRequest.post(WEBHOOK_URL)
                .header("Content-Type", "application/json")
                .body(payload)
                .execute();
        log.info("WeChat alert response: {}", response.body());
    }
}
