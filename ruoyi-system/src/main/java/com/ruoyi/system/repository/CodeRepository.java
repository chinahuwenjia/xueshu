package com.ruoyi.system.repository;

import com.ruoyi.system.domain.turnitin.Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CodeRepository extends MongoRepository<Code, String> {

    // 根据邮箱或者兑换码+业务类型+状态+分页查询(code,email, businessType, status, pageable,linkedAccount)如果有则精确查询,email支持模糊查询
    Page<Code> findByEmailOrCodeAndBusinessTypeAndStatusAndLinkedAccount(String code, String email, String businessType, String status, String linkedAccount, Pageable pageable);

    long countByStatus(String status);

    List<Code> findByStatusAndExpiryDateBefore(String active, Date expiryDate);

    Optional<Code> findByCode(String code);

    void deleteByCode(String code);
}
