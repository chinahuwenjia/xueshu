package com.ruoyi.web.controller.tool;

import com.google.gson.Gson;
import com.kuaidi100.sdk.response.SubscribeResp;
import com.kuaidi100.sdk.utils.SignUtils;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.ParcelStatusMapper;
import com.ruoyi.common.utils.WeChatAlertUtil;
import com.ruoyi.system.domain.ParcelStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/callback")
public class CallbackController {
    @Autowired
    private RedisCache redisCache;

    @PostMapping("/status")
    public SubscribeResp handleStatusUpdate(HttpServletRequest request) throws Exception {
        String paramJson = request.getParameter("param");
        String sign = request.getParameter("sign");
        String salt = "";
        String ourSign = SignUtils.sign(paramJson + salt);
        log.info("ourSign|{}|sign|{}", ourSign, sign);
        //加密如果相等，属于快递100推送；否则可以忽略掉当前请求

        SubscribeResp response = new SubscribeResp();
        if (paramJson == null) {
            response.setResult(false);
            response.setReturnCode("400");
            response.setMessage("Bad Request: Missing 'sign' or 'param'");
            return response;
        }

        Map<String, Object> param = new Gson().fromJson(paramJson, Map.class);
        // 假设更新包含字段 "number" 和 "state"
        if (param == null) {
            response.setResult(false);
            response.setReturnCode("400");
            response.setMessage("Bad Request: Invalid 'param'");
            return response;
        }

        Map<String, Object> lastResult = (Map<String, Object>) param.get("lastResult");
        if (lastResult == null) {
            response.setResult(false);
            response.setReturnCode("400");
            response.setMessage("Bad Request: Missing 'lastResult'");
            return response;
        }

        String number = (String) lastResult.get("nu");
        int state = Integer.parseInt((String) lastResult.get("state"));
        String stateDescription = ParcelStatusMapper.getStatusDescription(state);

        ParcelStatus optionalStatus = redisCache.getCacheObject(number);
        ParcelStatus currentStatus = Optional.ofNullable(optionalStatus).orElse(null);
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursInMillis = TimeUnit.HOURS.toMillis(20);
        long fourHoursInMillis = TimeUnit.HOURS.toMillis(4);
        // 如果当前状态为收件中，ParcelStatusMapper这个类里有映射关系，可以直接获取状态描述，并且当前时间距离上次更新时间超过24小时，并且距离上次更新时间超过4小时，则触发告警
        if (currentStatus != null) {
            // 检查是否在状态1、101、102、103之间
            if ((currentStatus.getState() == 1 || currentStatus.getState() == 101 || currentStatus.getState() == 102) &&
                    (state == 1 || state == 101 || state == 102)) {
                // 检查状态是否已经持续24小时
                if (currentTime - currentStatus.getStateStartTime() >= twentyFourHoursInMillis &&
                        currentTime - currentStatus.getLastAlertTime() >= fourHoursInMillis) {
                    triggerAlert(number, lastResult, stateDescription, currentTime - currentStatus.getStateStartTime());
                    currentStatus.setLastAlertTime(currentTime);
                }
            } else {
                // 状态发生变化，更新状态和状态开始时间
                currentStatus.setState(state);
                currentStatus.setStateStartTime(currentTime);
            }
            // 更新最后更新时间
            currentStatus.setLastUpdateTime(currentTime);
        } else {
            // 新的状态记录
            currentStatus = new ParcelStatus(number, state, currentTime, currentTime, 0, sign);
        }
        redisCache.setCacheObject(number, currentStatus, 7, TimeUnit.DAYS);

        response.setResult(true);
        response.setReturnCode("200");
        response.setMessage("提交成功");
        return response;
    }


    private void triggerAlert(String number, Map<String, Object> lastResult, String stateDescription, long durationInMillis) {
        String com = (String) lastResult.get("com");
        List<Map<String, String>> data = (List<Map<String, String>>) lastResult.get("data");

        StringBuilder dataBuilder = new StringBuilder();
        for (Map<String, String> entry : data) {
            dataBuilder.append(">时间: ").append(entry.get("time")).append("\n")
                    .append(">状态: ").append(entry.get("context")).append("\n")
                    .append(">地点: ").append(entry.get("areaName")).append("\n")
                    .append("\n");
        }

        long durationInHours = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        long durationInDays = durationInHours / 24;
        durationInHours = durationInHours % 24;
        long durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis) % 60;

        String content = String.format(
                "### 快递即将超时，速度查看\n" +
                        ">快递公司: **%s**\n" +
                        ">状态: **%s** (%s)\n" +
                        ">运单号: **%s**\n" +
                        ">物流信息:\n%s" +
                        ">已经持续时间: **%d天 %d小时 %d分钟**",
                com, stateDescription, lastResult.get("state"), number, dataBuilder, durationInDays, durationInHours, durationInMinutes
        );
        WeChatAlertUtil.sendMarkdownAlert(content);
        WeChatAlertUtil.sendMarkdownAlert(content);
    }
}