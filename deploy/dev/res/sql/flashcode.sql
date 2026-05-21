use flashcode_dev;
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `user_id`        bigint(20) NOT NULL COMMENT '应用所属用户主键id',
    `app_name`       varchar(100) DEFAULT NULL COMMENT '应用名称',
    `app_desc`       text         DEFAULT NULL COMMENT '应用描述',
    `app_type`       tinyint(1)   DEFAULT NULL COMMENT '应用类型',
    `app_doc`        text         DEFAULT NULL COMMENT '应用需求文档',
    `preview_url`    text         DEFAULT NULL COMMENT '应用预览地址',
    `app_screenshot` text         DEFAULT NULL COMMENT '应用截图（OSS URL）',
    PRIMARY KEY (`id`),
    KEY              `idx_apps_user` (`user_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 10000001 CHARACTER SET = utf8mb4 COMMENT = '应用信息表';

DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `app_id`   bigint(20) NOT NULL COMMENT '应用id',
    `msg_role` tinyint(1) NOT NULL COMMENT '消息类型 0：用户，1：大模型',
    `content`  text NOT NULL COMMENT '消息内容',
    PRIMARY KEY (`id`),
    KEY        `idx_chat_history_app` (`app_id`)
)ENGINE = InnoDB AUTO_INCREMENT = 10000001 CHARACTER SET = utf8mb4 COMMENT = '聊天历史记录表';