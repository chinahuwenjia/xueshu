package com.ruoyi.system.repository;

import com.ruoyi.system.domain.turnitin.ManagerAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GrammarlyRepository extends MongoRepository<ManagerAccount, String> {
    Optional<ManagerAccount> findByAccountName(String accountName);

    List<ManagerAccount> findByAccountType(String accountType);

}
