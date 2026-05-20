package com.zmbdp.portal.service.flash.domain.dto;

import lombok.Data;

/**
 * 需求文档 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class RequirementsDTO {

    /**
     * 应用 Id
     */
    private Long appId;

    /**
     * 需求内容
     */
    private String content;
}