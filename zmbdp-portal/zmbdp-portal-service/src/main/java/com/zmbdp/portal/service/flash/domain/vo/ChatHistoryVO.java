package com.zmbdp.portal.service.flash.domain.vo;

import lombok.Data;

@Data
public class ChatHistoryVO {

    /**
     * 消息主键id
     */
    private Long id;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 消息类型 0：用户，1：大模型
     */
    private int msgRole;

    /**
     * 消息内容
     */
    private String content;
}