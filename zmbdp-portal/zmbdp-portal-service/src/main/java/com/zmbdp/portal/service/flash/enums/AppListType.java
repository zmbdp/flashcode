package com.zmbdp.portal.service.flash.enums;

import lombok.Getter;

@Getter
public enum AppListType {

    DEPLOY(0, "已部署"),

    MY(1, "我的应用");

    private int value;

    private String desc;

    AppListType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}