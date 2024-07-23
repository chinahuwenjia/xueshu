package com.ruoyi.web.controller.turnitin;

import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.service.LeetcodeActivationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/leetcode")
public class LeetcodeController {

    @Autowired
    private LeetcodeActivationCodeService service;

    @PostMapping("/validate")
    public Map<String, Object> validateCode(@RequestBody Map<String, String> request) {
        String code = request.get("activationCode");
        String deviceId = request.get("deviceId");

        Optional<Code> activationCode = service.validateCode(code,deviceId);

        Map<String, Object> response = new HashMap<>();
        if (activationCode.isPresent()) {
            response.put("valid", true);
            response.put("expirationTime", activationCode.get().getExpiryDate());
            response.put("curlString", activationCode.get().getCurlString());
        } else {
            response.put("valid", false);
            response.put("message", "激活码无效");
        }
        return response;
    }
}
