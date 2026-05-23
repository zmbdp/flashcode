package com.zmbdp.portal.service.flash.enums;

import lombok.Getter;

@Getter
public enum PreviewDeployPath {

    PREVIEW("user-preview", "预览地址"),

    DEPLOY("user-deploy", "部署地址");

    private String path;
    private String desc;

    PreviewDeployPath(String path, String desc) {
        this.path = path;
        this.desc = desc;
    }
}