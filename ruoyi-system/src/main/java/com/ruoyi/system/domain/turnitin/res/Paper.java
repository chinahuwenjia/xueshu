package com.ruoyi.system.domain.turnitin.res;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class Paper {

    @JSONField(name = "id")
    private String id;

    @JSONField(name = "author")
    private String author;

    @JSONField(name = "assignment")
    private String assignment;

    @JSONField(name = "submission_url")
    private String submission_url;

    @JSONField(name = "similarity_score")
    private String similarity_score;

    @JSONField(name = "submission_trn")
    private String submission_trn;

    @JSONField(name = "glyph_url")
    private String glyph_url;

    @JSONField(name = "filename")
    private String filename;

    @JSONField(name = "word_count")
    private String word_count;

    @JSONField(name = "author_full_name")
    private String author_full_name;

    @JSONField(name = "title")
    private String title;

}
