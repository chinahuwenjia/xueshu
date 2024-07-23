package com.ruoyi.common.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.time.LocalTime;

public  class DingTalkRobot {

    private final static String SEND_URL = "https://oapi.dingtalk.com/robot/send?access_token=02f64549442902aee9986b00dc448e6d4f016325fdf1c253e318da2b3be2b48b";

    public static void sendMsg(String message) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(23, 30))) {
            message = "【grammarly报警】" + message;
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("msgtype", "text").putOpt("text", new JSONObject().putOpt("content", message));
            HttpUtil.createPost(SEND_URL)
                    .contentType("application/json")
                    .body(JSONUtil.toJsonStr(jsonObject))
                    .execute();
        }
    }

    public static void sendMarkdownMsg(String message) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(23, 30))) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("msgtype", "markdown").putOpt("markdown", new JSONObject().putOpt("title", "grammarly报警").putOpt("text", message));
            HttpUtil.createPost(SEND_URL)
                    .contentType("application/json")
                    .body(JSONUtil.toJsonStr(jsonObject))
                    .execute();
        }
    }

}
