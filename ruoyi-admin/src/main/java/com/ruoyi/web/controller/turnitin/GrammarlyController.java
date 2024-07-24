package com.ruoyi.web.controller.turnitin;

import com.ruoyi.system.domain.grammarly.AuthorizeDTO;
import com.ruoyi.system.domain.grammarly.TokenDTO;
import com.ruoyi.system.domain.grammarly.UserDTO;
import com.ruoyi.system.service.GrammarlyService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/grammarly")
@Slf4j
public class GrammarlyController {
    @Autowired
    private GrammarlyService grammarlyService;

    @ApiOperation("Grammarly文件提交")
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestParam("code") String activeCode,
                                    @RequestParam("file") MultipartFile file) {

        log.info("fileName: {}, code: {}", file.getOriginalFilename(), activeCode);
        try {
            Boolean result = grammarlyService.submit(activeCode, file);
            if (result) {
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("failed");
            }
        } catch (Exception e) {
            log.error("Error submitting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }

    @ApiOperation("Grammarly授权接口")
    @PostMapping("/auth")
    public ResponseEntity<?> auth(@RequestParam("code") String code) {
        try {
            AuthorizeDTO authorizeDTO = grammarlyService.auth(code);
            return ResponseEntity.ok(authorizeDTO);
        } catch (Exception e) {
            log.error("Error authorizing code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }


    @ApiOperation("Grammarly-token接口")
    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestParam("code") String code) {
        try {
            TokenDTO token = grammarlyService.getToken(code);
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("Error getting token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }

    @ApiOperation("Grammarly获取用户信息接口")
    @PostMapping("/user/v3")
    public ResponseEntity<?> userV3(@ApiParam(value = "通过兑换码去查询用户信息", required = true) @RequestParam("code") String code) {
        try {
            UserDTO result = grammarlyService.userV3(code);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }
}
