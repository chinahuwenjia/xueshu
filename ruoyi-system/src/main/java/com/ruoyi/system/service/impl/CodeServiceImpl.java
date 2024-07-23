package com.ruoyi.system.service.impl;

import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.utils.XueShuStaticParam;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.res.StatsDTO;
import com.ruoyi.system.repository.CodeRepository;
import com.ruoyi.system.service.CodeService;
import com.ruoyi.system.service.TurnitinProService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CodeServiceImpl implements CodeService {

    private static final Logger log = LoggerFactory.getLogger(CodeServiceImpl.class);
    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TurnitinProService turnitinProService;

    @Override
    public List<Code> generateCodes(String type, Integer usageLimit, Integer validDays, String businessType, Integer count) {
        log.info("Generating {} codes of type {} with usage limit {} and valid days {} for business type {}.", count, type, usageLimit, validDays, businessType);
        List<Code> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Code code = new Code();
            code.setCode(UUID.randomUUID().toString());
            code.setType(type);
            code.setUsageLimit(usageLimit);
            code.setValidDays(validDays);
            code.setCreatedAt(new Date());
            code.setUsedCount(0);
            code.setStatus("active");
            code.setBusinessType(businessType);
            codes.add(code);
        }
        return codeRepository.saveAll(codes);
    }


    @Override
    public Page<Code> searchCodes(String code, String businessType, String status, String linkedAccount, String email, Pageable pageable) {
        Query query = new Query();

        log.info("query code is {} , businessType is {} , linkedAccount is {} , status is {} , email is {}", code, businessType, linkedAccount, status, email);

        if (code != null && !code.isEmpty()) {
            query.addCriteria(Criteria.where("code").is(code));
        }
        if (email != null && !email.isEmpty()) {
            query.addCriteria(Criteria.where("email").is(email));
        }
        if (businessType != null && !businessType.isEmpty()) {
            query.addCriteria(Criteria.where("businessType").is(businessType));
        }
        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (linkedAccount != null && !linkedAccount.isEmpty()) {
            query.addCriteria(Criteria.where("linkedAccount").is(linkedAccount));
        }

        log.info("Searching codes with query: " + query.toString());

        long count = mongoTemplate.count(query, Code.class);
        query.with(pageable);
        List<Code> codes = mongoTemplate.find(query, Code.class);

        return new PageImpl<>(codes, pageable, count);

    }

    @Override
    public boolean deleteCodes(List<String> ids) {
        if (!ids.isEmpty()) {
            codeRepository.deleteAllById(ids);
            return true;
        }
        return false;
    }

    @Override
    public List<Code> getAllCodes() {
        return codeRepository.findAll();
    }

    @Override
    public Page<Code> getCodes(Pageable pageable) {
        return codeRepository.findAll(pageable);
    }

    @Override
    public Code submitCode(String code, String title, String region, MultipartFile file) {
        Code existingCode = codeRepository.findByCode(code).orElseThrow(() -> new RuntimeException("查重码不存在，请确认是否输入正确"));
        turnitinProService.submitFile(existingCode, title, region, file);
        return existingCode;
    }

    @Override
    public Code TurnitinProResult(String code) {
        log.info("开始获取查重报告，code是{}: ", code);
        Code existingCode = codeRepository.findByCode(code).orElseThrow(() -> new RuntimeException("查重码已过期或不存，请确认是否输入正确"));
        if (existingCode.getStatus().equals(XueShuStaticParam.EXPIRED)){
            throw new RuntimeException("查重码已过期，请重新购买");
        }
        if (existingCode.getStatus().equals(XueShuStaticParam.ACTIVE)){
            throw new RuntimeException("请先提交文件再进行查重");
        }
        return turnitinProService.getReport(existingCode);
    }

    public static Date addDaysToDate(Date date, int days) {
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime newLocalDateTime = localDateTime.plus(days, ChronoUnit.DAYS);
        return Date.from(newLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public Code getAICodeResult(String code) {
        Code existingCode = codeRepository.findByCode(code).orElseThrow(() -> new RuntimeException("查重码不存在，请确认是否输入正确"));
        return existingCode;
    }

    @Override
    public void updateCode(Code resultCode, String email, String paperID) {
        resultCode.setStatus(XueShuStaticParam.USED);
        resultCode.setEmail(email);
        resultCode.setPaperId(paperID);
        log.info("Updating code: " + resultCode.getCode());
        resultCode.setActiveDate(new Date(System.currentTimeMillis()));
        if (resultCode.getUsedCount() == null) {
            resultCode.setUsedCount(1);
        } else {
            resultCode.setUsedCount(resultCode.getUsedCount() + 1);
        }
        if (resultCode.getType().equals(XueShuStaticParam.TIMED)) {
            resultCode.setUsageLimit(null);
            if (resultCode.getExpiryDate() == null)
                resultCode.setExpiryDate(addDaysToDate(new Date(), resultCode.getValidDays()));
        }
        codeRepository.save(resultCode);

    }


    @Override
    public StatsDTO getStats() {
        StatsDTO stats = new StatsDTO();
        stats.setUsed(codeRepository.countByStatus("used"));
        stats.setUnredeemed(codeRepository.countByStatus("active"));
        stats.setExpired(codeRepository.countByStatus("expired"));
        stats.setTotal(codeRepository.count());
        return stats;
    }

    @Override
    public void deleteResult(String code) {
        Code existingCode = codeRepository.findByCode(code).orElseThrow(() -> new RuntimeException("删除文章失败，查重码不存在，请确认是否输入正确"));
        turnitinProService.deleteReport(existingCode);
    }


    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void expireCodes() {
        Date now = new Date(System.currentTimeMillis());
        List<Code> expiredCodes = codeRepository.findByStatusAndExpiryDateBefore("used", now);
        for (Code code : expiredCodes) {
            code.setStatus("expired");
            codeRepository.save(code);
        }
    }
}
