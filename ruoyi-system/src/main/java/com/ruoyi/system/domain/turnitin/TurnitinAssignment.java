package com.ruoyi.system.domain.turnitin;


import lombok.Data;

@Data
public class TurnitinAssignment {

    private String assignmentId;
    private String assignmentTitle;
    private String assignmentType;
    private String assignmentStatus;
    private String assignmentDueDate;
    private String url;
}
