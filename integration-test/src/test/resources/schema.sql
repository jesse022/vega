-- 后台类目表: parana_back_categories
drop table if exists `parana_back_categories`;
CREATE TABLE `parana_back_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `level` tinyint(1) NOT NULL COMMENT '级别',
  `status` tinyint(1) NOT NULL COMMENT '状态,1启用,-1禁用',
  `has_children` tinyint(1) NOT NULL COMMENT '是否有孩子',
  `has_spu` tinyint(1) NOT NULL COMMENT '是否有spu关联',
  `outer_id` VARCHAR(256) NULL COMMENT '外部 id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='后台类目表';
CREATE INDEX idx_back_categories_pid ON parana_back_categories (pid);


-- 品牌表: parana_brands
drop table if exists `parana_brands`;

CREATE TABLE `parana_brands` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '名称',
  `unique_name` varchar(100) NOT NULL COMMENT '名称',
  `en_name` VARCHAR(100) NULL COMMENT '英文名称',
  `en_cap` CHAR(1) NULL COMMENT '首字母',
  `logo` VARCHAR(128) NULL COMMENT '品牌logo',
  `description` varchar(200)  NULL COMMENT '描述',
  `status` tinyint(1)  NULL COMMENT '状态,1启用,-1禁用',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='品牌表';
CREATE INDEX idx_brands_name ON parana_brands (name);
CREATE INDEX idx_brands_en_name ON `parana_brands` (`en_name`);
CREATE INDEX idx_brands_unique_name ON `parana_brands` (`unique_name`);


-- 类目属性表: parana_category_attributes
drop table if exists `parana_category_attributes`;

CREATE TABLE `parana_category_attributes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL COMMENT '类目id',
  `attr_key` VARCHAR(20) NOT NULL COMMENT '属性名',
  `group` VARCHAR(20) NULL COMMENT '所属组名',
  `index` SMALLINT(3) NULL COMMENT '顺序编号',
  `status` tinyint(1) NOT NULL COMMENT '状态,1启用,-1删除',
  `attr_metas_json` varchar(255) NULL COMMENT 'json 格式存储的属性元信息',
  `attr_vals_json` VARCHAR(4096) NULL  COMMENT 'json 格式存储的属性值信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='品牌表';
CREATE INDEX idx_pca_category_id ON parana_category_attributes (category_id);

-- 前台类目表:
drop table if exists `parana_front_categories`;

CREATE TABLE `parana_front_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `level` tinyint(1)  NULL COMMENT '级别',
  `has_children` tinyint(1)  NULL COMMENT '是否有孩子',
  `logo` VARCHAR(256) NULL COMMENT 'logo',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前台类目表';
CREATE INDEX idx_front_categories_pid ON parana_front_categories (pid);

-- 前后台叶子类目映射表: parana_category_bindings
drop table if exists `parana_category_bindings`;

CREATE TABLE `parana_category_bindings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `front_category_id` bigint(20) NOT NULL COMMENT '前台叶子类目id',
  `back_category_id` bigint(20) NOT NULL COMMENT '后台叶子类目id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前后台叶子类目映射表';

-- 店铺内类目表: parana_shop_categories
drop table if exists `parana_shop_categories`;

CREATE TABLE `parana_shop_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `name` VARCHAR(20) NOT NULL COMMENT '类目名称',
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `level` tinyint(1) NOT NULL COMMENT '级别',
  `has_children` tinyint(1)  NULL COMMENT '是否有孩子',
  `has_item` tinyint(1)  NULL COMMENT '是否有商品关联',
  `index` int NULL COMMENT '排序',
  `disclosed` tinyint(1) NULL COMMENT '是否默认展开',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
)COMMENT = '店铺内类目表';
CREATE INDEX idx_shopcats_shop_id ON `parana_shop_categories` (shop_id);

-- 店铺内类目和商品关联表: parana_shop_category_items
drop table if exists `parana_shop_category_items`;

CREATE TABLE `parana_shop_category_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `shop_category_id` bigint(20) NOT NULL COMMENT '店铺内类目id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
)COMMENT = '店铺内类目表';
CREATE INDEX idx_shopcis_shop_id ON `parana_shop_category_items` (shop_id);


-- spu表: parana_spus
drop table if exists `parana_spus`;

CREATE TABLE `parana_spus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `spu_code` VARCHAR(40) NULL COMMENT 'spu编码',
  `category_id` int(11) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `brand_id` bigint(20)  NULL COMMENT '品牌id',
  `brand_name` varchar(100) NULL COMMENT '品牌名称',
  `name` varchar(200) NOT NULL COMMENT 'spu名称',
  `main_image` varchar(128)  NULL COMMENT '主图',
  `low_price` int(11) NULL COMMENT '实际售卖价格(所有sku的最低实际售卖价格)',
  `high_price` int(11) NULL COMMENT '实际售卖价格(所有sku的最高实际售卖价格)',
  `stock_type` TINYINT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储',
  `stock_quantity` int(11)  NULL COMMENT '库存',
  `status` tinyint(1) NOT NULL COMMENT '状态',
  `advertise` varchar(255) COMMENT '广告语',
  `specification` varchar(128) COMMENT '规格型号',
  `type` SMALLINT  NULL COMMENT 'spu类型 1为普通spu, 2为组合spu',
  `reduce_stock_type` SMALLINT DEFAULT 1 COMMENT '减库存方式, 1为拍下减库存, 2为付款减库存',
  `extra_json` VARCHAR(1024) COMMENT 'spu额外信息,建议json字符串',
  `spu_info_md5` CHAR(32) NULL COMMENT 'spu信息的m5值, 交易快照可能需要和这个摘要进行对比',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='SPU表';
CREATE UNIQUE INDEX idx_spus_spu_code ON `parana_spus` (`spu_code`);
CREATE INDEX idx_spus_cid ON `parana_spus` (`category_id`);

-- SPU详情: parana_spu_details
drop table if exists `parana_spu_details`;

CREATE TABLE `parana_spu_details` (
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `images_json` varchar(1024) DEFAULT NULL COMMENT '图片列表, json表示',
  `detail` text  NULL COMMENT '富文本详情',
  `packing_json` varchar(1024) COMMENT '包装清单,kv对, json表示',
  `service` text NULL COMMENT '售后服务',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`spu_id`)
) COMMENT='SPU详情';

-- spu属性: parana_spu_attributes
drop table if exists `parana_spu_attributes`;

CREATE TABLE `parana_spu_attributes` (
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `sku_attributes` varchar(4096)  NULL COMMENT 'spu的sku属性, json存储',
  `other_attributes` varchar(8192)  NULL COMMENT 'spu的其他属性, json存储',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`spu_id`)
) COMMENT='spu属性';



-- SKU模板表: parana_skus
drop table if EXISTS `parana_sku_templates`;

CREATE TABLE `parana_sku_templates` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `spu_id` bigint(20) NOT NULL COMMENT '商品id',
  `specification` VARCHAR(50) NULL COMMENT '型号/款式',
  `status` TINYINT(1) NOT NULL COMMENT 'sku template 状态, 1: 上架, -1:下架,  -3:删除',
  `image` varchar(128)  NULL COMMENT '图片url',
  `thumbnail` VARCHAR(128) NULL COMMENT '样本图 (SKU 缩略图) URL',
  `name` VARCHAR(100) NULL COMMENT '名称',
  `extra_price_json` VARCHAR(255)  NULL COMMENT '其他各种价格的json表示形式',
  `price` int(11) NULL COMMENT '实际售卖价格',
  `attrs_json` varchar(1024)  NULL COMMENT 'json存储的sku属性键值对',
  `stock_type` TINYINT NOT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储, (冗余自SPU表)',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `extra`     TEXT         DEFAULT NULL COMMENT 'sku额外信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='SKU模板表';
CREATE INDEX idx_skutmpls_spu_id ON `parana_sku_templates` (`spu_id`);
CREATE INDEX idx_skutmpls_sku_code ON `parana_sku_templates` (`sku_code`);
-- 商品表: parana_items
drop table if exists `parana_items`;

CREATE TABLE `parana_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_code` VARCHAR(40) NULL COMMENT '外部商品编码',
  `category_id` int(11) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `spu_id` int(11) NULL COMMENT 'SPU编号',
  `shop_id` int(11) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(100) NOT NULL DEFAULT '' COMMENT '店铺名称',
  `brand_id` bigint(20) NULL COMMENT '品牌id',
  `brand_name` varchar(100) NULL COMMENT '品牌名称',
  `name` varchar(200) NOT NULL  COMMENT '商品名称',
  `main_image` varchar(128) DEFAULT NULL COMMENT '主图',
  `low_price` int(11) NULL COMMENT '实际售卖价格(所有sku的最低实际售卖价格)',
  `high_price` int(11) NULL COMMENT '实际售卖价格(所有sku的最高实际售卖价格)',
  `stock_type` TINYINT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储',
  `stock_quantity` int(11)  NULL COMMENT '库存',
  `sale_quantity` int(11)  NULL COMMENT '销量',
  `status` tinyint(1) NOT NULL COMMENT '状态 1: 上架, -1:下架, -2:冻结, -3:删除',
  `on_shelf_at` datetime  NULL COMMENT '上架时间',
  `advertise` varchar(255) COMMENT '广告语',
  `specification` varchar(128) COMMENT '规格型号',
  `type` SMALLINT  NULL COMMENT '商品类型 1为普通商品, 2为组合商品',
  `reduce_stock_type` SMALLINT DEFAULT 1 COMMENT '减库存方式, 1为拍下减库存, 2为付款减库存',
  `extra_json` VARCHAR(1024) NULL COMMENT '商品额外信息,建议json字符串',
  `tags_json` VARCHAR(1024) NULL COMMENT '商品标签的json表示形式,只能运营操作, 对商家不可见',
  `item_info_md5` CHAR(32) NULL COMMENT '商品信息的m5值, 商品快照需要和这个摘要进行对比',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品表';
CREATE INDEX idx_items_item_code ON `parana_items` (`item_code`);
CREATE INDEX idx_items_shop_id ON parana_items (shop_id);
CREATE INDEX idx_items_updated_at ON parana_items (updated_at);



-- 商品SKU表: parana_skus
drop table if EXISTS `parana_skus`;

CREATE TABLE `parana_skus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺 ID (冗余自商品表)',
  `status` TINYINT(1) NOT NULL COMMENT 'sku状态, 1: 上架, -1:下架, -2:冻结, -3:删除',
  `specification` VARCHAR(50) NULL COMMENT '型号/款式',
  `outer_sku_id` VARCHAR(32) NULL COMMENT '外部sku编号',
  `outer_shop_id` VARCHAR(32) NULL COMMENT '外部店铺id',
  `image` varchar(128)  NULL COMMENT '图片url',
  `thumbnail` VARCHAR(128) NULL COMMENT '样本图 (SKU 缩略图) URL',
  `name` VARCHAR(100) NULL COMMENT '名称',
  `extra_price_json` VARCHAR(255)  NULL COMMENT 'sku其他各种价格的json表示形式',
  `price` int(11) NULL COMMENT '实际售卖价格',
  `attrs_json` varchar(1024)  NULL COMMENT 'json存储的sku属性键值对',
  `stock_type` TINYINT NOT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储, (冗余自商品表)',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `extra`     TEXT         DEFAULT NULL COMMENT 'sku额外信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品SKU表';
CREATE INDEX idx_skus_item_id ON `parana_skus` (`item_id`);
CREATE INDEX idx_skus_shop_id ON `parana_skus` (`shop_id`);
CREATE INDEX idx_skus_sku_code ON `parana_skus` (`sku_code`);

-- 商品详情: parana_item_details
drop table if exists `parana_item_details`;

CREATE TABLE `parana_item_details` (
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `images_json` varchar(1024)  NULL COMMENT '图片列表, json表示',
  `detail` VARCHAR(2048)  NULL COMMENT '富文本详情',
  `packing_json` varchar(1024) COMMENT '包装清单,kv对, json表示',
  `service` VARCHAR(1024) NULL COMMENT '售后服务',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`item_id`)
) COMMENT='商品详情';

-- 商品属性: parana_item_attributes
drop table if exists `parana_item_attributes`;

CREATE TABLE `parana_item_attributes` (
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `sku_attributes` varchar(4096)  NULL COMMENT '商品的sku属性, json存储',
  `other_attributes` varchar(8192)  NULL COMMENT '商品的其他属性, json存储',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`item_id`)
) COMMENT='商品属性';

-- 商品快照表: parana_item_snapshots
drop table if exists `parana_item_snapshots`;

CREATE TABLE `parana_item_snapshots` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20)  NULL COMMENT '商品id',
  `item_code` VARCHAR(40) NULL COMMENT '外部商品编码',
  `item_info_md5` CHAR(32) NULL COMMENT '商品信息的m5值, 商品快照需要和这个摘要进行对比',
  `shop_id` int(11) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(100) NOT NULL DEFAULT '' COMMENT '店铺名称',
  `name` varchar(200) NOT NULL  COMMENT '商品名称',
  `main_image` varchar(128) DEFAULT NULL COMMENT '主图',
  `images_json` varchar(1024)  NULL COMMENT '图片列表, json表示',
  `advertise` varchar(255) COMMENT '广告语',
  `specification` varchar(128) COMMENT '规格型号',
  `extra_json` VARCHAR(1024) NULL COMMENT '商品额外信息,建议json字符串',
  `tags_json` VARCHAR(1024) NULL COMMENT '商品标签的json表示形式,只能运营操作, 对商家不可见',
  `sku_attributes` varchar(4096)  NULL COMMENT '商品的sku属性, json存储',
  `other_attributes` varchar(8192)  NULL COMMENT '商品的其他属性, json存储',
  `detail` VARCHAR(2048)  NULL COMMENT '富文本详情',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品快照表';
create index item_id_md5 on parana_item_snapshots(item_id,item_info_md5);

-- 店铺表: parana_shops
drop table if exists `parana_shops`;

CREATE TABLE `parana_shops` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `outer_id` VARCHAR(32)  NULL COMMENT '外部店铺编码',
  `user_id` BIGINT(20) NOT NULL COMMENT '商家id',
  `user_name` VARCHAR(32) NOT NULL COMMENT '商家名称',
  `name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `status`  TINYINT(1) NOT NULL COMMENT '状态 1:正常, -1:关闭, -2:冻结',
  `type` TINYINT(1) NOT NULL  COMMENT '店铺状态',
  `phone` varchar(32) NULL COMMENT '联系电话',
  `business_id` int(4)  NULL COMMENT '行业id',
  `image_url` varchar(128) NULL COMMENT '店铺图片url',
  `address` varchar(128) NULL COMMENT '店铺地址',
  `extra_json` VARCHAR(1024) NULL COMMENT '商品额外信息,建议json字符串',
  `tags_json` VARCHAR(1024) NULL COMMENT '商品标签的json表示形式,只能运营操作, 对商家不可见',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='店铺表';
create index idx_shop_user_id on parana_shops(user_id);
create index idx_shop_name on parana_shops(`name`);
create index idx_shop_outer_id on parana_shops(`outer_id`);

-- 店铺附加表: parana_shop_extra
drop table if exists `parana_shop_extras`;

CREATE TABLE `parana_shop_extras` (
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `deposit_cost` int(11) NULL COMMENT '保证金金额',
  `rate` int(4) NULL COMMENT '商家费率',
  `account` VARCHAR(64) NULL COMMENT '店铺名称',
  `account_type`  SMALLINT(1) NULL COMMENT '1:支付宝 2:银行卡',
  `account_name` VARCHAR(64) NULL  COMMENT '开户人姓名',
  `bank_name` varchar(32) NULL COMMENT '银行名称',
  `pre_im_id` VARCHAR(32)  NULL COMMENT '售前客服联系方式id',
  `post_im_id` varchar(32) NULL COMMENT '售后客服联系方式id',
  `commission_type` SMALLINT(4) NULL COMMENT '抽佣类型 1 费率 2 差价',
  `billing_period` SMALLINT(4) NULL COMMENT '帐期,  1、5、10、15、30五类',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`shop_id`)
) COMMENT='店铺附加表';

-- 运费模板
drop table if exists `parana_delivery_fee_templates`;

CREATE TABLE `parana_delivery_fee_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `name` varchar(64) NOT NULL COMMENT '模板名称',
  `is_free` tinyint(1) NOT NULL COMMENT '是否包邮',
  `deliver_method` tinyint NOT NULL COMMENT '运送方式:1-快递,2-EMS,3-平邮',
  `charge_method` tinyint NOT NULL COMMENT '计价方式:1-按计量单位,2-固定运费',
  `fee` int(11) NULL COMMENT '运费,当计价方式为固定运费时使用',
  `init_amount` int(11) NULL COMMENT '首费数量',
  `init_fee` int(11) NULL COMMENT '首费金额',
  `incr_amount` int(11) NULL COMMENT '增费数量',
  `incr_fee` int(11) NULL COMMENT '曾费金额',
  `is_default` tinyint(1) NULL COMMENT '是否是默认模板',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='运费模板';
create index idx_delivery_fee_template_shop_id on parana_delivery_fee_templates(`shop_id`);

-- 特殊区域运费
drop table if exists `parana_special_delivery_fees`;

CREATE TABLE `parana_special_delivery_fees` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `delivery_fee_template_id` bigint(20) NOT NULL COMMENT '所属运费模板id',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `address_json` varchar(1024) NOT NULL COMMENT '区域id列表,json存储',
  `address_tree_json` text NOT NULL COMMENT '层级结构的区域json,主要用于前端展示',
  `desc` varchar(64) NULL COMMENT '描述',
  `is_free` tinyint(1) NOT NULL COMMENT '是否包邮',
  `deliver_method` tinyint NOT NULL COMMENT '运送方式:1-快递,2-EMS,3-平邮',
  `charge_method` tinyint NOT NULL COMMENT '计价方式:1-按计量单位,2-固定运费',
  `fee` int(11) NULL COMMENT '运费,当计价方式为固定运费时使用',
  `init_amount` int(11) NULL COMMENT '首费数量',
  `init_fee` int(11) NULL COMMENT '首费金额',
  `incr_amount` int(11) NULL COMMENT '增费数量',
  `incr_fee` int(11) NULL COMMENT '增费金额',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='特殊区域运费';
create index idx_specail_delivery_fees_dfti on parana_special_delivery_fees(`delivery_fee_template_id`);

-- 商品运费
drop table if exists `parana_item_delivery_fees`;
CREATE TABLE `parana_item_delivery_fees` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `delivery_fee` int(11) NULL COMMENT '运费, 不指定运费模板时用',
  `delivery_fee_template_id` bigint(20) NULL COMMENT '运费模板id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='商品运费表';
create index idx_item_delivery_fee_item_id on parana_item_delivery_fees(`item_id`);
create index idx_item_delivery_fee_template_id on parana_item_delivery_fees(`delivery_fee_template_id`);


-- 购物车商品
DROP TABLE IF EXISTS `parana_cart_items`;

CREATE TABLE IF NOT EXISTS `parana_cart_items` (
  `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `buyer_id`       BIGINT(20) UNSIGNED DEFAULT NULL COMMENT '买家ID',
  `shop_id`        BIGINT(20) UNSIGNED NOT NULL COMMENT '店铺ID',
  `sku_id`         BIGINT(20) UNSIGNED NOT NULL COMMENT 'SKU ID',
  `quantity`       INT(10) UNSIGNED    NOT NULL COMMENT '商品数量',
  `snapshot_price` INT                 NULL     COMMENT '快照价格',
  `extra_json`     varchar(1024) DEFAULT NULL COMMENT 'json储存的其他属性键值对',
  `created_at`     DATETIME DEFAULT NULL  COMMENT '创建时间',
  `updated_at`     DATETIME DEFAULT NULL  COMMENT '更新时间',
  PRIMARY KEY (`id`)
);
CREATE INDEX idx_cart_item_buyer_id ON `parana_cart_items` (`buyer_id`);


-- shopOrder 表: parana_shop_orders

DROP TABLE IF EXISTS `parana_shop_orders`;

CREATE TABLE `parana_shop_orders` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id`  BIGINT(20) NOT NULL  COMMENT '店铺id',
  `buyer_id`  BIGINT NOT NULL COMMENT '买家id',
  `fee`  BIGINT(20) NOT NULL COMMENT '实付金额',
  `status` SMALLINT NOT NULL COMMENT '状态',
  `buyer_name` VARCHAR(64) NOT NULL COMMENT '买家名称',
  `out_buyer_id`  VARCHAR(64)  NULL COMMENT '买家外部id',
  `shop_name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `out_shop_id`  VARCHAR(64)  NULL COMMENT '店铺外部id',
  `company_id`  BIGINT  NULL COMMENT '公司id',
  `origin_fee`  BIGINT(20)  NULL COMMENT '原价',
  `discount`  BIGINT(20)  NULL COMMENT '优惠金额',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `origin_ship_fee`  BIGINT(20)  NULL COMMENT '运费原始金额',
  `shipment_promotion_id` BIGINT(20) NULL COMMENT '运费营销活动id',
  `integral`  INT NULL COMMENT '积分减免金额',
  `balance` INT NULL COMMENT '余额减免金额',
  `promotion_id` BIGINT(20)  NULL COMMENT '店铺级别的优惠id',
  `shipment_type`  SMALLINT NULL COMMENT '配送方式',
  `pay_type`  SMALLINT NOT NULL COMMENT '支付类型, 1-在线支付 2-货到付款',
  `channel` SMALLINT NOT NULL COMMENT '订单渠道 1-手机 2-pc',
  `has_refund` tinyint NULL COMMENT '是否申请过逆向流程',
  `buyer_note`  VARCHAR(512)  NULL COMMENT '买家备注',
  `extra_json` VARCHAR(2048)  NULL COMMENT '子订单额外信息,json表示',
  `tags_json` VARCHAR(2048) NULL COMMENT '子订单tag信息, json表示',
  `out_id` VARCHAR(64) NULL COMMENT '外部订单id',
  `out_from` VARCHAR(64) NULL COMMENT '外部订单来源',
  `commission_rate` INT default 0 COMMENT '电商平台佣金费率, 万分之一',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='店铺维度订单';
create index idx_shop_orders_shop_id on parana_shop_orders(shop_id);
create index idx_shop_orders_buyer_id on parana_shop_orders(buyer_id);

-- itemOrder 表: parana_sku_orders

DROP TABLE IF EXISTS `parana_sku_orders`;

CREATE TABLE `parana_sku_orders` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_id`  BIGINT(20)  NOT NULL COMMENT 'sku id',
  `quantity`  BIGINT(20) NOT NULL COMMENT 'sku数量',
  `fee`  BIGINT(20) NOT NULL COMMENT '实付金额',
  `status` SMALLINT NOT NULL COMMENT '子订单状态',
  `order_id`  BIGINT(20)  NOT NULL COMMENT '订单id',
  `buyer_id`  BIGINT(20)  NOT NULL COMMENT '买家id',
  `out_id`  VARCHAR(64)  NULL COMMENT '外部自订单id',
  `buyer_name`  VARCHAR(32)  NULL COMMENT '买家姓名',
  `out_buyer_id`  VARCHAR(512)  NULL COMMENT '买家外部id',
  `item_id`  BIGINT(20) NOT NULL COMMENT '商品id' ,
  `item_name`  VARCHAR(512) NOT  NULL COMMENT '商品名称',
  `sku_image`  VARCHAR(512)  NULL COMMENT 'sku主图',
  `shop_id`  BIGINT(20) NOT NULL COMMENT '店铺id',
  `shop_name`  VARCHAR(512) NOT  NULL COMMENT '店铺名称',
  `out_shop_id`  VARCHAR(512)  NULL COMMENT '店铺外部id',
  `company_id`  BIGINT(20)  NULL COMMENT '公司id',
  `out_sku_id`  VARCHAR(64)  NULL COMMENT 'sku外部id',
  `sku_attributes`  VARCHAR(512)  NULL COMMENT 'sku属性, json表示',
  `channel`  SMALLINT  NULL COMMENT '订单渠道 1-pc, 2-手机',
  `pay_type`  SMALLINT  NULL COMMENT '支付类型 1-在线支付 2-货到付款',
  `shipment_type`  SMALLINT NULL COMMENT '配送方式',
  `origin_fee`  BIGINT(20)  NULL COMMENT '原价',
  `discount`  BIGINT(20)  NULL COMMENT '折扣',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费折扣',
  `integral`  INT NULL COMMENT '积分减免金额',
  `balance` INT NULL COMMENT '余额减免金额',
  `promotion_id` BIGINT(20)  NULL COMMENT '单品级别的优惠id',
  `item_snapshot_id`  BIGINT  NULL COMMENT '商品快照id',
  `has_refund`  TINYINT  NULL COMMENT '是否申请过退款',
  `invoiced`  TINYINT  NULL COMMENT '是否已开具过发票',
  `commented` SMALLINT NULL COMMENT '是否已评价',
  `extra_json` VARCHAR(2048)  NULL COMMENT '子订单额外信息,json表示',
  `tags_json` VARCHAR(2048) NULL COMMENT '子订单tag信息, json表示',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='sku维度订单';

CREATE INDEX idx_sku_orders_buyer_id ON `parana_sku_orders` (`buyer_id`);
CREATE INDEX idx_sku_orders_shop_id ON `parana_sku_orders` (`shop_id`);
CREATE INDEX idx_sku_orders_order_id ON `parana_sku_orders` (`order_id`);



-- 支付单表: parana_payments

DROP TABLE IF EXISTS `parana_payments`;

CREATE TABLE `parana_payments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `fee`  BIGINT(20) NOT NULL COMMENT '实付金额',
  `out_id` VARCHAR(128) NULL COMMENT '外部id',
  `pay_info_md5` CHAR(32) NULL COMMENT '支付信息md5',
  `origin_fee`  BIGINT(20)  NULL COMMENT '原始金额',
  `discount`  BIGINT(20)  NULL COMMENT '优惠金额',
  `integral`  INT NULL COMMENT '积分减免金额',
  `balance` INT NULL COMMENT '余额减免金额',
  `status`  SMALLINT NOT NULL COMMENT '状态, 0:待支付, 1:已支付, -1:删除',
  `pay_serial_no`  VARCHAR(32)  NULL COMMENT '外部支付流水号',
  `pay_account_no` VARCHAR(32)  NULL COMMENT '支付账号',
  `channel`  VARCHAR(16)  NULL COMMENT '支付渠道',
  `promotion_id` BIGINT(20) NULL COMMENT '平台级别优惠活动id',
  `extra_json`  VARCHAR(512)  NULL COMMENT '支付额外信息',
  `tags_json`  VARCHAR(512)  NULL COMMENT '支付额外信息, 运营使用',
  `paid_at` DATETIME NULL COMMENT '订单支付时间',
  `created_at`  DATETIME  NULL COMMENT '支付单创建时间',
  `updated_at`  DATETIME  NULL COMMENT  '支付单更新时间',
  PRIMARY KEY (`id`)
) COMMENT='支付单表';
create index idx_payments_pay_serial_no on parana_payments(pay_serial_no);
create UNIQUE index idx_payments_pay_info_md5 on parana_payments(pay_info_md5);


-- 支付单和(子)订单关联表
DROP TABLE IF EXISTS `parana_order_payments`;

CREATE TABLE `parana_order_payments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `payment_id`  BIGINT(20) NOT NULL COMMENT '支付单id',
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type` SMALLINT NOT NULL COMMENT '订单类型 1: 店铺订单支付, 2: 子订单支付',
  `status` SMALLINT  NOT  NULL COMMENT '0: 待支付, 1: 已支付, -1:已删除',
  `created_at`  DATETIME   NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='支付单和(子)订单关联表';
create index idx_order_payment_payment_id on parana_order_payments(payment_id);
create index idx_order_payment_order_id on parana_order_payments(order_id);

-- 发货单表: parana_shipments

DROP TABLE IF EXISTS `parana_shipments`;

CREATE TABLE `parana_shipments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `status`  SMALLINT NOT NULL COMMENT '0: 待发货, 1:已发货, 2: 已收货, -1: 已删除',
  `shipment_serial_no`  VARCHAR(32)  NULL COMMENT '物流单号',
  `shipment_corp_code`  VARCHAR(32)  NULL COMMENT '物流公司编号',
  `shipment_corp_name`  VARCHAR(32)  NULL COMMENT '物流公司名称',
  `sku_info_jsons`  VARCHAR(1024)  NULL COMMENT '对应的skuId及数量, json表示',
  `receiver_infos`  VARCHAR(512)  NULL COMMENT '收货人信息',
  `extra_json`  VARCHAR(512)  NULL COMMENT '发货单额外信息',
  `tags_json`  VARCHAR(512)  NULL COMMENT '发货单额外信息, 运营使用',
  `confirm_at`  DATETIME  NULL COMMENT '确认收货时间',
  `created_at`  DATETIME  NULL COMMENT '发货单创建时间',
  `updated_at`  DATETIME  NULL COMMENT '发货单更新时间',
  PRIMARY KEY (`id`)
) COMMENT='发货单表';
create index idx_shipments_ship_serial_no on parana_shipments(shipment_serial_no);


-- 发货单和(子)订单关联表
DROP TABLE IF EXISTS `parana_order_shipments`;

CREATE TABLE `parana_order_shipments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_id`  BIGINT(20) NOT NULL COMMENT '发货单id',
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type`  SMALLINT NOT NULL COMMENT '发货订单类型 1: 店铺订单发货, 2: 子订单发货',
  `status`  SMALLINT NOT NULL COMMENT '0: 待发货, 1: 已发货, -1:已删除',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='发货单和(子)订单关联表';
create index idx_order_shipment_shipment_id on parana_order_shipments(shipment_id);
create index idx_order_shipment_order_id on parana_order_shipments(order_id);


-- 用户收货信息表: parana_receiver_infos
DROP TABLE IF EXISTS `parana_receiver_infos`;

CREATE TABLE `parana_receiver_infos` (
  `id`                BIGINT          NOT NULL    AUTO_INCREMENT,
  `user_id`           BIGINT          NOT NULL    COMMENT '用户ID',
  `receive_user_name` VARCHAR(50)     NOT NULL    COMMENT '收货人姓名',
  `phone`             VARCHAR(32)     NULL        COMMENT '固定电话',
  `mobile`            VARCHAR(32)     NOT NULL    DEFAULT ''  COMMENT '手机号',
  `email`             VARCHAR(32)     NULL        COMMENT '邮箱地址',
  `is_default`        TINYINT         NOT NULL    DEFAULT '0' COMMENT '是否默认 1：默认',
  `status`            TINYINT         NOT NULL    COMMENT '状态，1：正常，-1：删除',
  `province`          VARCHAR(50)     NOT NULL    COMMENT '省',
  `province_id`       BIGINT          NOT NULL    COMMENT '省ID',
  `city`              VARCHAR(50)     NOT NULL    COMMENT '市',
  `city_id`           BIGINT          NOT NULL    COMMENT '市ID',
  `region`            VARCHAR(50)     NOT NULL    COMMENT '区',
  `region_id`         BIGINT          NOT NULL    COMMENT '区ID',
  `street`            VARCHAR(50)     NULL        COMMENT '街道，可以为空',
  `street_id`         BIGINT          NULL        COMMENT '街道ID，可以为空',
  `detail`            VARCHAR(256)    NOT NULL    COMMENT '详细地址',
  `postcode`          VARCHAR(32)     NULL        COMMENT '邮政编码',
  `created_at`        DATETIME        NOT NULL    COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL    COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='用户收货信息表';

-- 订单收货信息表

DROP TABLE IF EXISTS `parana_order_receiver_infos`;

CREATE TABLE `parana_order_receiver_infos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type`  SMALLINT NOT  NULL COMMENT '1: 店铺订单, 2: 子订单',
  `receiver_info_json`  VARCHAR(512) NOT NULL COMMENT 'json表示的收货信息',
  `created_at`  DATETIME  NULL  COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
);
create index idx_order_receiver_order_id on parana_order_receiver_infos(order_id);

-- 用户发票表
DROP TABLE IF EXISTS `parana_invoices`;

CREATE TABLE `parana_invoices` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id`  BIGINT(20) NOT NULL COMMENT '用户id',
  `title`  VARCHAR(128) NOT NULL COMMENT '发票title',
  `detail_json`  VARCHAR(512)  NULL COMMENT '发票详细信息',
  `status`  BIGINT(20) NOT NULL COMMENT '发票状态',
  `is_default`  tinyint NOT NULL COMMENT '是否默认',
  `created_at`  DATETIME  NULL  COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='用户发票表';
create index idx_invoice_user_id on parana_invoices(user_id);

--  订单发票关联表
DROP TABLE IF EXISTS `parana_order_invoices`;

CREATE TABLE `parana_order_invoices` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id`  BIGINT(20) NOT NULL COMMENT '发票id',
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type`  SMALLINT NOT  NULL COMMENT '1: 店铺订单, 2: 子订单',
  `status`  BIGINT(20) NOT NULL COMMENT '0: 待开, 1: 已开, -1: 删除作废',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='订单发票关联表';
create index idx_oi_order_id on parana_order_invoices(order_id);

-- 退款单表
DROP TABLE IF EXISTS `parana_refunds`;
CREATE TABLE `parana_refunds` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `fee`  BIGINT(20)  NULL COMMENT '实际退款金额',
  `shop_id` BIGINT(20) NOT NULL COMMENT '店铺id',
  `shop_name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `buyer_id` BIGINT(20) NOT NULL COMMENT '买家id',
  `buyer_name` VARCHAR(64) NOT NULL COMMENT '买家名称',
  `out_id`  VARCHAR(512)  NULL COMMENT '外部业务id',
  `integral`  BIGINT(20)  NULL COMMENT '要退的积分',
  `balance`  BIGINT(20)  NULL COMMENT '要退的余额',
  `status`  BIGINT(20)  NULL COMMENT '状态',
  `refund_serial_no`  VARCHAR(512)  NULL COMMENT '退款流水号',
  `payment_id` BIGINT(20) NULL COMMENT '对应的支付单id',
  `trade_no`  VARCHAR(512)  NULL COMMENT '对应支付单的电商平台交易流水号',
  `pay_serial_no`  VARCHAR(512)  NULL COMMENT '对应支付单的交易流水号',
  `refund_account_no`  VARCHAR(512)  NULL COMMENT '退款到哪个账号',
  `channel`  VARCHAR(512)  NULL COMMENT '退款渠道',
  `promotion_id`  BIGINT(20)  NULL COMMENT '涉及到的平台级优惠id',
  `buyer_note`  VARCHAR(512)  NULL COMMENT '买家备注',
  `seller_note`  VARCHAR(512)  NULL COMMENT '商家备注',
  `extra_json`  VARCHAR(512)  NULL COMMENT '附加信息, 商家使用',
  `tags_json`  VARCHAR(512)  NULL COMMENT '标签信息, 运营使用',
  `refund_at`  DATETIME  NULL COMMENT '退款成功时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='退款单表';
create index idx_refunds_shop_id on parana_refunds(shop_id);
create index idx_refunds_refund_serial_no on parana_refunds(refund_serial_no);

-- 退款单和订单关联表
DROP TABLE IF EXISTS `parana_order_refunds`;

CREATE TABLE `parana_order_refunds` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `refund_id`  BIGINT(20)  NOT NULL COMMENT '退款单id',
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type`  SMALLINT NOT  NULL COMMENT '1: 店铺订单, 2: 子订单',
  `status`  BIGINT(20)   NULL COMMENT '状态 0:待退款, 1:已退款, -1:删除',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='退款单和订单关联表';
create index idx_order_refund_rid on parana_order_refunds(refund_id);
create index idx_order_refund_oid on parana_order_refunds(order_id);

-- 物流公司
DROP TABLE IF EXISTS `parana_express_companies`;
CREATE TABLE `parana_express_companies` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `code` varchar(32) NOT NULL COMMENT '物流公司代号',
  `name` varchar(64) NOT NULL COMMENT '物流公司名',
  `status` tinyint(6) NOT NULL COMMENT '状态,-1:停用,1:启用',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8;
create UNIQUE index idx_pec_code on parana_express_companies(`code`);

-- 营销活动定义表
DROP TABLE IF EXISTS `parana_promotion_defs`;
CREATE TABLE `parana_promotion_defs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name`  VARCHAR(64) NOT NULL COMMENT '营销工具唯一标识',
  `type`  SMALLINT NOT  NULL COMMENT '营销工具类型',
  `status`  SMALLINT NOT  NULL COMMENT '状态, , 1: 启用 -1:禁用',
  `user_scope_key`  VARCHAR(512)  NULL COMMENT '用户选择',
  `sku_scope_key`  VARCHAR(512)  NULL COMMENT 'sku选择',
  `behavior_key`  VARCHAR(512)  NULL COMMENT '营销方式',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
);


-- 营销活动表
DROP TABLE IF EXISTS `parana_promotions`;
CREATE TABLE `parana_promotions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id`  BIGINT(20)  NOT NULL COMMENT '店铺id, 如果是平台级别的营销, 则为0',
  `name`  VARCHAR(32) NOT  NULL COMMENT '营销活动名称' ,
  `promotion_def_id`  BIGINT(20) NOT  NULL COMMENT '营销工具定义id',
  `type`  SMALLINT NOT  NULL COMMENT '营销工具类型,冗余',
  `status`  SMALLINT NOT  NULL COMMENT '状态, 0: 初始化, 1: 可能生效, 需要根据生效开始和截至时间进一步判断 -1:已过期',
  `start_at`  DATETIME NOT  NULL COMMENT '营销活动开始时间',
  `end_at`  DATETIME  NOT  NULL COMMENT '营销活动结束时间',
  `user_scope_params_json`  VARCHAR(512)  NULL COMMENT '营销选择用户范围参数',
  `sku_scope_params_json`  VARCHAR(512)  NULL COMMENT '营销选择sku范围参数',
  `condition_params_json`  VARCHAR(512)  NULL COMMENT '营销执行前提条件的参数, json表示',
  `behavior_params_json`  VARCHAR(512)  NULL COMMENT '营销方式参数',
  `extra_json`  VARCHAR(512)  NULL COMMENT '附加信息',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
);
create index idx_promotion_shop_id on parana_promotions(shop_id);

-- 用户营销表
DROP TABLE IF EXISTS `parana_user_promotions`;

CREATE TABLE `parana_user_promotions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id`  BIGINT(20) NOT NULL COMMENT '用户id',
  `shop_id`  BIGINT(20) NOT NULL COMMENT '店铺id',
  `promotion_id`  BIGINT(20) NOT NULL COMMENT '营销活动id',
  `type`  SMALLINT NOT  NULL COMMENT '营销工具类型,冗余',
  `name`  VARCHAR(32) NOT  NULL COMMENT '营销活动名称',
  `available_quantity`  BIGINT(20)  NULL COMMENT '可用数量(或金额)',
  `frozen_quantity`  BIGINT(20)  NULL COMMENT '冻结数量(或金额)',
  `status`  SMALLINT NOT NULL COMMENT '状态, 0: 初始化, 1: 可能生效, 需要根据生效开始和截至时间进一步判断 -1:已过期',
  `extra_json`  VARCHAR(512)  NULL COMMENT '附加信息',
  `start_at`  DATETIME NOT  NULL COMMENT '营销活动开始时间',
  `end_at`  DATETIME  NOT  NULL COMMENT '营销活动结束时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
);
create index idx_up_user_id on parana_user_promotions(user_id);

DROP TABLE IF EXISTS `parana_promotion_tracks`;
CREATE TABLE `parana_promotion_tracks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `promotion_id` bigint(20) NOT NULL COMMENT '营销id',
  `received_quantity` int(11) NOT NULL COMMENT '已领取数量',
  `used_quantity` int(11) NOT NULL COMMENT '已使用数量',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='营销跟踪表';
create index idx_pt_promotion_id on parana_promotion_tracks(promotion_id);


-- 评价表
DROP TABLE IF EXISTS `parana_order_comments`;

CREATE TABLE `parana_order_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) NOT NULL COMMENT '父评论id,以后评论叠加会用到,现在默认都是-1',
  `belong_user_type` SMALLINT NOT NULL COMMENT '1为买家发起评论,2为商家发起评论',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `user_name` VARCHAR(64) NOT NULL COMMENT '用户名称',
  `sku_order_id` bigint(20) NOT NULL COMMENT 'sku订单id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `item_name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `sku_attributes`  VARCHAR(512)  NULL COMMENT 'sku属性, json表示',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` VARCHAR(128) NOT NULL COMMENT '店铺名称',
  `quality` INT NULL COMMENT '质量评分',
  `describe` INT NULL COMMENT '描述评分',
  `service` INT NULL COMMENT '服务评分',
  `express` INT NULL COMMENT '物流评分',
  `context` TEXT NULL COMMENT '评价内容',
  `status` INT NOT NULL COMMENT '评价状态 1->正常 -1->删除',
  `extra_json` TEXT  NULL COMMENT '评价预留字段,存一些图片信息,json表示',
  `has_display` TINYINT DEFAULT FALSE COMMENT '是否已晒单',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
)COMMENT = '评价表';

CREATE INDEX idx_order_comment_user_id ON parana_order_comments(`user_id`);
CREATE INDEX idx_order_comment_item_id  ON parana_order_comments(`item_id`);
CREATE INDEX idx_order_comment_shop_id ON parana_order_comments(`shop_id`);


-- 结算相关

-- 支付渠道明细
DROP TABLE IF EXISTS `parana_pay_channel_details`;

CREATE TABLE `parana_pay_channel_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pay_channel_code`  VARCHAR(512)  NULL COMMENT '支付渠道Code',
  `channel`  VARCHAR(64)  NULL COMMENT '支付渠道名称',
  `channel_account`  VARCHAR(128)  NULL COMMENT '支付渠道账户',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `gateway_rate`  BIGINT(20)  NULL COMMENT '支付平台佣金比率',
  `trade_fee`  BIGINT(20)  NULL COMMENT '实际入账金额 正数（收入）负数（支出）',
  `actual_income_fee`  BIGINT(20)  NULL COMMENT '实际入账金额 正数（收入）负数（支出）',
  `trade_type`  BIGINT(20)  NULL COMMENT '交易类型1 支付 -1 退款',
  `trade_no`  VARCHAR(512)  NULL COMMENT '电商平台交易流水号',
  `gateway_trade_no`  VARCHAR(512)  NULL COMMENT '支付平台交易流水号',
  `check_status`  BIGINT(20)  NULL COMMENT '对账状态',
  `check_finished_at`  DATETIME  NULL COMMENT '对账完成时间',
  `trade_finished_at`  DATETIME  NULL COMMENT '支付成功或退款成功时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 支付渠道日汇总
DROP TABLE IF EXISTS `parana_pay_channel_daily_summarys`;

CREATE TABLE `parana_pay_channel_daily_summarys` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `channel`  VARCHAR(512)  NULL COMMENT '支付渠道名称',
  `trade_fee`  BIGINT(20)  NULL COMMENT '交易金额',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `net_income_fee`  BIGINT(20)  NULL COMMENT '净收入',
  `sum_at` DATE NULL  COMMENT '汇总时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 平台日汇总
DROP TABLE IF EXISTS `parana_platform_trade_daily_summarys`;

CREATE TABLE `parana_platform_trade_daily_summarys` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_count`  BIGINT(20)  NULL COMMENT '订单数',
  `refund_order_count`  BIGINT(20)  NULL COMMENT '退款单数',
  `origin_fee`  BIGINT(20)  NULL COMMENT '应收货款',
  `refund_fee`  BIGINT(20)  NULL COMMENT '退款金额',
  `seller_discount`  BIGINT(20)  NULL COMMENT '商家优惠',
  `platform_discount`  BIGINT(20)  NULL COMMENT '电商平台优惠',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费优惠',
  `actual_pay_fee`  BIGINT(20)  NULL COMMENT '实收货款',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `platform_commission`  BIGINT(20)  NULL COMMENT '电商平台佣金',
  `seller_receivable_fee`  BIGINT(20)  NULL COMMENT '商家应收',
  `summary_type`  int  NULL COMMENT '汇总类型: 0-所有, 1-正向, 2-逆向',
  `sum_at`  DATETIME  NULL COMMENT '汇总时间（该数据是某一天的）',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 商家日汇总
DROP TABLE IF EXISTS `parana_seller_trade_daily_summarys`;

CREATE TABLE `parana_seller_trade_daily_summarys` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `seller_id`  BIGINT(20)  NULL COMMENT '商家ID',
  `seller_name`  VARCHAR(512)  NULL COMMENT '商家名称',
  `order_count`  BIGINT(20)  NULL COMMENT '订单数',
  `refund_order_count`  BIGINT(20)  NULL COMMENT '退款单数',
  `origin_fee`  BIGINT(20)  NULL COMMENT '应收货款',
  `refund_fee`  BIGINT(20)  NULL COMMENT '退款金额',
  `seller_discount`  BIGINT(20)  NULL COMMENT '商家优惠',
  `platform_discount`  BIGINT(20)  NULL COMMENT '电商平台优惠',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费优惠',
  `actual_pay_fee`  BIGINT(20)  NULL COMMENT '实收货款',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `platform_commission`  BIGINT(20)  NULL COMMENT '电商平台佣金',
  `seller_receivable_fee`  BIGINT(20)  NULL COMMENT '商家应收',
  `summary_type`  int  NULL COMMENT '汇总类型: 0-所有, 1-正向, 2-逆向',
  `sum_at`  DATETIME  NULL COMMENT '汇总时间（该数据是某一天的）',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 账务明细
DROP TABLE IF EXISTS `parana_settlements`;

CREATE TABLE `parana_settlements` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `channel`  VARCHAR(64)  NULL COMMENT '支付渠道',
  `channel_account`  VARCHAR(128)  NULL COMMENT '支付渠道账户',
  `origin_fee`  BIGINT(20)  NULL COMMENT '应收(退)货款',
  `seller_discount`  BIGINT(20)  NULL COMMENT '商家优惠',
  `platform_discount`  BIGINT(20)  NULL COMMENT '电商平台优惠',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费优惠',
  `actual_fee`  BIGINT(20)  NULL COMMENT '实收(退)货款',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `platform_commission`  BIGINT(20)  NULL COMMENT '电商平台佣金',
  `trade_type`  BIGINT(20)  NULL COMMENT '交易类型',
  `trade_no`  VARCHAR(512)  NULL COMMENT '电商平台交易流水号 (支付和退款的流水号)',
  `gateway_trade_no`  VARCHAR(512)  NULL COMMENT '支付平台交易流水号 (支付和退款的流水号)',
  `refund_no`  VARCHAR(512)  NULL COMMENT '电商平台退款流水号',
  `gateway_refund_no`  VARCHAR(512)  NULL COMMENT '支付平台退款流水号',
  `order_ids`  VARCHAR(512)  NULL COMMENT '订单号, 当合并支付时会有多个, 用逗号分割, 如: 1,2,3',
  `payment_or_refund_id`  BIGINT(20)  NULL COMMENT '支付订单号或退款订单号',
  `check_status`  BIGINT(20)  NULL COMMENT '对账状态',
  `check_finished_at`  DATETIME  NULL COMMENT '对账完成时间',
  `trade_finished_at`  DATETIME  NULL COMMENT '支付成功或退款成功时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 订单明细
DROP TABLE IF EXISTS `parana_settle_order_details`;


CREATE TABLE `parana_settle_order_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_id`  BIGINT(20)  NULL COMMENT '订单id,  也可以是子订单级别的, 由orderType决定',
  `order_type`  BIGINT(20)  NULL COMMENT '本次支付针对的订单类型, 1: 店铺订单支付, 2: 子订单支付',
  `seller_id`  BIGINT(20)  NULL COMMENT '商家ID',
  `seller_name` VARCHAR(128) NULL COMMENT '商家名称',
  `origin_fee`  BIGINT(20)  NULL COMMENT '应收货款',
  `seller_discount`  BIGINT(20)  NULL COMMENT '商家优惠',
  `platform_discount`  BIGINT(20)  NULL COMMENT '电商平台优惠',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费优惠',
  `actual_pay_fee`  BIGINT(20)  NULL COMMENT '实收货款',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `platform_commission`  BIGINT(20)  NULL COMMENT '电商平台佣金',
  `seller_receivable_fee`  BIGINT(20)  NULL COMMENT '商家应收',
  `trade_no`  VARCHAR(64)  NULL  COMMENT '电商平台支付流水号',
  `gateway_trade_no`  VARCHAR(64)  NULL  COMMENT '支付平台支付流水号',
  `channel`  VARCHAR(32)  NULL  COMMENT '支付渠道',
  `channel_account`  VARCHAR(128)  NULL  COMMENT '支付渠道账号',
  `order_created_at`  DATETIME  NULL  COMMENT '订单创建时间',
  `order_finished_at`  DATETIME  NULL  COMMENT '订单完成时间',
  `paid_at`  DATETIME  NULL COMMENT '支付时间',
  `check_at`  DATETIME  NULL COMMENT '对账时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 退款单
DROP TABLE IF EXISTS `parana_settle_refund_order_details`;

CREATE TABLE `parana_settle_refund_order_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_id`  BIGINT(20)  NULL COMMENT '订单id,  也可以是子订单级别的, 由orderType决定',
  `sku_order_id`  BIGINT(20)  NULL COMMENT '子订单单Id',
  `seller_id`  BIGINT(20)  NULL COMMENT '商家ID',
  `seller_name` VARCHAR(128) NULL COMMENT '商家名称',
  `origin_fee`  BIGINT(20)  NULL COMMENT '应退货款',
  `seller_discount`  BIGINT(20)  NULL COMMENT '商家优惠',
  `platform_discount`  BIGINT(20)  NULL COMMENT '电商平台优惠',
  `ship_fee`  BIGINT(20)  NULL COMMENT '应退运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费优惠',
  `actual_refund_fee`  BIGINT(20)  NULL COMMENT '实退货款',
  `platform_commission`  BIGINT(20)  NULL COMMENT '应退电商平台佣金',
  `gateway_commission`  BIGINT(20)  NULL COMMENT '支付平台佣金',
  `seller_deduct_fee`  BIGINT(20)  NULL COMMENT '商家应扣',
  `refund_id`  BIGINT  NULL  COMMENT '退款单号',
  `channel`  VARCHAR(64)  NULL  COMMENT '支付渠道',
  `channel_account`  VARCHAR(64)  NULL  COMMENT '支付渠道账号',
  `refund_no`  VARCHAR(64)  NULL  COMMENT '电商平台退款流水号',
  `gateway_refund_no`  VARCHAR(64)  NULL  COMMENT '支付平台退款流水号',
  `refund_created_at`  DATETIME  NULL  COMMENT '退款订单创建时间',
  `refund_agreed_at`  DATETIME  NULL  COMMENT '退款订单同意时间',
  `refund_at`  DATETIME  NULL COMMENT '退款时间',
  `check_at`  DATETIME  NULL COMMENT '对账时间',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
);

-- 平台抽佣规则
DROP TABLE IF EXISTS `parana_commission_rules`;

CREATE TABLE IF NOT EXISTS `parana_commission_rules` (
  `id`                      BIGINT        NOT NULL    AUTO_INCREMENT COMMENT '自增主键',
  `business_type`           SMALLINT      NOT NULL    COMMENT '业务类型 1:店铺,2:类目',
  `business_id`             BIGINT        NOT NULL    COMMENT '业务id',
  `business_name`           VARCHAR(64)   NULL        COMMENT '业务名称',
  `rate`                    INT(11) DEFAULT NULL      COMMENT '费率(针对每家店铺万分之几)',
  `description`             VARCHAR(64)   NULL        COMMENT '描述',
  `created_at`              DATETIME      NULL        COMMENT '创建时间',
  `updated_at`              DATETIME      NULL        COMMENT '修改时间',
  PRIMARY KEY (`id`));


-- 第三方账务


-- -----------------------------------------------------
-- Table `parana_alipay_trans`  支付宝交易对账数据
-- -----------------------------------------------------
DROP TABLE IF EXISTS `parana_alipay_trans`;
CREATE TABLE IF NOT EXISTS `parana_alipay_trans` (
  `id`                          BIGINT        NOT NULL   AUTO_INCREMENT COMMENT '自增主键',
  `balance`                     VARCHAR(32)   NULL       COMMENT '账户余额',
  `bank_account_name`           VARCHAR(32)   NULL       COMMENT '银行账户名称',
  `bank_account_no`             VARCHAR(32)   NULL       COMMENT '银行账户',
  `bank_name`                   VARCHAR(64)   NULL       COMMENT '银行名',
  `buyer_name`                  VARCHAR(127)  NULL       COMMENT '买家姓名',
  `buyer_account`               VARCHAR(32)   NULL       COMMENT '买家账户',
  `currency`                    VARCHAR(16)   NULL       COMMENT '货币代码(156:人民币)',
  `deposit_bank_no`             VARCHAR(32)   NULL       COMMENT '充值网银流水',
  `income`                      VARCHAR(32)   NULL       COMMENT '收入金额',
  `iw_account_log_id`           VARCHAR(32)   NULL       COMMENT '帐务流水',
  `memo`                        VARCHAR(127)  NULL       COMMENT '备注信息',
  `merchant_out_order_no`       VARCHAR(64)   NULL       COMMENT '外部交易编号（订单号）',
  `other_account_email`         VARCHAR(127)  NULL       COMMENT '帐务对方邮箱',
  `other_account_fullname`      VARCHAR(127)  NULL       COMMENT '帐务对方全称',
  `other_user_id`               VARCHAR(32)   NULL       COMMENT '帐务对方支付宝用户号',
  `outcome`                     VARCHAR(32)   NULL       COMMENT '支出金额',
  `partner_id`                  VARCHAR(32)   NULL       COMMENT '合作者身份id',
  `seller_account`              VARCHAR(32)   NULL       COMMENT '买家支付宝人民币支付帐号(user_id+0156)',
  `seller_fullname`             VARCHAR(64)   NULL       COMMENT '卖家姓名',
  `service_fee`                 VARCHAR(32)   NULL       COMMENT '交易服务费',
  `service_fee_ratio`           VARCHAR(16)   NULL       COMMENT '交易服务费率',
  `total_fee`                   VARCHAR(32)   NULL       COMMENT '交易总金额',
  `trade_no`                    VARCHAR(32)   NULL       COMMENT '支付宝交易流水',
  `trade_refund_amount`         VARCHAR(32)   NULL       COMMENT '累计退款金额',
  `trans_account`               VARCHAR(32)   NULL       COMMENT '帐务本方支付宝人民币资金帐号(user_id+0156)',
  `trans_code_msg`              VARCHAR(16)   NULL       COMMENT '业务类型',
  `trans_date`                  VARCHAR(32)   NULL       COMMENT '交易发生日期',
  `trans_out_order_no`          VARCHAR(32)   NULL       COMMENT '商户订单号',
  `sub_trans_code_msg`          VARCHAR(32)   NULL       COMMENT '子业务类型代码，详见文档',
  `sign_product_name`           VARCHAR(32)   NULL       COMMENT '签约产品',
  `rate`                        VARCHAR(16)   NULL       COMMENT '费率',
  `trade_at`                    DATETIME      NULL       COMMENT '交易时间',
  `created_at`                  DATETIME      NULL       COMMENT '创建时间',
  `updated_at`                  DATETIME      NULL       COMMENT '修改时间',
  PRIMARY KEY (`id`)
);
CREATE INDEX idx_eat_iw_account_log_id ON parana_alipay_trans (iw_account_log_id);
CREATE INDEX idx_eat_trans_no ON parana_alipay_trans (trade_no);
CREATE INDEX idx_eat_merchant_no ON parana_alipay_trans (merchant_out_order_no);


DROP TABLE IF EXISTS `parana_wechatpay_trans`;
-- 微信支付账务明细
CREATE TABLE `parana_wechatpay_trans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `transaction_id`    VARCHAR(32)   NOT NULL COMMENT '微信支付订单号',
  `out_trade_no`      VARCHAR(32)   NOT NULL COMMENT '商户系统的订单号，与请求一致',
  `trade_status`      VARCHAR(16)   NULL COMMENT '交易状态 SUCCESS FAIL',
  `trade_time`        VARCHAR(32)   NOT NULL COMMENT '交易时间',
  `appid`             VARCHAR(64)   NULL COMMENT '公众账号 ID',
  `mch_id`            VARCHAR(32)   NULL COMMENT '微信支付分配的商户号',
  `sub_mch_id`        VARCHAR(32)   NULL COMMENT '微信支付分配的子商户号， 受理模式下必填',
  `device_info`       VARCHAR(128)  NULL COMMENT '微信支付分配的终端设备号',
  `open_id`           VARCHAR(128)  NULL COMMENT '用户标识',
  `trade_type`        VARCHAR(16)   NULL COMMENT 'JSAPI、NATIVE、APP 交易类型',
  `bank_type`         VARCHAR(32)   NULL COMMENT '付款银行，采用字符串类型的银行标识',
  `fee_type`          VARCHAR(16)   NULL COMMENT '货币种类，符合ISO 4217 标准的三位字母代码，默认人民币：CNY',
  `total_fee`         VARCHAR(64)   NOT NULL COMMENT '本次交易金额 元',
  `coupon_fee`        VARCHAR(64)   NULL  COMMENT '现金券金额 元',
  `refund_apply_date`  VARCHAR(32)   NULL COMMENT '退款申请时间',
  `refund_success_date` VARCHAR(32)   NULL COMMENT '退款成功时间',
  `refund_id`         VARCHAR(32)   NULL COMMENT '微信退款单号',
  `out_refund_no`     VARCHAR(32)   NULL COMMENT '商户退款单号',
  `refund_fee`        VARCHAR(64)   NULL COMMENT '退款金额',
  `coupon_refund_fee` VARCHAR(64)   NULL COMMENT '现金券退款金额',
  `refund_channel`    VARCHAR(16)   NULL COMMENT '退款类型 ORIGINAL—原路退款 BALANCE—退回到余额',
  `refund_status`     VARCHAR(16)   NULL COMMENT '退款状态：SUCCES—退款成功 FAIL—退款失败 PROCESSING—退款处理中 NOTSURE—未确定，需要商户 原退款单号重新发起 CHANGE—转入代发，退款到 银行发现用户的卡作废或者冻结了，导致原路退款银行 卡失败，资金回流到商户的现金帐号，需要商户人工干 预，通过线下或者财付通转 账的方式进行退款。',
  `body`              VARCHAR(128)  NULL COMMENT '商品名称',
  `attach`            TEXT          NULL COMMENT '商户数据包 附加数据',
  `poundage_fee`      VARCHAR(64)   NULL COMMENT '手续费',
  `rate`              VARCHAR(16)   NULL COMMENT '费率',
  `bank_order_no`     VARCHAR(64)   NULL COMMENT '银行订单号',
  `trade_info`        TEXT          NULL COMMENT '交易说明',
  `trade_at`                    DATETIME      NULL       COMMENT '交易时间',
  `created_at`                  DATETIME      NULL       COMMENT '创建时间',
  `updated_at`                  DATETIME      NULL       COMMENT '修改时间',
  PRIMARY KEY (`id`)
)COMMENT = '微信支付账务明细';




-- unionpay对账数据

DROP TABLE IF EXISTS `parana_unionpay_trans`;

CREATE TABLE IF NOT EXISTS `parana_unionpay_trans` (
  `id`                          BIGINT        NOT NULL   AUTO_INCREMENT COMMENT '自增主键',
  `transaction_code`            VARCHAR(32)   NULL       COMMENT '交易码',
  `acq_ins_code`                VARCHAR(32)   NULL       COMMENT '代理机构标识码',
  `send_code`                   VARCHAR(32)   NULL       COMMENT '发送机构标识码',
  `trace_no`                    VARCHAR(32)   NULL       COMMENT '系统跟踪号',
  `pay_card_no`                 VARCHAR(32)   NULL       COMMENT '帐号',
  `txn_amt`                     VARCHAR(32)   NULL       COMMENT '交易金额',
  `mer_cat_code`                VARCHAR(32)   NULL       COMMENT '商户类别',
  `term_type`                   VARCHAR(32)   NULL       COMMENT '终端类型',
  `query_id`                    VARCHAR(32)   NULL       COMMENT '查询流水号',
  `type`                        VARCHAR(32)   NULL       COMMENT '支付方式（旧）',
  `order_id`                    VARCHAR(32)   NULL       COMMENT '商户订单号',
  `pay_card_type`               VARCHAR(32)   NULL       COMMENT '支付卡类型',
  `original_trace_no`           VARCHAR(32)   NULL       COMMENT '原始交易的系统跟踪号',
  `original_time`                VARCHAR(32)   NULL       COMMENT '原始交易日期时间',
  `third_party_fee`             VARCHAR(32)   NULL       COMMENT '商户手续费',
  `settle_amount`               VARCHAR(32)   NULL       COMMENT '结算金额',
  `pay_type`                    VARCHAR(32)   NULL       COMMENT '支付方式',
  `company_code`                VARCHAR(32)   NULL       COMMENT '集团商户代码',
  `txn_type`                    VARCHAR(32)   NULL       COMMENT '交易类型',
  `txn_sub_type`                VARCHAR(32)   NULL       COMMENT '交易子类',
  `biz_type`                    VARCHAR(32)   NULL       COMMENT '业务类型',
  `acc_type`                    VARCHAR(32)   NULL       COMMENT '帐号类型',
  `bill_type`                  VARCHAR(32)   NULL       COMMENT '账单类型',
  `bill_no`                     VARCHAR(32)   NULL       COMMENT '账单号码  ',
  `interact_mode`               VARCHAR(32)   NULL       COMMENT '交互方式',
  `orig_qry_id`                  VARCHAR(32)   NULL       COMMENT '原交易查询流水号',
  `mer_id`                      VARCHAR(32)   NULL       COMMENT '商户代码',
  `divide_type`                 VARCHAR(32)   NULL       COMMENT '分账入账方式',
  `sub_mer_id`                  VARCHAR(32)   NULL       COMMENT '二级商户代码',
  `sub_mer_abbr`                VARCHAR(32)   NULL       COMMENT '二级商户简称',
  `divide_amount`               VARCHAR(32)   NULL       COMMENT '二级商户分账入账金额',
  `clearing`                    VARCHAR(32)   NULL       COMMENT '清算净额',
  `term_id`                     VARCHAR(32)   NULL       COMMENT '终端号',
  `mer_reserved`                VARCHAR(32)   NULL       COMMENT '商户自定义域',
  `discount`                    VARCHAR(32)   NULL       COMMENT '优惠金额',
  `invoice`                     VARCHAR(32)   NULL       COMMENT '发票金额',
  `addition_third_party_fee`    VARCHAR(32)   NULL       COMMENT '分期付款附加手续费',
  `stage`                       VARCHAR(32)   NULL       COMMENT '分期付款期数',
  `transaction_media`           VARCHAR(32)   NULL       COMMENT '交易介质',
  `original_order_id`           VARCHAR(32)   NULL       COMMENT '原始交易订单号',
  `txn_time`                    DATETIME      NULL       COMMENT '交易时间',
  `created_at`                  DATETIME      NULL       COMMENT '创建时间',
  `updated_at`                  DATETIME      NULL       COMMENT '修改时间',
  PRIMARY KEY (`id`)
);
CREATE INDEX idx_put_query_id ON parana_unionpay_trans (query_id);


-- -----------------------------------------------------
-- Table `parana_kjtpay_trans`  快捷通交易对账数据
-- -----------------------------------------------------
DROP TABLE IF EXISTS `parana_kjtpay_trans`;
CREATE TABLE IF NOT EXISTS `parana_kjtpay_trans` (
  `id`                          BIGINT        NOT NULL   AUTO_INCREMENT COMMENT '自增主键',
  `outer_no`                    VARCHAR(64)   NULL       COMMENT '商户订单号',
  `orig_outer_no`               VARCHAR(64)   NULL       COMMENT '原商户订单号',
  `inner_no`                    VARCHAR(64)   NULL       COMMENT '交易订单号',
  `type`                        VARCHAR(64)   NULL       COMMENT '交易类型',
  `amount`                      VARCHAR(64)   NULL       COMMENT '交易下单时间,支付时间',
  `rate`                        VARCHAR(32)   NULL       COMMENT '费率',
  `rate_fee`                    VARCHAR(32)   NULL       COMMENT '手续费',
  `status`                      VARCHAR(16)   NULL       COMMENT '状态',
  `order_at`                   DATETIME      NULL       COMMENT '交易下单时间',
  `paid_at`                   DATETIME      NULL       COMMENT '支付时间',
  `created_at`                  DATETIME      NULL       COMMENT '创建时间',
  `updated_at`                  DATETIME      NULL       COMMENT '修改时间',
  PRIMARY KEY (`id`)
);
CREATE INDEX idx_ekt_outer_no ON parana_kjtpay_trans (outer_no);
CREATE INDEX idx_ekt_inner_no ON parana_kjtpay_trans (inner_no);



-- -----------------------------------------------------
-- Table `parana_settlement_abnormal_track` 帐务记录异常跟踪
-- -----------------------------------------------------
DROP TABLE IF EXISTS `parana_settle_abnormal_tracks`;

CREATE TABLE IF NOT EXISTS `parana_settle_abnormal_tracks` (
  `id`                      BIGINT        NOT NULL    AUTO_INCREMENT COMMENT '自增主键',
  `abnormal_info`           VARCHAR(256)        NOT NULL    COMMENT '异常信息',
  `abnormal_type`           SMALLINT      NULL        COMMENT '异常类型',
  `is_handle`               BIT           NULL     DEFAULT 0 COMMENT '是否已处理',
  `description`             VARCHAR(256)  NULL        COMMENT '备注',
  `created_at`              DATETIME      NULL        COMMENT '创建时间',
  `updated_at`              DATETIME      NULL        COMMENT '修改时间',
  PRIMARY KEY (`id`));

CREATE INDEX idx_esat_abnormal_type ON parana_settle_abnormal_tracks(abnormal_type);


-- 用户表: parana_users
DROP TABLE IF EXISTS `parana_users`;

CREATE TABLE `parana_users` (
  `id`                BIGINT          NOT NULL    AUTO_INCREMENT,
  `name`              VARCHAR(40)     NULL        COMMENT '用户名',
  `email`             VARCHAR(32)     NULL        COMMENT '邮件',
  `mobile`            VARCHAR(16)     NULL        COMMENT '手机号码',
  `password`          VARCHAR(32)     NULL        COMMENT '登录密码',
  `type`              SMALLINT        NOT NULL    COMMENT '用户类型',
  `status`            TINYINT         NOT NULL    COMMENT '状态 0:未激活, 1:正常, -1:锁定, -2:冻结, -3: 删除',
  `roles_json`        VARCHAR(512)    NULL        COMMENT '用户角色信息',
  `extra_json`        VARCHAR(1024)   NULL        COMMENT '用户额外信息,建议json字符串',
  `tags_json`         VARCHAR(1024)   NULL        COMMENT '用户标签的json表示形式,只能运营操作, 对商家不可见',
  `created_at`        DATETIME        NOT NULL,
  `updated_at`        DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY idx_users_name(name),
  UNIQUE KEY idx_users_email(email),
  UNIQUE KEY idx_users_mobile(mobile)
) COMMENT='用户表';

-- 菜鸟地址表
DROP TABLE IF EXISTS `parana_addresses`;

CREATE TABLE `parana_addresses` (
  `id`                BIGINT          NOT NULL,
  `pid`               BIGINT          NULL        COMMENT '父级ID',
  `name`              VARCHAR(50)     NULL        COMMENT '名称',
  `level`             INT             NULL        COMMENT '级别',
  `pinyin`            VARCHAR(100)    NULL        COMMENT '拼音',
  `english_name`      VARCHAR(100)    NULL        COMMENT '英文名',
  `unicode_code`      VARCHAR(200)    NULL        COMMENT 'ASCII码',
  `order_no`          VARCHAR(32)     NULL        COMMENT '排序号',
  PRIMARY KEY (`id`)
);

-- 用户详情表: parana_user_profiles
DROP TABLE IF EXISTS `parana_user_profiles`;

CREATE TABLE `parana_user_profiles` (
  `id`                BIGINT          NOT NULL    AUTO_INCREMENT,
  `user_id`           BIGINT          NULL        COMMENT '用户id',
  `realname`          VARCHAR(32)     NULL        COMMENT '真实姓名',
  `gender`            SMALLINT        NULL        COMMENT '性别1男2女',
  `province_id`       BIGINT          NOT NULL    COMMENT '省id',
  `province`          VARCHAR(100)    NOT NULL    COMMENT '省',
  `city_id`           BIGINT          NULL        COMMENT '城id',
  `city`              VARCHAR(100)    NULL        COMMENT '城',
  `region_id`         BIGINT          NULL        COMMENT '区id',
  `region`            VARCHAR(100)    NULL        COMMENT '区',
  `street`            VARCHAR(130)    NULL        COMMENT '地址',
  `extra_json`        VARCHAR(2048)   NULL        COMMENT '其他信息, 以json形式存储',
  `avatar`            VARCHAR(512)    NOT NULL    COMMENT '头像',
  `birth`             VARCHAR(40)     NULL        COMMENT '出生日期',
  `created_at`        DATETIME        NOT NULL,
  `updated_at`        DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY idx_user_id(user_id)
) COMMENT='用户详情表';


-- 采购单表
DROP TABLE IF EXISTS `vega_purchase_orders`;

create TABLE IF NOT EXISTS `vega_purchase_orders` (
  `id`          bigint    unsigned  not null auto_increment ,
  `name`  varchar(256)   not null  comment '采购单名称',
  `buyer_id`    bigint         not null  comment '采购员id',
  `buyer_name`  varchar(256)   not null  comment '采购员名称',
  `sku_quantity`       int       not null  comment '商品种类',
  `extra_json` varchar(1024) DEFAULT NULL COMMENT'扩展字段JSON格式',
  `created_at`  datetime       not null  comment '创建时间',
  `updated_at`  datetime       not null  comment '更新时间',
  PRIMARY KEY (`id`)
);
create index idx_vpo_buyer_id on vega_purchase_orders (buyer_id);
create index idx_vpo_name on vega_purchase_orders (name);



-- 采购商品清单表
DROP TABLE IF EXISTS `vega_purchase_sku_orders`;

create TABLE IF NOT EXISTS `vega_purchase_sku_orders` (
  `id`          bigint    unsigned  not null auto_increment ,
  `purchase_id` bigint         not null  comment '采购单id',
  `buyer_id`    bigint         not null  comment '采购员id',
  `buyer_name`  varchar(256)   not null  comment '采购员名称',
  `shop_id`     BIGINT         not NULL  COMMENT '店铺id',
  `shop_name`   varchar(64)    not null  comment '店铺名称',
  `sku_id`         bigint    not null  comment '对应销售属性id',
  `quantity`       int       not null  comment '数量',
  `status`         tinyint  not null  comment '0未选中，1已选中',
  `extra_json` varchar(1024) DEFAULT NULL COMMENT'扩展字段JSON格式',
  `created_at`  datetime       not null  comment '创建时间',
  `updated_at`  datetime       not null  comment '更新时间',
  PRIMARY KEY (`id`)
);
create index idx_vpso_buyer_id on vega_purchase_sku_orders (buyer_id);
create index idx_vpso_purchase_id on vega_purchase_sku_orders (purchase_id);
create index idx_vpso_sku_id on vega_purchase_sku_orders (sku_id);

-- 派送单关联表
DROP TABLE IF EXISTS `vega_order_dispatch_relation`;

create TABLE IF NOT EXISTS `vega_order_dispatch_relation` (
  `id`    bigint   unsigned  not null auto_increment,
  `operate_id` bigint DEFAULT null COMMENT '操作人ID',
  `operate_name` varchar(256) DEFAULT null COMMENT '操作人名称',
  `order_id` bigint DEFAULT null COMMENT '订单id',
  `receive_shop_id` bigint DEFAULT null COMMENT '接单店铺id',
  `dispatch_shop_id` bigint DEFAULT null COMMENT '派单店铺id',
  `created_at` datetime DEFAULT null COMMENT '创建时间',
  `updated_at` datetime DEFAULT null COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '派送单关联表';
CREATE INDEX `idx_order_dispatch_relation_order_id` ON `vega_order_dispatch_relation` (`order_id`);
CREATE INDEX `idx_order_dispatch_relation_receive_shop_id` ON `vega_order_dispatch_relation` (`receive_shop_id`);


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

-- 经销商授权类目折扣表
CREATE TABLE `vega_category_authes`(
`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`shop_id` BIGINT(20)  NOT NULL COMMENT '店铺ID',
`shop_name` varchar(50) NOT NULL COMMENT '店铺名称',
`category_auth_list` text DEFAULT NULL COMMENT '经销商授权类目List',
`discount_lower_limit` int(10) DEFAULT NULL COMMENT '倍率下限',
`category_discount_list` text DEFAULT NULL COMMENT '经销商针对类目设置的折扣表',
`created_at` datetime NOT NULL COMMENT '创建时间',
`updated_at` datetime NOT NULL COMMENT '最后一次更新时间',
PRIMARY KEY(`id`)
)COMMENT='经销商授权类目折扣';
CREATE INDEX `idx_vega_category_authes_shop_id` ON `vega_category_authes`(shop_id);

