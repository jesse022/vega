alter table parana_shops modify `tags_json` TEXT DEFAULT  NULL COMMENT '商品标签的json表示形式,只能运营操作, 对商家不可见';
alter table parana_shop_orders add column `commented` tinyint DEFAULT 0 COMMENT '是否已评价' after `has_refund`;
alter table parana_delivery_fee_templates add column `low_price` int(11) NULL COMMENT '订单不满该金额时，运费为lowFee' after `fee`,
add column `low_fee` int(11) NULL COMMENT '订单不满low_price时，运费为lowFee' after `low_price`,
add column  `high_price` int(11) NULL COMMENT '订单高于该金额时，运费为highFee' after `low_fee`,
add column  `high_fee` int(11) NULL COMMENT '订单高于high_price时，运费为highFee' after `high_price`,
add column  `middle_fee` int(11) NULL COMMENT '订单价格在lowFee，highFee之间时，运费为middleFee' after `high_fee`;

alter table parana_special_delivery_fees add column `low_price` int(11) NULL COMMENT '订单不满该金额时，运费为lowFee' after `fee`,
add column `low_fee` int(11) NULL COMMENT '订单不满low_price时，运费为lowFee' after `low_price`,
add column  `high_price` int(11) NULL COMMENT '订单高于该金额时，运费为highFee' after `low_fee`,
add column  `high_fee` int(11) NULL COMMENT '订单高于high_price时，运费为highFee' after `high_price`,
add column  `middle_fee` int(11) NULL COMMENT '订单价格在lowFee，highFee之间时，运费为middleFee' after `high_fee`;

ALTER TABLE parana_payments ADD COLUMN `stage` INT NULL COMMENT '分批支付中,表示当前是第几批支付所生成的支付单' AFTER `pay_serial_no`;

ALTER TABLE parana_sku_orders ADD COLUMN `commission_rate` INT default 0 COMMENT '电商平台佣金费率, 万分之一' AFTER `commented`;

ALTER TABLE parana_settle_refund_order_details ADD COLUMN `sum_at` DATETIME  NULL COMMENT '汇总时间' after `check_at`;
ALTER TABLE parana_settle_order_details ADD COLUMN `sum_at` DATETIME  NULL COMMENT '汇总时间' after `check_at`;

ALTER TABLE parana_refunds ADD COLUMN `refund_type`  SMALLINT NOT  NULL default 0 COMMENT '0: 售中退款, 1: 售后退款' after `id`;

ALTER TABLE `vega_ranks` change  `integration_start` `growth_value_start` bigint(11) NOT NULL COMMENT '等级成长值起始值';
ALTER TABLE `vega_ranks`  change `integration_end`    `growth_value_end` bigint(11) NOT NULL COMMENT '等级成长值终止值';
ALTER TABLE `vega_user_rank_resumes`  change `integration` `growth_value` bigint(11) DEFAULT '0' COMMENT '用户成长值' ;

ALTER TABLE parana_order_comments ADD COLUMN `images_json` TEXT NULL COMMENT '图片信息' AFTER `status`;


ALTER TABLE parana_sku_orders ADD COLUMN `has_apply_after_sale` SMALLINT NULL COMMENT '是否申请过售后' AFTER `commented`;

ALTER TABLE vega_purchase_orders add column  `is_temp` tinyint(4) NOT NULL COMMENT '是否为临时采购单' after `sku_quantity`;

ALTER TABLE parana_front_categories ADD COLUMN `outer_id` VARCHAR(256) NULL COMMENT '外部id' AFTER `logo`;

ALTER TABLE vega_shop_skus ADD COLUMN `category_id` bigint(20)  NULL COMMENT '类目id' AFTER `shop_id`;


ALTER TABLE `parana_brands` ADD COLUMN `outer_id` VARCHAR(256) NULL COMMENT '外部 id' AFTER `status`;

update parana_category_attributes set attr_metas_json=replace(attr_metas_json,"}",',"SEARCHABLE":"true"}');

CREATE UNIQUE INDEX `idx_vega_shop_skus_shop_id_sku_id_unique` on `vega_shop_skus` (`shop_id`, `sku_id`);

CREATE UNIQUE INDEX `idx_vega_shop_items_shop_id_item_id_unique` on `vega_shop_items` (`shop_id`, `item_id`);

-- shop_orders表新增type字段
alter table parana_shop_orders add column `type` tinyint NULL COMMENT '订单类型' after `status`;

-- shop_order表和sku_order表增加用于分销的抽佣费率字段distribute_rate
alter table parana_shop_orders add column `distribution_rate` INT default 0 COMMENT '分销抽佣费率, 万分之一' after `commission_rate`;
alter table parana_sku_orders add column `distribution_rate` INT default 0 COMMENT '分销抽佣费率, 万分之一' after `commission_rate`;

-- shop_order新增推荐人字段
alter table parana_shop_orders add column `referer_id`  BIGINT  NULL COMMENT '推荐人id' after `company_id`;
alter table parana_shop_orders add column `referer_name` VARCHAR(64) NULL COMMENT '推荐人名称' after `referer_id`;

ALTER TABLE vega_shop_extras ADD COLUMN `shop_authorize` INT default NULL COMMENT '是否授权 1：是 0：否' AFTER `shop_type`;

ALTER TABLE vega_credit_alter_resumes ADD COLUMN `user_id` bigint(20) default NULL COMMENT '用户Id' AFTER `id`;
ALTER TABLE vega_credit_alter_resumes ADD COLUMN `user_name` varchar(32) default NULL COMMENT '用户名称' AFTER `user_id`;


ALTER TABLE vega_shop_users ADD COLUMN `available_credit` bigint(20) default NULL COMMENT '可用额度' AFTER `discount`;
ALTER TABLE vega_shop_users ADD COLUMN `total_credit` bigint(20) default NULL COMMENT '总额度' AFTER `available_credit`;
ALTER TABLE vega_shop_users ADD COLUMN `credit_payment_days` int(11) default NULL COMMENT '账期' AFTER `total_credit`;
ALTER TABLE vega_shop_users ADD COLUMN `is_credit_available` int(11) default NULL COMMENT '信用额度是否可用' AFTER `credit_payment_days`;

ALTER TABLE vega_user_service_manager ADD COLUMN `shop_id` bigint(20) NOT NULL COMMENT '店铺ID' AFTER `user_name`;

ALTER TABLE vega_user_service_manager ADD COLUMN `shop_name` varchar(64) DEFAULT NULL COMMENT '店铺名称（冗余）' AFTER `shop_id`;

ALTER TABLE vega_user_service_manager ADD COLUMN `type` tinyint(4) NOT NULL COMMENT '类型 0：平台，1：一级服务商，2：二级服务商' AFTER `status`;

ALTER TABLE vega_service_manager_users ADD COLUMN `type` tinyint(4) NOT NULL COMMENT '类型（冗余） 0：平台，1：一级服务商，2：二级服务商' AFTER `service_manager_name`;


-- 店铺类目支持logo
alter table parana_shop_categories add column `logo` varchar(128) DEFAULT NULL COMMENT '店铺类目logo' AFTER `name`;

-- promotion表新增description字段
alter table parana_promotions add column `description` VARCHAR(128) NULL COMMENT '营销活动描述' after `name`;


-- 商品列表添加根据type搜索条件(发布时需要同时发item、web已经admin模块)
-- 前台类目和后台类目添加type字段
alter table parana_back_categories add column `type` tinyint(1) NULL DEFAULT 1 COMMENT '类型' after `level`;
alter table parana_front_categories add column `type` tinyint(1) NULL DEFAULT 1 COMMENT '类型' after `name`;

-- vega_shop_extras表增加is_old_member字段
ALTER TABLE vega_shop_extras ADD COLUMN `is_old_member` INT default NULL COMMENT '是否老会员 1：是 0：否' AFTER `shop_authorize`;

alter table parana_shipments modify column sku_info_jsons varchar(3072);

