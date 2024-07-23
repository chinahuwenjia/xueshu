package com.ruoyi.system.domain;

import jdk.nashorn.internal.objects.annotations.Constructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@RedisHash("ParcelStatus")
public class ParcelStatus implements Serializable {
    @Id
    private String number;
    private int state;
    private long lastUpdateTime;
    private long stateStartTime;
    private long lastAlertTime;
    private String sign;

    public ParcelStatus(String number, int state, long lastUpdateTime, long stateStartTime, long lastAlertTime, String sign) {
        this.number = number;
        this.state = state;
        this.lastUpdateTime = lastUpdateTime;
        this.stateStartTime = stateStartTime;
        this.lastAlertTime = lastAlertTime;
        this.sign = sign;
    }

}