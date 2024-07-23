package com.ruoyi.web.controller.turnitin;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.utils.XueShuStaticParam;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.TurnitinStatus;
import com.ruoyi.system.service.CodeService;
import com.ruoyi.system.service.TurnitinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Date;


@RestController
@RequestMapping("/turnitin")
@Slf4j
public class TurnitinController extends BaseController {

    @Autowired
    private CodeService codeService;

    @Autowired
    private TurnitinService turnitinService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitCode(@RequestParam("code")  @NotNull String code,
                                        @RequestParam("title") @NotNull  String title,
                                        @RequestParam("region") @NotNull String region,
                                        @RequestParam("file")  @NotNull MultipartFile file) {
        try {
            Code resultCode = codeService.getAICodeResult(code);
            // 校验状态
            validatorStatus(resultCode);
            Code submittedCode = codeService.submitCode(code, title, region, file);
            return ResponseEntity.ok(submittedCode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("提交失败: " + e.getMessage());
        }
    }

    @GetMapping("/result")
    public ResponseEntity<?> getCodeResult(@RequestParam("code") String code) {
        try {
            Code resultCode = codeService.TurnitinProResult(code);
            return ResponseEntity.ok(resultCode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(" " + e.getMessage());
        }
    }

    @GetMapping("/deleteResult")
    public ResponseEntity<?> deleteCodeResult(@RequestParam("code") String code) {
        try {
            codeService.deleteResult(code);
            return ResponseEntity.ok("删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败: " + e.getMessage());
        }
    }


    @GetMapping("/aiResult")
    public ResponseEntity<?> getAICodeResult(@RequestParam("code")@NotNull String code,
                                             @RequestParam("paperID") @NotNull String paperID,
                                             @RequestParam("email") @Email @NotNull  String email) {
        log.info("resultCode: " + code);
        try {
            Code resultCode = codeService.getAICodeResult(code);

            // 校验状态
            validatorStatus(resultCode);
            // 获取报告
            String AIReportLink = turnitinService.getAiReport(resultCode, paperID);
            // 修改resultCode状态
            codeService.updateCode(resultCode,email,paperID);
            // 封装返回报告链接
            TurnitinStatus turnitinStatus = new TurnitinStatus();
            turnitinStatus.setCode(HttpStatus.OK.value());
            turnitinStatus.setDownloaderUrl(AIReportLink);
            return ResponseEntity.ok(turnitinStatus);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("获取失败：" + e.getMessage());
        }
    }

    public void validatorStatus(Code resultCode) throws Exception {
        String status = resultCode.getStatus();
        if (StringUtil.isEmpty(status)) {
            throw new Exception("该查重码状态异常");
        }

        if (status.equals(XueShuStaticParam.EXPIRED)){
            throw new Exception("该查重码已过期");
        }
        if (resultCode.getExpiryDate()!= null && resultCode.getExpiryDate().before(new Date())){
            throw new Exception("该查重码已过期");
        }
        if (status.equals(XueShuStaticParam.USED)) {
            // 如果usedCount不为null，并且可使用次数超过usageLimit，则返回已经使用超限了
            if (resultCode.getUsedCount() != null && resultCode.getUsageLimit() != null && resultCode.getUsedCount() >= resultCode.getUsageLimit()) {
                throw new Exception("查重码已使用超限，可用次数为：" + resultCode.getUsageLimit());
            }

            // 如果激活时间与有效天数不为空，并且激活时间加有效天数小于当前时间，则激活码过期
            if (resultCode.getActiveDate() != null && resultCode.getValidDays() != null) {

                Date activeDate = resultCode.getActiveDate();
                long activeTime = activeDate.getTime();
                long validTime = activeTime + resultCode.getValidDays() * 24 * 60 * 60 * 1000L;
                log.info("activeTime: " + activeTime + ", validTime: " + validTime);
                // 比较当前时间和有效时间
                if (System.currentTimeMillis() > validTime) {
                    throw new Exception("查重码已过期，可用次数为：" + resultCode.getUsageLimit());
                }
            }
        }
    }
}
