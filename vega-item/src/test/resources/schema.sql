-- 店铺商品
drop TABLE if EXISTS `vega_shop_items`;
CREATE TABLE if not EXISTS `vega_shop_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `item_name` varchar(128) NOT NULL COMMENT '商品名称',
  `status` tinyint(1) NOT NULL COMMENT '状态',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='店铺商品表';
-- create index idx_shop_item_shop_id on vega_shop_items(shop_id);
-- create index idx_shop_item_item_id on vega_shop_items(item_id);


-- 店铺sku
CREATE TABLE if not EXISTS  `vega_shop_skus` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `category_id` bigint(20) NOT NULL COMMENT '类目id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `sku_id` bigint(20) NOT NULL COMMENT 'sku id',
  `status` tinyint(1) NOT NULL COMMENT 'sku状态',
  `price` int(11) NULL COMMENT '实际售卖价格',
  `extra_price_json` varchar(255) NULL COMMENT 'sku其他各种价格的json表示形式',
  `stock_type` tinyint(4) NOT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储, (冗余自商品表)',
  `stock_quantity` int(11) NULL COMMENT '库存',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='店铺sku表';
-- create index idx_shop_sku_shop_id_sku_id on vega_shop_skus(shop_id,sku_id);
-- create index idx_shop_sku_item_id on vega_shop_skus(item_id);

-- 店铺商品运费
CREATE TABLE if not EXISTS  `vega_shop_item_delivery_fees` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `delivery_fee` int(11) NULL COMMENT '运费, 不指定运费模板时用',
  `delivery_fee_template_id` bigint(20) NULL COMMENT '运费模板id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='店铺商品运费表';
-- create index idx_shop_item_delivery_fee_shop_id_item_id on vega_shop_item_delivery_fees(`shop_id`,`item_id`);
-- create index idx_shop_item_delivery_fee_template_id on vega_shop_item_delivery_fees(`delivery_fee_template_id`);


-- 经销商授权类目
DROP TABLE IF EXISTS `vega_category_authes`;
CREATE TABLE `vega_category_authes`(
`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`shop_id` BIGINT(20)  NOT NULL COMMENT '店铺ID',
`shop_name` varchar(50) NOT NULL COMMENT '店铺名称',
`discount_lower_limit` int(10) DEFAULT NULL COMMENT '倍率下限',
`category_auth_list` text DEFAULT NULL COMMENT '经销商授权类目List',
`category_discount_list` text DEFAULT NULL COMMENT '经销商针对类目设置的折扣表',
`created_at` datetime NOT NULL COMMENT '创建时间',
`updated_at` datetime NOT NULL COMMENT '最后一次更新时间',
PRIMARY KEY(`id`)
)COMMENT='经销商授权类目折扣';
CREATE INDEX `idx_vega_category_authes_shop_id` ON `vega_category_authes`(shop_id);


-- 复用 parana shop start --

 CREATE TABLE `parana_shops` (
   `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
   `outer_id` varchar(32) DEFAULT NULL COMMENT '外部店铺编码',
   `user_id` bigint(20) NOT NULL COMMENT '商家id',
   `user_name` varchar(32) NOT NULL COMMENT '商家名称',
   `name` varchar(64) NOT NULL COMMENT '店铺名称',
   `status` tinyint(1) NOT NULL COMMENT '状态 1:正常, -1:关闭, -2:冻结',
   `type` tinyint(1) NOT NULL COMMENT '店铺状态',
   `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
   `business_id` int(4) DEFAULT NULL COMMENT '行业id',
   `image_url` varchar(128) DEFAULT NULL COMMENT '店铺图片url',
   `address` varchar(128) DEFAULT NULL COMMENT '店铺地址',
   `extra_json` varchar(1024) DEFAULT NULL COMMENT '商品额外信息,建议json字符串',
   `tags_json` varchar(1024) DEFAULT NULL COMMENT '商品标签的json表示形式,只能运营操作, 对商家不可见',
   `created_at` datetime NOT NULL,
   `updated_at` datetime NOT NULL,
   PRIMARY KEY (`id`),
   KEY `idx_shop_user_id` (`user_id`),
   KEY `idx_shop_name` (`name`),
   KEY `idx_shop_outer_id` (`outer_id`)
 ) COMMENT = '店铺表';

-- 复用 parana shop end --


-- 三力士 shop extra 表
DROP TABLE IF EXISTS `vega_shop_extras`;
CREATE TABLE `vega_shop_extras` (
  `id`                   bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id`              bigint(20)         DEFAULT null COMMENT '店铺ID',
  `shop_pid`             bigint(20)         DEFAULT null COMMENT '上级店铺ID',
  `shop_parent_name`     varchar(128)       DEFAULT null COMMENT '上级店铺名称',
  `shop_name`            varchar(128)       DEFAULT null COMMENT '店铺名称',
  `shop_status`          INTEGER            DEFAULT NULL COMMENT '店铺状态',
  `shop_type`            INTEGER            DEFAULT NULL COMMENT '店铺类型',
  `user_id`              bigint(20)         DEFAULT null COMMENT '用户ID',
  `user_name`            varchar(128)       DEFAULT null COMMENT '用户名',
  `purchase_discount`    INTEGER            DEFAULT NULL COMMENT '二级经销商的采购折扣',
  `discount_lower_limit` integer            DEFAULT NULL comment '倍率下限',
  `member_discount_json` VARCHAR(128)       DEFAULT null COMMENT '默认会员折扣, JSON',
  `available_credit`     bigint            DEFAULT NULL COMMENT '可用信用额度',
  `total_credit`         bigint            DEFAULT NULL COMMENT '总信用额度',
  `credit_payment_days`  integer            DEFAULT NULL COMMENT '账龄(还款日期)',
  `is_credit_available`  INTEGER            DEFAULT NULL comment '信用额度是否可用',
  `bank_account`         varchar(64)        DEFAULT NULL comment '银行卡号',
  `contact_name`         varchar(64)        DEFAULT NULL comment '联系人',
  `contact_phone`        varchar(64)        DEFAULT NULL comment '联系电话',
  `province_id`          bigint(20)         DEFAULT null COMMENT '省ID',
  `province`             varchar(64)        DEFAULT NULL comment '省',
  `city_id`              bigint(20)         DEFAULT null COMMENT '市ID',
  `city`                 varchar(64)        DEFAULT NULL comment '市',
  `region_id`            bigint(20)         DEFAULT null COMMENT '区ID',
  `region`               varchar(64)        DEFAULT NULL comment '区',
  `street`               varchar(128)       DEFAULT NULL comment '街道',
  `postcode`             varchar(64)        DEFAULT NULL comment '邮编',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT = '三力士 shop extra 表';
CREATE INDEX `idx_vega_shop_extra_shop_id` ON `vega_shop_extras`(shop_id);
CREATE INDEX `idx_vega_shop_extra_shop_pid` ON `vega_shop_extras`(shop_pid);


-- 经销商信用额度变更履历
DROP TABLE IF EXISTS `vega_credit_alter_resumes`;
create TABLE IF NOT EXISTS `vega_credit_alter_resumes`(
  `id`            bigint   unsigned  not null auto_increment,
  `shop_id`       bigint        DEFAULT null COMMENT '店铺ID',
  `shop_name`     VARCHAR(128)  DEFAULT null COMMENT '店铺名称名称',
  `operate_id`    bigint        DEFAULT null COMMENT '操作人ID',
  `operate_name`  VARCHAR(128)  DEFAULT null COMMENT '操作人名称',
  `trade_no`      varchar(128)  DEFAULT null COMMENT '支付请求流水号',
  `refund_no`     varchar(128)  DEFAULT null COMMENT '退款请求流水号',
  `payment_code`  varchar(128)  DEFAULT null COMMENT '第三方支付流水号',
  `refund_code`   varchar(128)  DEFAULT null COMMENT '第三方退款流水号',
  `should_repayment_date` datetime     DEFAULT null COMMENT '应还款日期',
  `actual_repayment_date` datetime     DEFAULT null COMMENT '实际还款日期',
  `last_credit`   bigint(20)       DEFAULT null COMMENT '上一次信用额度',
  `newest_credit` bigint(20)       DEFAULT null COMMENT '最新信用额度',
  `available_credit`  bigint(20)            DEFAULT NULL COMMENT '可用信用额度',
  `total_credit`  bigint(20)            DEFAULT NULL COMMENT '总信用额度',
  `alter_value`   integer       DEFAULT null COMMENT '变更额度',
  `alter_type`    integer       DEFAULT null COMMENT '变更类型',
  `alter_status`  INTEGER       DEFAULT null comment '还款状态',
  `already_payment` bigint(20)     DEFAULT null comment '已还金额',
  `remain_payment` bigint(20)      DEFAULT null COMMENT '剩余未还款金额',
  `fine_amount`    bigint(20)      DEFAULT NULL comment '罚息',
  `is_payment_complete`  integer DEFAULT null COMMENT '是否还款完成',
  `extra_json`    text          DEFAULT null COMMENT '扩展信息字段',
  `created_at` datetime DEFAULT null COMMENT '创建时间',
  `updated_at` datetime DEFAULT null COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '经销商信用额度变更履历';
CREATE INDEX `idx_vcar_dealer_id` ON `vega_credit_alter_resumes` (`shop_id`);
CREATE INDEX `idx_vcar_operate_id` ON `vega_credit_alter_resumes` (`operate_id`);


-- 还款履历表
DROP TABLE IF EXISTS `vega_credit_repayment_resumes`;
create TABLE IF NOT EXISTS `vega_credit_repayment_resumes` (
  `id`            bigint   unsigned  not null auto_increment,
  `alter_resume_id`    bigint     DEFAULT null COMMENT '消费ID',
  `fee`                bigint    default null COMMENT '本次实际还款(退款)金额',
  `last_debt_amount`   bigint    default null COMMENT '上一次欠款金额',
  `remain_amount` bigint    default null COMMENT '剩余未还款金额',
  `fine_amount`   bigint    default null COMMENT '罚息',
  `beyond_amount` bigint    default null COMMENT '超出还款金额',
  `type`          integer    DEFAULT null COMMENT '类型,(1：还款，2：退款)',
  `status`        integer    DEFAULT null COMMENT '还款状态(0:待运营审核，1:运营审核通过，-1:运营拒绝)运营拒绝后可继续申请运营审核',
  `extra_json`    text       DEFAULT null COMMENT '扩展信息字段',
  `created_at` datetime DEFAULT null COMMENT '创建时间',
  `updated_at` datetime DEFAULT null COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '还款履历表';
CREATE INDEX `idx_vcrep_alter_resume_id` ON `vega_credit_repayment_resumes` (`alter_resume_id`);


--  商品导入处理表
DROP TABLE IF EXISTS `vega_item_imports`;

CREATE TABLE IF NOT EXISTS `vega_item_imports` (
  `id`                    bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`               bigint(20) unsigned NOT NULL COMMENT '供应商店铺 ID',
  `item_uploaded_json`    longtext COMMENT '商品上传数据 {"shopId": 1, "products": [...]}',
  `status`                tinyint(4) NOT NULL COMMENT '执行状态, 0: 未执行, 1: 成功, -1: 失败',
  `error_result`          varchar(64) COMMENT '失败时的错误原因 (错误报告)',
  `created_at`            datetime NOT NULL COMMENT '创建时间',
  `updated_at`            datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_imports_shop_id` (`shop_id`)
) COMMENT='商品导入处理表';

-- parana 商品表
DROP TABLE IF EXISTS `parana_items`;

CREATE TABLE IF NOT EXISTS `parana_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_code` varchar(40) DEFAULT NULL COMMENT '商品编码(可能外部)',
  `category_id` int(11) unsigned NOT NULL COMMENT '后台类目 ID',
  `spu_id` int(11) DEFAULT NULL COMMENT 'SPU编号',
  `shop_id` int(11) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(100) NOT NULL DEFAULT '' COMMENT '店铺名称',
  `brand_id` bigint(20) DEFAULT NULL COMMENT '品牌id',
  `brand_name` varchar(100) DEFAULT '' COMMENT '品牌名称',
  `name` varchar(200) NOT NULL DEFAULT '' COMMENT '商品名称',
  `main_image` varchar(128) DEFAULT NULL COMMENT '主图',
  `low_price` int(11) DEFAULT NULL COMMENT '实际售卖价格(所有sku的最低实际售卖价格)',
  `high_price` int(11) DEFAULT NULL COMMENT '实际售卖价格(所有sku的最高实际售卖价格)',
  `stock_type` tinyint(4) DEFAULT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `sale_quantity` int(11) DEFAULT NULL COMMENT '销量',
  `status` tinyint(1) NOT NULL COMMENT '状态',
  `on_shelf_at` datetime DEFAULT NULL COMMENT '上架时间',
  `advertise` varchar(255) DEFAULT NULL COMMENT '广告语',
  `specification` varchar(128)  DEFAULT NULL COMMENT '规格型号',
  `type` smallint(6) DEFAULT NULL COMMENT '商品类型 1为普通商品, 2为组合商品',
  `reduce_stock_type` smallint(6) DEFAULT '1' COMMENT '减库存方式, 1为拍下减库存, 2为付款减库存',
  `extra_json` varchar(1024) DEFAULT NULL COMMENT '商品额外信息,建议json字符串',
  `tags_json` varchar(1024)  DEFAULT NULL COMMENT '商品标签的json表示形式,只能运营操作, 对商家不可见',
  `item_info_md5` char(32)  DEFAULT NULL COMMENT '商品信息的m5值, 商品快照需要和这个摘要进行对比',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_items_shop_id` (`shop_id`),
  KEY `idx_items_item_code` (`item_code`)
) COMMENT='商品表';



-- parana商品详情
DROP TABLE IF EXISTS `parana_item_details`;

CREATE TABLE IF NOT EXISTS `parana_item_details` (
  `item_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '商品id',
  `images_json` varchar(2048) DEFAULT NULL COMMENT '图片列表, json表示',
  `detail` text COMMENT '富文本详情',
  `packing_json` varchar(1024) DEFAULT NULL COMMENT '包装清单,kv对, json表示',
  `service` varchar(1024) DEFAULT NULL COMMENT '售后服务',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`item_id`)
)COMMENT='商品详情';


-- parana sku
DROP TABLE IF EXISTS `parana_skus`;

CREATE TABLE IF NOT EXISTS  `parana_skus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_code` varchar(40) DEFAULT NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `shop_id` bigint(20) unsigned NOT NULL COMMENT '店铺 ID (冗余自商品表)',
  `status` tinyint(1) NOT NULL COMMENT '商品状态 (冗余自商品表)',
  `specification` varchar(50) DEFAULT NULL COMMENT '型号/款式',
  `model` varchar(50) DEFAULT NULL COMMENT '型号/款式',
  `outer_sku_id` varchar(32) DEFAULT NULL COMMENT '外部sku编号',
  `outer_shop_id` varchar(32) DEFAULT NULL COMMENT '外部店铺id',
  `image` varchar(128) DEFAULT NULL COMMENT '图片url',
  `name` varchar(100) DEFAULT NULL COMMENT '名称',
  `extra_price_json` varchar(255) DEFAULT NULL COMMENT 'sku其他各种价格的json表示形式',
  `price` int(11) DEFAULT NULL COMMENT '实际售卖价格(低)',
  `attrs_json` varchar(1024) DEFAULT NULL COMMENT 'json存储的sku属性键值对',
  `stock_type` tinyint(4) NOT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储, (冗余自商品表)',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `extra` text COMMENT 'sku额外信息',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `thumbnail` varchar(128) DEFAULT NULL COMMENT '样本图 (SKU 缩略图) URL',
  PRIMARY KEY (`id`),
  KEY `idx_skus_item_id` (`item_id`),
  KEY `idx_skus_shop_id` (`shop_id`),
  KEY `idx_skus_sku_code` (`sku_code`),
  KEY `idx_skus_model` (`model`)
) COMMENT='商品SKU表';


-- 积分商品表
DROP TABLE IF EXISTS `vega_integration_items`;
create TABLE IF NOT EXISTS `vega_integration_items` (
  `id`                 bigint   unsigned  not null auto_increment,
  `name`               varchar(128)     DEFAULT null COMMENT '积分商品名称',
  `stock_quantity`     INTEGER    default null COMMENT '库存',
  `status`             INTEGER    default null COMMENT '积分商品状态,1:上架, -1:下架, -3:删除',
  `integration_price`  INTEGER    default null COMMENT '积分售价',
  `images_json`        varchar(512)     default null COMMENT '积分商品图片',
  `extra_json`         text       DEFAULT null COMMENT '扩展信息字段',
  `created_at` datetime DEFAULT null COMMENT '创建时间',
  `updated_at` datetime DEFAULT null COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '积分商品表';