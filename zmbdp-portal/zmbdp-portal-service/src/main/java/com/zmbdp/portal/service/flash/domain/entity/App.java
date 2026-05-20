package com.zmbdp.portal.service.flash.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zmbdp.common.domain.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用信息实体类
 * <p>
 * 存储用户创建的应用基本信息和部署状态
 *
 * @author 稚名不带撇
 */
@Data
@TableName("app")
@EqualsAndHashCode(callSuper = true)
public class App extends BaseDO {

    /**
     * 所属用户主键ID
     */
    private Long userId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用描述
     */
    private String appDesc;

    /**
     * 应用类型：0 = html，1 = vue3，2 = vue3_spring
     */
    private Integer appType;

    /**
     * 应用需求文档
     */
    private String appDoc;

    /**
     * 应用首页截图
     */
    private String appScreenshot;

    /**
     * 预览的url
     */
    private String previewUrl;

    /**
     * 部署状态： 0 = 未部署  1 = 已部署
     */
    private Integer deployStatus;

    /**
     * 应用部署的url
     */
    private String appUrl;
}