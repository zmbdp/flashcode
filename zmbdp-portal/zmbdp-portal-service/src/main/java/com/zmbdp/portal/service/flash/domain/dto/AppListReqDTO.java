package com.zmbdp.portal.service.flash.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppListReqDTO extends BasePageReqDTO implements Serializable {

    private Integer appType;

    private Integer deployStatus;

    private Long userId;

    private int listType;
}