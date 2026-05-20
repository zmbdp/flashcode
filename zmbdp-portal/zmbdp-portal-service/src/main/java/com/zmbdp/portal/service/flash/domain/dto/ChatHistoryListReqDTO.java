package com.zmbdp.portal.service.flash.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatHistoryListReqDTO extends BasePageReqDTO implements Serializable {

    /**
     * 应用的主键Id
     */
    private Long appId;
}