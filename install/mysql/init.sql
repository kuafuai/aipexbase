DROP DATABASE `aipexbase`;
CREATE DATABASE IF NOT EXISTS aipexbase CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `aipexbase`;

DROP TABLE IF EXISTS `aipexbase`.`api_key`;
CREATE TABLE `aipexbase`.`api_key` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `key_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'apikey',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '描述',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '应用id',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'AVTIVE' COMMENT '状态：AVTIVE\\DISABLE',
  `create_at` varchar(30) DEFAULT NULL COMMENT '创建时间',
  `last_used_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最近一次调用时间',
  `expire_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `keyname` (`key_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DROP TABLE IF EXISTS `aipexbase`.`app_info`;
CREATE TABLE `aipexbase`.`app_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `app_name` varchar(128) NOT NULL,
  `icon_url` text,
  `need_auth` tinyint(1) DEFAULT '1',
  `auth_table` text,
  `enable_permission` tinyint(1) DEFAULT '0',
  `enable_web_console` tinyint(1) DEFAULT '0',
  `status` varchar(32) DEFAULT 'active',
  `description` text,
  `config_json` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `owner` bigint DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_id` (`app_id`),
  KEY `idx_app_info_app_id` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1475 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DROP TABLE IF EXISTS `aipexbase`.`app_requirement_sql`;
CREATE TABLE `aipexbase`.`app_requirement_sql` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `requirement_id` varchar(64) NOT NULL,
  `content` longtext,
  `back_content` text,
  `dsl_content` longtext,
  `version` int DEFAULT '1',
  `status` varchar(32) DEFAULT 'draft',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app_id_and_status` (`app_id`,`status`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1509 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




DROP TABLE IF EXISTS `aipexbase`.`app_sql_execution_log`;
CREATE TABLE `aipexbase`.`app_sql_execution_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `requirement_id` varchar(64) NOT NULL,
  `sql_content` text NOT NULL,
  `executed_by` varchar(64) DEFAULT NULL,
  `executed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `success` tinyint(1) DEFAULT '1',
  `error_message` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



DROP TABLE IF EXISTS `aipexbase`.`app_table_column_info`;
CREATE TABLE `aipexbase`.`app_table_column_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `requirement_id` varchar(64) NOT NULL,
  `table_id` bigint NOT NULL,
  `column_name` varchar(128) NOT NULL,
  `column_type` varchar(64) NOT NULL,
  `dsl_type` varchar(64) DEFAULT NULL,
  `is_primary` tinyint(1) DEFAULT '0',
  `is_nullable` tinyint(1) DEFAULT '1',
  `is_show` tinyint(1) DEFAULT '0',
  `default_value` text,
  `column_comment` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app_id_and_requirement_id` (`app_id`,`requirement_id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=95044 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



DROP TABLE IF EXISTS `aipexbase`.`app_table_info`;
CREATE TABLE `aipexbase`.`app_table_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `requirement_id` varchar(64) NOT NULL,
  `table_name` varchar(128) NOT NULL,
  `physical_table_name` varchar(128) NOT NULL,
  `description` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app_id_and_requirement_id` (`app_id`,`requirement_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15190 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




DROP TABLE IF EXISTS `aipexbase`.`app_table_relation`;
CREATE TABLE `aipexbase`.`app_table_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(64) NOT NULL,
  `requirement_id` varchar(64) NOT NULL,
  `primary_table_id` bigint NOT NULL,
  `primary_table_column_id` bigint NOT NULL,
  `table_id` bigint NOT NULL,
  `table_column_id` bigint NOT NULL,
  `relation_type` varchar(32) DEFAULT 'ONE_TO_MANY',
  `on_delete` varchar(32) DEFAULT 'NO ACTION',
  `on_update` varchar(32) DEFAULT 'NO ACTION',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_api_id` (`app_id`,`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10486 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



DROP TABLE IF EXISTS `aipexbase`.`delay_task_app_info`;
CREATE TABLE `aipexbase`.`delay_task_app_info` (
  `id` bigint NOT NULL,
  `app_id` varchar(64) NOT NULL COMMENT '延迟任务对应的数据表',
  `task_id` int NOT NULL COMMENT '对应库中的延迟任务',
  `task_status` varchar(32) NOT NULL COMMENT '任务状态',
  `service_name` varchar(255) DEFAULT NULL COMMENT '执行的服务名称',
  KEY `delay_task_app_info_service_name_index` (`service_name`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='延迟任务对应的database信息';


DROP TABLE IF EXISTS `aipexbase`.`dynamic_api_setting`;
CREATE TABLE `dynamic_api_setting` (
  `id` int NOT NULL AUTO_INCREMENT,
  `app_id` varchar(255) NOT NULL,
  `key_name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `method` varchar(255) DEFAULT NULL,
  `body_type` varchar(255) DEFAULT NULL,
  `body_template` text,
  `header` text,
  `protocol` varchar(255) DEFAULT NULL,
  `data_path` varchar(255) DEFAULT '',
  `data_type` varchar(255) DEFAULT '',
  `show` tinyint DEFAULT '0',
  `data_raw` text,
  `var_raw` text,
  `market_id` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33745 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;





DROP TABLE IF EXISTS `aipexbase`.`users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `last_login_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `nick_name` varchar(255) DEFAULT NULL,
  `avator` varchar(255) DEFAULT NULL,
  `codeflying_user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_codeflying_user_id` (`codeflying_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `aipexbase`.`api_billing_record`;
CREATE TABLE `api_billing_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` varchar(255) DEFAULT NULL,
  `api_id` int DEFAULT NULL,
  `dynamic_api_id` int DEFAULT NULL,
  `billing_model` int DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `total_amount` decimal(10,2) DEFAULT NULL,
  `called_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_api_id` (`api_id`),
  KEY `idx_dynameic_id` (`dynamic_api_id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DROP TABLE IF EXISTS `aipexbase`.`api_market`;
CREATE TABLE `api_market` (
  `id` int NOT NULL AUTO_INCREMENT,
  `provider_id` int DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `url` varchar(1000) NOT NULL,
  `method` int NOT NULL,
  `protocol` int NOT NULL,
  `auth_type` varchar(255) DEFAULT NULL,
  `auth_config` varchar(2000) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `body_type` int DEFAULT NULL,
  `body_template` varchar(1000) DEFAULT NULL,
  `headers` varchar(1000) DEFAULT NULL,
  `data_path` varchar(255) DEFAULT NULL,
  `data_type` int DEFAULT NULL,
  `data_row` varchar(1000) DEFAULT NULL,
  `var_row` varchar(1000) DEFAULT NULL,
  `status` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `aipexbase`.`api_pricing`;
CREATE TABLE `api_pricing` (
  `id` int NOT NULL AUTO_INCREMENT,
  `market_id` int NOT NULL,
  `pricing_model` int DEFAULT '0',
  `unit_price` decimal(10,2) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DROP TABLE IF EXISTS `aipexbase`.`user_balance`;
CREATE TABLE `user_balance` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `balance` decimal(12,2) DEFAULT '0.00',
  `frozen_balance` decimal(12,2) DEFAULT '0.00',
  `status` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



