package com.zmbdp.portal.service.flash.domain.vo;

import lombok.Data;

/**
 * 需求文档 VO
 *
 * @author 稚名不带撇
 */
@Data
public class RequirementsVO {

    /**
     * 应用 Id
     */
    private Long appId;

    /**
     * 需求内容
     */
    private String content;
}