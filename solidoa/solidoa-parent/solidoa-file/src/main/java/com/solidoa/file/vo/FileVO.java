package com.solidoa.file.vo;

import lombok.Data;

@Data
public class FileVO {
    private Long id;
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String bucket;
    private String businessType;
    private Long businessId;
    private Long uploaderId;
    private String createTime;
}