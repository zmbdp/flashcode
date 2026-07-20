package com.zmbdp.portal.service.flash.service;

import com.zmbdp.portal.service.flash.domain.dto.GenerateAppResDTO;

public interface IAppGenerationService {

    /**
     * 生成应用
     *
     * @param appId  应用 id
     * @param appDoc 需求文档
     * @return 生成的应用信息
     */
    GenerateAppResDTO appGenerate(Long appId, String appDoc);
}