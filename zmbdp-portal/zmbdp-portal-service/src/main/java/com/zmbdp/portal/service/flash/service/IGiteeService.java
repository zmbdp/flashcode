package com.zmbdp.portal.service.flash.service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Gitee 服务
 *
 * @author 稚名不带撇
 */
public interface IGiteeService {

    /**
     * 提交代码
     *
     * @param appId   应用 id
     * @param appPath 应用路径
     * @param appType 应用类型
     * @param files   文件
     */
    void commit(Long appId, Path appPath, String appType, Map<String, String> files);

//    /**
//     * 拉取用户代码
//     *
//     * @param appId           应用 id
//     * @param userCodeBaseDir 用户代码根目录
//     */
//    void pullUserAppCode(Long appId, Path userCodeBaseDir);
//
//    /**
//     * 删除应用
//     *
//     * @param appId 应用 id
//     */
//    void delete(Long appId);
}