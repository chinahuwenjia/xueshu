package com.ruoyi.system.service;

import com.ruoyi.system.domain.turnitin.Code;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface TurnitinProService {

    boolean submitFile(Code code, String title, String region, MultipartFile file) ;

    Code getReport(Code code) ;

    void deleteReport(Code code);
}
