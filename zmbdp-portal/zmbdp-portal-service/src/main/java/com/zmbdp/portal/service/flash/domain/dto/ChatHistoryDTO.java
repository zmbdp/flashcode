package com.zmbdp.portal.service.flash.domain.dto;

import lombok.Data;

@Data
public class ChatHistoryDTO {

    private Long id;

    private Long appId;

    private int msgRole;

    private String content;
}