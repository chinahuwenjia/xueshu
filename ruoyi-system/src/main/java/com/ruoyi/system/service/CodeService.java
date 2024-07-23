package com.ruoyi.system.service;

import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.res.StatsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface CodeService {
    List<Code> generateCodes(String type, Integer usageLimit, Integer validDays, String businessType, Integer count);
    Page<Code> searchCodes(String code, String businessType, String status, String linkedAccount, String email,Pageable pageable);
    boolean deleteCodes(List<String> ids);
    List<Code> getAllCodes();
    Page<Code> getCodes(Pageable pageable);
    void expireCodes();
    Code submitCode(String code, String region, String author, MultipartFile fileName);
    Code TurnitinProResult(String code);

    Code getAICodeResult(String code);

    void updateCode(Code code, String email, String paperID);

    StatsDTO getStats();

    void deleteResult(String code);
}
