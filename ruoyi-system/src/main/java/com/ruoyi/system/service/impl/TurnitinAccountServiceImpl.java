package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.turnitin.ManagerAccount;
import com.ruoyi.system.repository.TurnitinTeacherRepository;
import com.ruoyi.system.service.TurnitinAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TurnitinAccountServiceImpl implements TurnitinAccountService {

    @Autowired
    private TurnitinTeacherRepository repository;

    public List<ManagerAccount> getAllAccounts() {
        return repository.findAll();
    }

    public ManagerAccount addAccount(ManagerAccount account) {
        return repository.save(account);
    }

    public ManagerAccount updateAccount(ManagerAccount account) {
        return repository.save(account);
    }

    public void deleteAccount(String id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ManagerAccount> getAccountById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<ManagerAccount> getAccountByAccountType(String accountType) {
        return repository.findByAccountType(accountType);
    }

    @Override
    public Optional<ManagerAccount> getTurnitinProAccount(String accountName) {
        return repository.findByAccountName(accountName);
    }
}
