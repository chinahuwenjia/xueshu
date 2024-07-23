package com.ruoyi.system.domain.turnitin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GrammarlyDocumentRes {

    private long id;
    private long userId;
    private String createdAt;
    private String updatedAt;
    private Integer size;
    private String first_content;

    /**
     * 0未删除 1被删除
     */
    private String is_deleted;

    private String document_id;
    private String title;


}
