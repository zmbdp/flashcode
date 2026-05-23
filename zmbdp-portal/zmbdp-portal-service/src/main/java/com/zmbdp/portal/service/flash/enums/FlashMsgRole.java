package com.zmbdp.portal.service.flash.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FlashMsgRole {

    USER_MSG("用户消息", 0),

    AI_MSG("大模型消息", 1);

    private String desc;

    private int value;
}