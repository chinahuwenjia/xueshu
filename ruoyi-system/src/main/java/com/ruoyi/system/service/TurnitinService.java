package com.ruoyi.system.service;

import com.ruoyi.system.domain.turnitin.Code;
import org.springframework.web.multipart.MultipartFile;

public interface TurnitinService {

    boolean submitAssignment(String assignmentId, MultipartFile file);


    String getAiReport(Code code, String paperID);
}
