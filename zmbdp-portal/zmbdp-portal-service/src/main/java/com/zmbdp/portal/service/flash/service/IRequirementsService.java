package com.zmbdp.portal.service.flash.service;

import com.zmbdp.portal.service.flash.domain.dto.RequirementsDTO;

/**
 * 应用需求生成服务
 *
 * @author 稚名不带撇
 */
public interface IRequirementsService {

    /**
     * 生成应用需求
     *
     * @param input 用户输入
     * @return 应用需求
     */
    RequirementsDTO requirementsGenerate(String input);
}
