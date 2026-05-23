package com.zmbdp.portal.service.flash.enums;

import lombok.Getter;

@Getter
public enum DeployStatus {

    NOT_DEPLOYED(0, "未部署"),

    DEPLOYED(1, "已部署");

    private Integer value;

    private String desc;

    DeployStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}