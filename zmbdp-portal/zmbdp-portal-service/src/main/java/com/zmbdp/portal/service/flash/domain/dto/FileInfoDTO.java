package com.zmbdp.portal.service.flash.domain.dto;

import lombok.Data;

@Data
public class FileInfoDTO {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件内容
     */
    private String content;
}