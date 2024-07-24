package com.ruoyi.system.domain.grammarly;

import lombok.Data;

import java.util.List;
import java.util.Map;

@lombok.Data
public class UserDTO {
    public String id;
    public String type;
    public String firstName;
    public String name;
    public String email;
    public String loginType;
    public boolean subscriptionFree;
    public boolean confirmed;
    public boolean disabled;
    public boolean free;
    public boolean anonymous;
    public boolean freemium;
    public boolean isTest;
    public boolean trusted;
    public List<String> groups;
    public Map<String, String> customFields;
    public Map<String, Object> settings;
    public String registrationDate;
    public String freemiumRegDate;
    public String extensionInstallDate;
    public InstitutionInfo institutionInfo;
    public EditorFeatures editorFeatures;
    public boolean plagiarismOn;
    public boolean institutionPlagiarismDisabled;
    public boolean institutionScoreDisabled;
    public boolean institutionLicenseDiscountinued;
    public boolean institutionQuickReplacement;
    public boolean institutionFullCards;
    public boolean institutionProofit;
    public boolean grammarlyEdu;
    public long institutionId;
    public String institutionName;
    public boolean institutionAdmin;
    public List<String> loginProviders;
    public List<Role> roles;

    @Data
    public static class InstitutionInfo {
        public long id;
        public String name;
        public boolean admin;
        public boolean expired;
        public boolean domainValidation;
        public boolean business;
        public boolean k12;
        public boolean licenseDiscountinued;
        public boolean ssoEnabled;
        public boolean ssoActivated;
        public String ssoId;
        public boolean voxEnabled;
        public VoxInfo voxInfo;
        public boolean trialSurveyEnabled;
        public String creationDate;
        public String organizationType;
        public String onboardingType;
        public int maxUsers;
        public SubscriptionInfo subscriptionInfo;

        @Data
        public static class VoxInfo {
            public String companyName;
            public boolean styleGuideEnabled;
        }

        @Data
        public static class SubscriptionInfo {
            public int volume;
            public String expirationDate;
            public boolean isOnTrial;
            public String migrationState;
        }
    }

    @Data
    public static class EditorFeatures {
        public boolean plagiarismDisabled;
        public boolean scoreDisabled;
        public boolean quickReplacement;
        public boolean fullCards;
        public boolean proofit;
        public boolean msWordEnabled;
        public boolean msOutlookEnabled;
        public boolean docsDisabled;
    }

    @Data
    public static class Role {
        public long externalId;
        public String type;
        public String role;
    }
}

