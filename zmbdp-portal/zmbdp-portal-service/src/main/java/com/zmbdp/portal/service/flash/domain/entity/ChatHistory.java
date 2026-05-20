package com.zmbdp.portal.service.flash.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zmbdp.common.domain.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天记录实体类
 * <p>
 * 存储用户发送的聊天消息和模型生成的回复消息
 *
 * @author 稚名不带撇
 */
@Data
@TableName("chat_history")
@EqualsAndHashCode(callSuper = true)
public class ChatHistory extends BaseDO {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 消息类型 0：用户，1：大模型
     */
    private int msgRole;

    /**
     * 消息内容
     */
    private String content;
}