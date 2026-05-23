package com.zmbdp.portal.service.flash.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenerateAppResDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long appId;

    private String previewUrl;

    private String appType;
}