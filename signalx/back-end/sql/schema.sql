-- Database Initialization Script for SignalX

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加密后的密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER' COMMENT '角色 (ADMIN/USER)',
  `status` tinyint(2) DEFAULT 1 COMMENT '状态 (1:正常, 0:禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除 (0:正常, 1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ----------------------------
-- Table structure for task
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `keywords` JSON NOT NULL COMMENT '监测关键词列表 (JSON array)',
  `start_time` DATETIME DEFAULT NULL COMMENT '监测起始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '监测终止时间',
  `interval` INT NOT NULL COMMENT '监测频率 (分钟)',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-停用(INACTIVE), 1-启用(ACTIVE)',
  `next_run_time` DATETIME DEFAULT NULL COMMENT '下一次计划生成报告的时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 (0:正常, 1:删除)',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_schedule` (`status`, `next_run_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='舆情监测任务表';

-- ----------------------------
-- Table structure for openclaw_task
-- ----------------------------
DROP TABLE IF EXISTS `openclaw_task`;
CREATE TABLE `openclaw_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(255) NOT NULL COMMENT '任务名称',
    `url` VARCHAR(1024) NOT NULL COMMENT '定制化爬取网址',
    `crawl_logic` TEXT NOT NULL COMMENT '自定义爬取逻辑说明',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `interval` INT NOT NULL DEFAULT 60 COMMENT '执行频率(分钟)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
    `next_run_time` DATETIME DEFAULT NULL COMMENT '下次运行时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_schedule` (`status`, `next_run_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Openclaw 定时任务表';

-- ----------------------------
-- Table structure for report
-- ----------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '关联的监测任务ID',
  `task_type` tinyint NOT NULL DEFAULT 0 COMMENT '任务类型 (0:Task, 1:OpenclawTask)',
  `name` varchar(255) NOT NULL COMMENT '报告名称',
  `summary` longtext COMMENT '报告正文MD',
  `coze_run_id` varchar(64) DEFAULT NULL COMMENT 'Coze工作流运行ID',
  `file_url` varchar(512) DEFAULT NULL COMMENT '报告文件URL (存储在COS)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0:GENERATING, 1:COMPLETED, 2:FAILED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_coze_run_id` (`coze_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析报告表';

-- ----------------------------
-- Table structure for slice
-- ----------------------------
DROP TABLE IF EXISTS `slice`;
CREATE TABLE `slice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '切片ID',
  `task_id` BIGINT NOT NULL COMMENT '关联任务ID',
  `title` VARCHAR(255) NOT NULL COMMENT '切片标题/事件名',
  `heat` DOUBLE DEFAULT 0.0 COMMENT '热度值',
  `sentiment` VARCHAR(32) DEFAULT 'NEUTRAL' COMMENT '情感倾向 (POSITIVE, NEGATIVE, NEUTRAL)',
  `summary` TEXT COMMENT '切片摘要',
  `event_time` DATETIME COMMENT '实际事件发生时间 (业务时间)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间 (系统时间)',
  PRIMARY KEY (`id`),
  INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测分析切片表';

-- ----------------------------
-- Table structure for news
-- ----------------------------
DROP TABLE IF EXISTS `news`;
CREATE TABLE `news` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '关联任务ID',
  `task_type` tinyint(2) NOT NULL DEFAULT 0 COMMENT '任务类型 (0:Task, 1:OpenclawTask)',
  `slice_id` bigint(20) DEFAULT NULL COMMENT '关联事件ID/切片ID (可为空)',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `content` text COMMENT '内容摘要',
  `url` varchar(512) NOT NULL COMMENT '原文链接',
  `publish_time` datetime NOT NULL COMMENT '发布时间',
  `source` varchar(64) NOT NULL COMMENT '来源',
  `sentiment` varchar(32) DEFAULT 'NEUTRAL' COMMENT '情感倾向',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `slice_pos` int DEFAULT NULL COMMENT '切片索引（仅业务临时使用，可为空）',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_slice_id` (`slice_id`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='舆情资讯数据表';

-- ----------------------------
-- Table structure for source_keyword_offset
-- ----------------------------
DROP TABLE IF EXISTS `source_keyword_offset`;
CREATE TABLE `source_keyword_offset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `keyword_id` BIGINT NOT NULL COMMENT '当前系统复用为TaskID',
  `source` VARCHAR(255) NOT NULL COMMENT '渠道(组合关键字)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_keyword_source` (`keyword_id`, `source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道抓取进度偏移表';

-- ----------------------------
-- Table structure for qa_log
-- ----------------------------
DROP TABLE IF EXISTS `qa_log`;
CREATE TABLE `qa_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID (ReportID 或 SliceID)',
  `target_type` tinyint(2) NOT NULL COMMENT '目标类型 (0: REPORT, 1: SLICE)',
  `model` varchar(32) NOT NULL DEFAULT 'deepseek-chat' COMMENT '使用的模型 (deepseek-chat/deepseek-reasoner)',
  `question` text NOT NULL COMMENT '用户提问',
  `reasoning_content` mediumtext COMMENT 'AI思考过程(思维链) - 适配 deepseek-reasoner',
  `answer` mediumtext COMMENT 'AI最终回答',
  `prompt_tokens` int(11) DEFAULT 0 COMMENT '输入Token消耗',
  `completion_tokens` int(11) DEFAULT 0 COMMENT '输出Token消耗',
  `total_tokens` int(11) DEFAULT 0 COMMENT '总Token消耗',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_target` (`user_id`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答记录表';

SET FOREIGN_KEY_CHECKS = 1;
