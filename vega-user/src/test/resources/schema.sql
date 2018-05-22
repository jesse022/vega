
-- 等级表
DROP TABLE IF EXISTS `vega_ranks`;
CREATE TABLE `vega_ranks`(
  `id`                  BIGINT(20)       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pid`                 bigint           DEFAULT null COMMENT '父ID',
  `name`                varchar(128)     NOT NULL     COMMENT '等级名称',
  `integration_start`   bigint          NOT NULL     COMMENT '等级积分起始值',
  `integration_end`     bigint          NOT NULL     COMMENT '等级积分终止值',
  `discount`            int(11)          DEFAULT NULL COMMENT '折扣值,允许为空',
  `extra_json`          varchar(1024)    DEFAULT NULL COMMENT '额外的扩展字段JSON格式',
  `created_at`          datetime         NOT NULL     COMMENT '创建时间',
  `updated_at`          datetime         NOT NULL     COMMENT '最后一次更新时间',
PRIMARY KEY(`id`),
UNIQUE KEY (`name`)
)COMMENT = '等级表，5个等级';


-- 用户等级表
DROP TABLE IF EXISTS `vega_user_ranks`;
CREATE TABLE `vega_user_ranks`(
  `id`                   bigint(20)      NOT NULL AUTO_INCREMENT COMMENT'主键',
  `user_id`              bigint(20)      NOT NULL COMMENT'用户ID',
  `user_name`            varchar(32)     DEFAULT NULL COMMENT'用户名称',
  `integration`          int(11)         DEFAULT 0 COMMENT'用户当前积分',
  `rank_id`              bigint(20)      COMMENT'用户所属等级ID',
  `rank_name`            varchar(32)     DEFAULT NULL COMMENT'用户所属等级名称',
  `extra_json`           varchar(1024)   DEFAULT NULL COMMENT'扩展字段JSON格式',
  `created_at`           datetime        NOT NULL COMMENT'创建时间',
  `updated_at`           datetime        NOT NULL COMMENT'最后一次更新时间',
PRIMARY KEY(`id`),
UNIQUE KEY `user_id_name` (`user_id`,`user_name`)
)COMMENT = '用户等级表';
CREATE INDEX `idx_user_ranks_user_id` ON `vega_user_ranks` (`user_id`);


-- 经销商专属用户信息表
DROP TABLE IF EXISTS `vega_shop_users`;
CREATE TABLE `vega_shop_users`(
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`shop_id` bigint(20) NOT NULL COMMENT '经销商ID',
`shop_name` varchar(32) DEFAULT NULL COMMENT '经销商名称',
`user_id` bigint(20) DEFAULT 0 COMMENT '用户ID',
`mobile` varchar(16) DEFAULT NULL COMMENT '手机号码',
`user_name` varchar(32) DEFAULT NULL COMMENT '用户名称',
`discount` int (11) NOT NULL COMMENT '折扣值 0代表没有折扣',
 `extra_json` varchar(1024) DEFAULT NULL COMMENT '扩展字段JSON格式',
`created_at` datetime NOT NULL COMMENT '创建时间',
 `updated_at` datetime NOT NULL COMMENT '最后一次更新时间',
PRIMARY KEY(`id`)
)COMMENT = '经销商设定用户指定折扣表';
CREATE INDEX `idx_shop_users_shop_id` ON `vega_shop_users` (`shop_id`);
CREATE INDEX `idx_shop_users_user_id` ON `vega_shop_users` (`user_id`);

-- 用户等级履历表
DROP TABLE IF EXISTS `vega_user_rank_resumes`;
CREATE TABLE `vega_user_rank_resumes`(
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT'主键',
`operate_id` bigint(20) NOT NULL COMMENT'操作人ID',
`operate_name` varchar(32) DEFAULT NULL COMMENT'操作人名称',
`user_id` bigint(20) NOT NULL COMMENT'用户ID',
`user_name` varchar(32) DEFAULT NULL COMMENT'用户名称',
`integration` bigint(20) DEFAULT 0 COMMENT'用户当前积分',
`rank_id` bigint(20) COMMENT'用户所属等级ID',
`rank_name` varchar(32) DEFAULT NULL COMMENT'等级名称',
`extra` varchar(1024) DEFAULT NULL COMMENT'扩展字段',
`created_at` datetime NOT NULL COMMENT'创建时间',
`updated_at` datetime NOT NULL COMMENT'最后一次更新时间',
PRIMARY KEY(`id`)
)COMMENT = '用户等级履历表';
CREATE INDEX `idx_user_rank_resumes_user_id` ON `vega_user_rank_resumes` (`user_id`);
CREATE INDEX `idx_user_rank_resumes_operate_id` ON `vega_user_rank_resumes` (`operate_id`);


-- 供货区域
DROP TABLE IF EXISTS `vega_delivery_scopes`;
CREATE TABLE `vega_delivery_scopes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `shop_id` bigint(20) DEFAULT NULL COMMENT '店铺ID',
  `shop_name` varchar(64) DEFAULT NULL COMMENT '店铺名称',
  `pId` bigint(20) DEFAULT NULL COMMENT '上级店铺ID',
  `scope` text COMMENT '供货范围',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)COMMENT='供货区域';
CREATE INDEX `idx_vega_delivery_scopes_shop_id` ON `vega_delivery_scopes` (`shop_id`);

DROP TABLE IF EXISTS `vega_notify_articles`;
CREATE TABLE `vega_notify_articles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `theme` varchar(2048) NOT NULL COMMENT '主题',
  `content` text NOT NULL COMMENT '内容',
  `status` integer DEFAULT NULL COMMENT '状态,1:正常，-1：删除',
  `file_url` varchar(2048) DEFAULT NULL COMMENT '附件地址',
  `notify_supplier` INTEGER DEFAULT 0 COMMENT '是否通知供应商，0：不通知，1：通知',
  `notify_dealer_first` INTEGER DEFAULT 0 COMMENT '是否通知一级经销商，0：不通知，1：通知',
  `notify_dealer_second` INTEGER DEFAULT 0 COMMENT '是否通知二级经销商，0：不通知，1：通知',
  `extra_json` varchar(2048) DEFAULT NULL COMMENT '额外信息',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)COMMENT='公告通知表';
CREATE INDEX vega_notify_article_supplier ON vega_notify_articles (notify_supplier);
CREATE INDEX vega_notify_article_dealer_first ON vega_notify_articles (notify_dealer_first);
CREATE INDEX vega_notify_article_dealer_second ON vega_notify_articles (notify_dealer_second);



DROP TABLE IF EXISTS `vega_nation`;

CREATE TABLE `vega_nation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(40) NOT NULL,
  `province` varchar(40) NOT NULL,
  `city` varchar(40) NOT NULL,
  `district` varchar(40) NOT NULL,
  `parent` varchar(40) NOT NULL,
  `staff_Id` varchar(50) DEFAULT NULL COMMENT '七鱼客服ID',
  `group_Id` varchar(50) DEFAULT NULL COMMENT '七鱼客服组ID',
  PRIMARY KEY (`id`)
)COMMENT='七鱼客服地区关联表';