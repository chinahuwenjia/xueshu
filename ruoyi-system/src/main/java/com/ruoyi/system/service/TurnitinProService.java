package com.ruoyi.system.service;

import com.ruoyi.system.domain.turnitin.Code;
import org.springframework.web.multipart.MultipartFile;

public interface TurnitinProService {

    boolean submitFile(Code code, String title, String region, MultipartFile file, String excludeBibliography, String excludeQuotes, String excludeSmallMatchesMethod, int excludeSmallMatchesValueWords, int excludeSmallMatchesValuePercentage) ;

    Code getReport(Code code) ;

    void deleteReport(Code code);
}
