package com.ruoyi.web.controller.turnitin;

import com.ruoyi.system.domain.grammarly.AuthorizeDTO;
import com.ruoyi.system.domain.grammarly.TokenDTO;
import com.ruoyi.system.domain.grammarly.TreatmentDTO;
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

import java.util.List;

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

    @ApiOperation("获取Tratement，这个接口请求有多个入参，目前只实现了其中一个")
    @PostMapping("/tratement/get")
    public ResponseEntity<?> getTratement( @RequestParam("code") String code) {
        try {
            List<TreatmentDTO> treatmentDTOList = grammarlyService.getTratement(code);
            return ResponseEntity.ok(treatmentDTOList);
        }catch (Exception e) {
            log.error("Error getting tratement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }

    @ApiOperation("这是需要获得accessToken，尽量复用之前的token接口，目前有效期好像是300s，所以可以考虑缓存一下token，Grammarly-document接口,返回空串")
    @PostMapping("/document")
    public ResponseEntity<?> document( @RequestParam("code") String code) {
        try {
            List<String> treatmentDTOList = grammarlyService.getDocumentAPI(code);
            if(null != treatmentDTOList){
                return ResponseEntity.ok(treatmentDTOList);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取document接口失败");
        } catch (Exception e) {
            log.error("Error getting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }
}
