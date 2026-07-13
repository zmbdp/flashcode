package com.zmbdp.portal.service.flash.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenerateAppResVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long appId;

    private String previewUrl;

    private String appType;

    public GenerateAppResVO() {
    }
}