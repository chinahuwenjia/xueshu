package com.ruoyi.system.domain.turnitin;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = "turnitin_teacher")
@Data
public class ManagerAccount implements Serializable {

    @Id
    private String id;

    @Indexed(unique = true)
    private String accountName;

    private String curlString;

    private String accountType; // For example: 'check', 'ai'

    private String userID;

}
