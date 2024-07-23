package com.ruoyi.system.domain.turnitin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "turnitin_codes")
public class Code  implements Serializable {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    /**
     * code类型，可以是single_check=单次检查 ai=AI报告 both=既能查重又能AI time=比如30天内可以无限次查AI
     */
    private String type;

    private Integer usageLimit;

    private Integer validDays;

    private String fileName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CST")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CST")
    private Date expiryDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CST")
    private Date activeDate;

    private Integer usedCount;

    /**
     * used  active  expired
     */
    private String status;

    private Integer wordCount;


    /**
     * 这个code关联的账号
     */
    private String linkedAccount;

    private String businessType;

    private String userId;

    private Boolean repeatable;

    private String email;

    private String classId;

    private String paperId;

    private String similarityPdfUrl;

    private String similarity;

    private String aiWriting;

    private String aiWritingPdfUrl;

    private String deleteUrl;

    /**
     * 设备ID 用于绑定设备的唯一标识
     */
    private String deviceID;

    /**
     * curl 用于获取headers
     */
    private String curlString;

}
