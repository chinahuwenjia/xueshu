package com.ruoyi.system.service;

import com.ruoyi.system.domain.turnitin.Code;
import org.springframework.web.multipart.MultipartFile;

public interface GrammarlyService {

    boolean submit(String code, MultipartFile file);

    boolean deleteFile(String code);

    Code getFile(String code);
}
