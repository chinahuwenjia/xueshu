package com.ruoyi.web.controller.turnitin;

import com.ruoyi.system.service.GrammarlyService;
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
}
