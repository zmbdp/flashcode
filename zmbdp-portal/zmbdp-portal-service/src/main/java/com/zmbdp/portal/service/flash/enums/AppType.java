package com.zmbdp.portal.service.flash.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppType {

    HTML("HTML", 0),

    VUE("VUE", 1),

    VUE_SPRING("VUE_SPRING", 2);

    private String type;

    private int value;

    public static int getValue(String type) {
        for (AppType appType : AppType.values()) {
            if (appType.getType().equals(type)) {
                return appType.getValue();
            }
        }
        return -1;
    }

    public static String getAppType(int value) {
        for (AppType appType : AppType.values()) {
            if (appType.getValue() == value) {
                return appType.getType();
            }
        }
        return null;
    }
}