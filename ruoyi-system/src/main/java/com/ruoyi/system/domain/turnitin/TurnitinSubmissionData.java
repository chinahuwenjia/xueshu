package com.ruoyi.system.domain.turnitin;

import com.alibaba.fastjson2.annotation.JSONField;
import com.ruoyi.system.domain.turnitin.res.Paper;
import lombok.Data;

import java.util.List;

@Data
public class TurnitinSubmissionData {
    @JSONField(name = "Paper")
    private List<Paper> papers;
}
