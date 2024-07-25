package com.ruoyi.system.domain.grammarly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@lombok.Data
public class TreatmentDTO {
    @JsonProperty("experimentName")
    private String experimentName;
    @JsonProperty("qualifiedName")
    private String qualifiedName;
    @JsonProperty("experimentId")
    private String experimentId;
    @JsonProperty("groupName")
    private String groupName;
    @JsonProperty("userId")
    private long userId;
    @JsonProperty("containerId")
    private String containerId;
    @JsonProperty("overrideType")
    private String overrideType;
    @JsonProperty("isTest")
    private boolean isTest;
    @JsonProperty("type")
    private String type;
    @JsonProperty("sender")
    private String sender;
    @JsonProperty("needLog")
    private boolean needLog;
    @JsonProperty("holdoutTreatment")
    private HoldoutTreatment holdoutTreatment;

    // Getters and setters

    @lombok.Data
    public static class HoldoutTreatment {
        @JsonProperty("experimentName")
        private String experimentName;
        @JsonProperty("qualifiedName")
        private String qualifiedName;
        @JsonProperty("experimentId")
        private String experimentId;
        @JsonProperty("groupName")
        private String groupName;
        @JsonProperty("userId")
        private long userId;
        @JsonProperty("containerId")
        private String containerId;
        @JsonProperty("overrideType")
        private String overrideType;
        @JsonProperty("isTest")
        private boolean isTest;
        @JsonProperty("type")
        private String type;
        @JsonProperty("sender")
        private String sender;
        @JsonProperty("needLog")
        private boolean needLog;

    }
}
