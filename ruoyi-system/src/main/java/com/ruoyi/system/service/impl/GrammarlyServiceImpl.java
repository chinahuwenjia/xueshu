package com.ruoyi.system.service.impl;


import com.ruoyi.common.utils.XueShuStaticParam;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.GrammarlyDocumentRes;
import com.ruoyi.system.domain.turnitin.ManagerAccount;
import com.ruoyi.system.repository.CodeRepository;
import com.ruoyi.system.repository.GrammarlyRepository;
import com.ruoyi.system.service.GrammarlyService;
import com.ruoyi.system.service.token.GrammarlyOAuth2Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GrammarlyServiceImpl implements GrammarlyService {

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private GrammarlyRepository grammarlyRepository;

    @Override
    public boolean submit(String code, MultipartFile file) {
        Code currentCode = codeRepository.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Grammarlt检查码不存在"));
        List<ManagerAccount> managerAccounts = grammarlyRepository.findByAccountType("grammarly");
        if (CollectionUtils.isEmpty(managerAccounts)){
            log.error("Grammarly账号不存在");
            return false;
        }
        Collections.shuffle(managerAccounts);
        ManagerAccount managerAccount = managerAccounts.get(0);
        GrammarlyDocumentRes  grammarlyDocumentRes = GrammarlyOAuth2Client.uploadFileToGrammarly(currentCode ,file,  managerAccount);
        if (grammarlyDocumentRes == null){
            return false;
        }
        currentCode.setLinkedAccount(managerAccount.getAccountName());
        currentCode.setWordCount(grammarlyDocumentRes.getSize());
        currentCode.setStatus(XueShuStaticParam.USED);
        currentCode.setPaperId(String.valueOf(grammarlyDocumentRes.getId()));
        codeRepository.save(currentCode);
        return true;
    }


    @Override
    public boolean deleteFile(String code) {
        return false;
    }

    @Override
    public Code getFile(String code) {
        return null;
    }
}
