package com.ruoyi.system.domain.grammarly;

import lombok.Data;

@Data
public class TokenDTO {
    private String access_token;
    private String refresh_token;
    private Integer expires_in;
    private String device_id;
    private Long refresh_token_expires_in;
}
