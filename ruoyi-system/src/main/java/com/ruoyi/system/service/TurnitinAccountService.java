package com.ruoyi.system.service;

import com.ruoyi.system.domain.turnitin.ManagerAccount;

import java.util.List;
import java.util.Optional;

public interface TurnitinAccountService {

    public List<ManagerAccount> getAllAccounts() ;

    public ManagerAccount addAccount(ManagerAccount account);

    public ManagerAccount updateAccount(ManagerAccount account) ;

    public void deleteAccount(String id) ;

    public Optional<ManagerAccount> getAccountById(String id) ;

    List<ManagerAccount> getAccountByAccountType(String accountType);

    Optional<ManagerAccount> getTurnitinProAccount(String accountName);
}
