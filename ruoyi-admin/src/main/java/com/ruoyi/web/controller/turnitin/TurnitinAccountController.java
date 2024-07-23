package com.ruoyi.web.controller.turnitin;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.turnitin.ManagerAccount;
import com.ruoyi.system.service.TurnitinAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ruoyi.common.utils.PageUtils.startPage;

@RestController
@RequestMapping("/system/turnitin/teacher")
public class TurnitinAccountController extends BaseController {
    @Autowired
    private TurnitinAccountService service;

    @GetMapping
    public List<ManagerAccount> getAllAccounts() {
        startPage();
        return service.getAllAccounts();
    }

    @Log(title = "turinitin", businessType = BusinessType.INSERT)
    @PostMapping
    @PreAuthorize("@ss.hasPermi('turnitin:account:add')")
    public ManagerAccount addAccount(@RequestBody ManagerAccount account) {
        ManagerAccount savedAccount = new ManagerAccount();
        savedAccount.setAccountName(account.getAccountName());
        savedAccount.setAccountType(account.getAccountType());
        savedAccount.setCurlString(account.getCurlString());
        savedAccount.setUserID(account.getUserID());
        return service.addAccount(savedAccount);
    }

    @Log(title = "turinitin", businessType = BusinessType.UPDATE)
    @PutMapping
    public ManagerAccount updateAccount(@RequestBody ManagerAccount account) {
        ManagerAccount savedAccount = service.getAccountById(account.getId()).orElse(null);
        if (savedAccount == null) {
            throw new IllegalArgumentException("要编辑的账号不存在，请刷新页面");
        }
        savedAccount.setAccountName(account.getAccountName());
        savedAccount.setAccountType(account.getAccountType());
        savedAccount.setCurlString(account.getCurlString());
        savedAccount.setUserID(account.getUserID());
        return service.updateAccount(account);
    }

    @Log(title = "turinitin", businessType = BusinessType.DELETE)
    @PostMapping("/{id}")
    public void deleteAccount(@PathVariable String id) {
        service.deleteAccount(id);
    }


    @GetMapping("/accounts")

    public ResponseEntity<?> getLinkedAccounts(@RequestParam  String  businessType) {
        List<ManagerAccount> accounts = service.getAccountByAccountType(businessType);
        return ResponseEntity.ok(accounts);
    }

}
