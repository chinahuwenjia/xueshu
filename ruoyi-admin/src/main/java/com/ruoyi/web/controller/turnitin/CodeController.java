package com.ruoyi.web.controller.turnitin;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.req.CodeReq;
import com.ruoyi.system.domain.turnitin.res.StatsDTO;
import com.ruoyi.system.service.CodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/system/codes")
public class CodeController extends BaseController {

    @Autowired
    private CodeService codeService;

    @Log(title = "turinitin_code", businessType = BusinessType.INSERT)
    @PostMapping("/generate/batch")
    public ResponseEntity<?> generateCodes(@RequestBody CodeReq codeReq) {
        logger.info("Generating codes with type: {}, usageLimit: {}, validDays: {}, businessType: {}, count: {}",
                codeReq.getType(), codeReq.getUsageLimit(), codeReq.getValidDays(), codeReq.getBusinessType(), codeReq.getCount());
        String type = codeReq.getType();
        Integer usageLimit = codeReq.getUsageLimit();
        Integer validDays = codeReq.getValidDays();
        String businessType = codeReq.getBusinessType();
        Integer count = codeReq.getCount();
        try {
            List<Code> codes = codeService.generateCodes(type, usageLimit, validDays, businessType, count);
            return ResponseEntity.ok(codes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCodes(@RequestParam(value = "code", required = false) String code,
                                         @RequestParam(value = "businessType", required = false) String businessType,
                                         @RequestParam(value = "status", required = false) String status,
                                         @RequestParam(value = "linkedAccount", required = false) String linkedAccount,
                                         @RequestParam(value = "email", required = false) String email,
                                         Pageable pageable) {
        try {
            logger.info("Searching codes with code: {}, businessType: {}, status: {}, linkedAccount: {}, email: {}, pageable: {}",
                    code, businessType, status, linkedAccount, email, pageable);
            Page<Code> codes = codeService.searchCodes(code,  businessType, status, linkedAccount,email, pageable);
            return ResponseEntity.ok(codes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("搜索失败: " + e.getMessage());
        }
    }

    @Log(title = "turinitin_code", businessType = BusinessType.DELETE)
    @PostMapping("/batchdelete")
    public ResponseEntity<?> deleteCodes(@RequestBody List<String> ids) {
        try {
            boolean deleted = codeService.deleteCodes(ids);
            if (deleted) {
                return ResponseEntity.ok("兑换码删除成功");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("兑换码未找到");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Code>> getAllCodes() {
        List<Code> codes = codeService.getAllCodes();
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<Code>> getCodes(Pageable pageable) {
        Page<Code> codes = codeService.getCodes(pageable);
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getCodeStats() {
        try {
            StatsDTO codeStats = codeService.getStats();
            return ResponseEntity.ok(codeStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取统计信息失败: " + e.getMessage());
        }
    }

    @Log(title = "turinitin_code", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void exportCodes(HttpServletResponse response) throws IOException {
        List<Code> codes = codeService.getAllCodes();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; file=codes.csv");

        PrintWriter writer = response.getWriter();
        writer.write("code,type,usageLimit,validDays,classId,paperId,businessType,createdAt,expiryDate,usedCount,status\n");
        for (Code code : codes) {
            writer.write(String.format("%s,%s,%d,%d,%s,%s,%s,%s,%s,%d,%s\n",
                    code.getCode(), code.getType(),
                    code.getUsageLimit() == null ? 0 : code.getUsageLimit(),
                    code.getValidDays() == null ? 0 : code.getValidDays(),
                    code.getClassId(), code.getPaperId(),
                    code.getBusinessType(), code.getCreatedAt(),
                    code.getExpiryDate(), code.getUsedCount(),
                    code.getStatus()));
        }
        writer.flush();
        writer.close();
    }

}

