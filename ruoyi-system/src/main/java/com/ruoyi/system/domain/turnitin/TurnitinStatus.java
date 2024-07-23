package com.ruoyi.system.domain.turnitin;

import lombok.Data;

@Data
public class TurnitinStatus {

    private int code;

    private String status;

    private String msg;

    private String downloaderUrl;
}
