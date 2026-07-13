package com.zmbdp.portal.service.flash.controller;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.portal.service.flash.domain.vo.GenerateAppResVO;
import com.zmbdp.portal.service.flash.domain.vo.RequirementsVO;
import com.zmbdp.portal.service.flash.service.IAppGenerationService;
import com.zmbdp.portal.service.flash.service.IRequirementsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 闪码控制器
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/flash")
public class FlashCodeController {

    @Autowired
    private IRequirementsService requirementsService;

    @Autowired
    private IAppGenerationService appGenerationService;

    /**
     * 生成需求文档
     *
     * @param input 输入内容
     * @return 生成好的需求文档
     */
    @PostMapping("/requirements/generate")
    Result<RequirementsVO> requirementsGenerate(@RequestParam String input) {
        if (input == null || input.isEmpty()) {
            return Result.fail("小闪需要您提供一些需求信息，比如：'我想做一个员工考勤打卡的小程序'");
        }
        return Result.success(
                BeanCopyUtil.copyProperties(requirementsService.requirementsGenerate(input), RequirementsVO.class)
        );
    }

    /**
     * 生成应用
     *
     * @param appId  应用 id
     * @param appDoc 需求文档
     * @return 生成的应用信息
     */
    @PostMapping("/apps/generate")
    Result<GenerateAppResVO> appGenerate(@RequestParam Long appId, @RequestParam String appDoc) {
        if (appId == null || appId <= 0) {
            return Result.fail("小闪需要您提供应用 id");
        }
        if (appDoc == null || appDoc.isEmpty()) {
            return Result.fail("小闪需要您提供需求文档");
        }
        return Result.success(
                BeanCopyUtil.copyProperties(appGenerationService.appGenerate(appId, appDoc), GenerateAppResVO.class)
        );
    }
}