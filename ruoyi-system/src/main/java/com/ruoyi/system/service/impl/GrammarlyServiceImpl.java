package com.ruoyi.system.service.impl;


import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.common.utils.XueShuStaticParam;
import com.ruoyi.system.domain.grammarly.AuthorizeDTO;
import com.ruoyi.system.domain.grammarly.TokenDTO;
import com.ruoyi.system.domain.grammarly.TreatmentDTO;
import com.ruoyi.system.domain.grammarly.UserDTO;
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
import java.util.Map;

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
        if (CollectionUtils.isEmpty(managerAccounts)) {
            log.error("Grammarly账号不存在");
            return false;
        }
        Collections.shuffle(managerAccounts);
        ManagerAccount managerAccount = managerAccounts.get(0);
        GrammarlyDocumentRes grammarlyDocumentRes = GrammarlyOAuth2Client.uploadFileToGrammarly(currentCode, file, managerAccount);
        if (grammarlyDocumentRes == null) {
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

    @Override
    public AuthorizeDTO auth(String code) {
        ManagerAccount managerAccount = getManagerAccount(code);
        String curl = managerAccount.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        String codeVerifier = GrammarlyOAuth2Client.generateCodeVerifier();
        return GrammarlyOAuth2Client.getAuthorizationCode(codeVerifier, headers);
    }

    private ManagerAccount getManagerAccount(String code) {
        Code currentCode = codeRepository.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Grammarlt检查码不存在"));
        List<ManagerAccount> managerAccounts = grammarlyRepository.findByAccountType("grammarly");
        if (CollectionUtils.isEmpty(managerAccounts)) {
            throw new RuntimeException("auth:Grammarly账号不存在");
        }
        String linkedAccount = currentCode.getLinkedAccount();
        ManagerAccount managerAccount = null;
        if (linkedAccount != null) {
            managerAccount = grammarlyRepository.findByAccountName(linkedAccount);
        }
        if (linkedAccount == null || managerAccount == null) {
            Collections.shuffle(managerAccounts);
            managerAccount = managerAccounts.get(0);
            currentCode.setLinkedAccount(managerAccount.getAccountName());
            currentCode.setCurlString(managerAccount.getCurlString());
        }
        return managerAccount;
    }

    @Override
    public TokenDTO getToken(String code) {
        ManagerAccount managerAccount = getManagerAccount(code);
        String curl = managerAccount.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        String codeVerifier = GrammarlyOAuth2Client.generateCodeVerifier();
        AuthorizeDTO authorizeDTO = GrammarlyOAuth2Client.getAuthorizationCode(codeVerifier, headers);
        return GrammarlyOAuth2Client.getTokens(authorizeDTO.getCode(), codeVerifier, headers);
    }

    @Override
    public UserDTO userV3(String code) {
        ManagerAccount managerAccount = getManagerAccount(code);
        String curl = managerAccount.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
       return GrammarlyOAuth2Client.getUser(headers);
    }

    @Override
    public List<TreatmentDTO> getTratement(String code) {
        ManagerAccount managerAccount = getManagerAccount(code);
        String curl = managerAccount.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        return GrammarlyOAuth2Client.treatmentGet(headers);
    }

    @Override
    public List<String> getDocumentAPI(String code) {
        ManagerAccount managerAccount = getManagerAccount(code);
        String curl = managerAccount.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        return GrammarlyOAuth2Client.getDocumentAPI(headers);
    }


}
