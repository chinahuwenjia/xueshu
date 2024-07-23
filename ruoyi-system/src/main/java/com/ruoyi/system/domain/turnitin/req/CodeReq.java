package com.ruoyi.system.domain.turnitin.req;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class CodeReq {

    private String keyword;

    private String type;

    private Integer usageLimit;;

    private Integer validDays;

    private String businessType;

    private Integer count;

    private String linkedAccount;

    private String userId;

    private Boolean repeatable;

    private String status;

}
