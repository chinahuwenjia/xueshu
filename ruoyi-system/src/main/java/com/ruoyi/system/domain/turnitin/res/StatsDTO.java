package com.ruoyi.system.domain.turnitin.res;


import lombok.Data;

@Data
public class StatsDTO {

    private long used;
    private long unredeemed;
    private long expired;
    private long total;

}
