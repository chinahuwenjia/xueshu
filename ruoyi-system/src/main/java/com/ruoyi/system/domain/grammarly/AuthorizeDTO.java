package com.ruoyi.system.domain.grammarly;

import lombok.Data;

@Data
public class AuthorizeDTO {

    /**
     * The code returned by the OAuth2 provider.
     */
    private String code;

    /**
     * The state parameter passed to the OAuth2 provider.
     */
    private String state;
}
