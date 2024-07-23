package com.ruoyi.system.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.common.utils.DingTalkRobot;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.ManagerAccount;
import com.ruoyi.system.repository.CodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class LeetcodeActivationCodeService {

    @Autowired
    private CodeRepository repository;

    @Autowired
    private TurnitinAccountService turnitinAccountService;

    public Optional<Code> validateCode(String code, String deviceId) {
        log.info("开始验证激活码：{},deviceId:{}", code, deviceId);
        Optional<Code> activationCode = repository.findByCode(code);
        if (activationCode.isPresent()) {
            Code currentCode = activationCode.get();
            long currentTime = System.currentTimeMillis();
            if (currentCode.getExpiryDate() != null && currentTime >= currentCode.getExpiryDate().getTime()) {
                throw new RuntimeException("当前激活码已过期，请重新购买");
            }
            String linkedAccount = currentCode.getLinkedAccount();
            // 保存curl
            if (StringUtils.isEmpty(linkedAccount)) {
                List<ManagerAccount> turnitinProAccountList = turnitinAccountService.getAccountByAccountType("leetcode");
                if (turnitinProAccountList.isEmpty()) {
                    throw new RuntimeException("后台无leetcode账号，咨询在线客服");
                }
                Collections.shuffle(turnitinProAccountList);
                currentCode.setCurlString(turnitinProAccountList.get(0).getCurlString());
                currentCode.setLinkedAccount(turnitinProAccountList.get(0).getAccountName());
            }else {
                ManagerAccount managerAccount = turnitinAccountService.getTurnitinProAccount(linkedAccount).orElseThrow(() -> new RuntimeException("后台无该账号，请联系管理员"));
                currentCode.setCurlString(managerAccount.getCurlString());
            }
            // 保存设备id
            String localDeviceID = currentCode.getDeviceID();
            if (!StringUtils.isEmpty(localDeviceID) && !localDeviceID.equals(deviceId)) {
                throw new RuntimeException("当前激活码已绑定设备，需要更换设备请重新购买");
            }
            currentCode.setDeviceID(deviceId);
            if (currentCode.getExpiryDate() == null) {
                currentCode.setActiveDate(new Date(currentTime));
                // 保存当前时间加上有效期code的validDays
                currentCode.setExpiryDate(new Date(currentTime + 1000 * 60 * 60 * 24 * currentCode.getValidDays()));
            }
            repository.save(currentCode);
            log.info("leetcode激活码验证成功：{},deviceId:{}", code, deviceId);
        }
        return activationCode;
    }


    private void refresh() {
        List<ManagerAccount> turnitinProAccountList = turnitinAccountService.getAccountByAccountType("leetcode");

        for (ManagerAccount managerAccount : turnitinProAccountList) {
            String curl = managerAccount.getCurlString();
            Map<String, String> headers = CookieParse.convertCurlToMap(curl);
            String url = "https://www.leetcode.com/";
            HttpRequest request = HttpUtil.createPost(url).addHeaders(headers);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                log.error("获取leetcode状态失败，账号是：{},原因是：{}", managerAccount.getAccountName(), response.body());
                DingTalkRobot.sendMsg("获取获取leetcode状态失败态失败，账号是：" + managerAccount.getAccountName() + "，原因是：" + response.body());

            }
        }
    }
}
