package com.ruoyi.system.domain.turnitin.res;

import lombok.Data;

import java.util.Map;

@Data
public class SwsLaunchToken {
    private String timestamp;
    private String token;
    private Payload payload;

    // Getters and Setters
    @Data
    public static class Payload {
        private String id;
        private long issuedAt;
        private long expiration;
        private String subject;
        private String issuer;
        private String audience;
        private String[] authorities;
        private Config config;

        @Data
        public static class Config {
            private Map<String, Submission> submissions;
            private Tenant tenant;
            private Sidebar sidebar;

            // Getters and Setters
            @Data
            public static class Submission {
                private String author;
                private String title;

                // Getters and Setters
            }

            @Data
            public static class Tenant {
                private int nodeId;
                private String id;

                // Getters and Setters
            }

            @Data
            public static class Sidebar {
                private Modes modes;
                private String defaultMode;

                // Getters and Setters
                @Data
                public static class Modes {
                    private boolean flags;

                    // Getters and Setters
                }
            }
        }
    }
}
