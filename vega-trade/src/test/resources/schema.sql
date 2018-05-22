-- 采购单表
DROP TABLE IF EXISTS `vega_purchase_orders`;

create TABLE IF NOT EXISTS `vega_purchase_orders` (
  `id`          bigint    unsigned  not null auto_increment ,
  `name`  varchar(256)   not null  comment '采购单名称',
  `buyer_id`    bigint         not null  comment '采购员id',
  `buyer_name`  varchar(256)   not null  comment '采购员名称',
  `sku_quantity`       int       not null  comment '商品种类',
  `is_temp`       tinyint(4)     not null  comment '是否为临时采购单',
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



-- -----------------------------------------------------
-- Table `vega_allinpay_trans`  银联交易对账数据
-- -----------------------------------------------------
DROP TABLE IF EXISTS `vega_allinpay_trans`;
CREATE TABLE IF NOT EXISTS `vega_allinpay_trans` (
  `id`                          BIGINT        NOT NULL   AUTO_INCREMENT COMMENT '自增主键',
  `trans_code_msg`              VARCHAR(32)   NULL       COMMENT '交易类型(ZF:支付成功、TH:退款成功,资金已经退款至持卡人账户、CX:冲销成功,未能成功退款,资金已经退回给商户)',
  `trans_date`                  VARCHAR(32)   NULL       COMMENT '结算日期',
  `seller_account`              VARCHAR(32)   NULL       COMMENT '商户号',
  `trade_at`                    DATETIME      NULL       COMMENT '交易时间',
  `trans_out_order_no`          VARCHAR(32)   NULL       COMMENT '商户订单号',
  `trade_no`                    VARCHAR(32)   NULL       COMMENT '通联交易流水',
  `total_fee`                   VARCHAR(32)   NULL       COMMENT '交易总金额',
  `service_fee`                 VARCHAR(32)   NULL       COMMENT '交易服务费',
  `service_fee_ratio`           VARCHAR(16)   NULL       COMMENT '交易服务费率',
  `settlement_fee`              VARCHAR(32)   NULL       COMMENT '清算金额(分)',
  `currency`                    VARCHAR(16)   NULL       COMMENT '货币代码(156:人民币)',
  `order_origin_fee`            VARCHAR(32)   NULL       COMMENT '商户原始订单金额(分)',
  `memo`                        VARCHAR(127)  NULL       COMMENT '备注信息',
  `created_at`                  DATETIME      NULL       COMMENT '创建时间',
  `updated_at`                  DATETIME      NULL       COMMENT '修改时间',
  PRIMARY KEY (`id`)
);
CREATE INDEX idx_vat_trans_code_msg ON vega_allinpay_trans (trans_code_msg);
CREATE INDEX idx_vat_trans_no ON vega_allinpay_trans (trade_no);
CREATE INDEX idx_vat_trans_out_order_no ON vega_allinpay_trans (trans_out_order_no);


DROP TABLE IF EXISTS `vega_direct_pay_info`;
CREATE TABLE `vega_direct_pay_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL COMMENT '订单号,日汇总的ID号',
  `business_id` varchar(100) NOT NULL COMMENT '业务参考号',
  `status` integer DEFAULT NULL COMMENT '处理结果',
  `describe` varchar(2048) DEFAULT NULL COMMENT '描述信息',
  `extra_json` varchar(2048) DEFAULT NULL COMMENT '额外信息',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX vega_direct_pay_info_business_id ON vega_direct_pay_info (business_id);



-- 积分商品订单
DROP TABLE IF EXISTS `vega_integration_orders`;
create TABLE  `vega_integration_orders` (
  `id`                     bigint   unsigned  not null auto_increment,
  `buyer_id`               bigint   unsigned  not null COMMENT '购买用户ID',
  `buyer_name`             varchar(64)    default null COMMENT '购买用户名称',
  `buyer_phone`            varchar(64)    default null COMMENT '电话号码',
  `item_id`                bigint   unsigned  not null COMMENT '积分商品ID',
  `item_name`              varchar(64)    default null COMMENT '积分商品名称',
  `item_image`             varchar(512) DEFAULT NULL   COMMENT '商品图片',
  `integration_price`      INTEGER        default null COMMENT '积分单价',
  `integration_fee`        INTEGER  unsigned  not null COMMENT '花费积分',
  `quantity`               INTEGER  unsigned  not null COMMENT '数量',
  `status`                 INTEGER    default null COMMENT '订单状态, 2:已完成, 1:待发货',
  `address_info_json`      varchar(512)      DEFAULT null COMMENT '收货地址信息',
  `delivery_company`       varchar(64)    default null COMMENT '快递公司',
  `delivery_no`            varchar(64)    default null COMMENT '快递单号',
  `extra_json`             text       DEFAULT null COMMENT '扩展信息字段',
  `created_at` datetime DEFAULT null COMMENT '创建时间',
  `updated_at` datetime DEFAULT null COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT = '积分商品订单';


DROP TABLE IF EXISTS `vega_seller_trade_daily_summarys`;
CREATE TABLE `vega_seller_trade_daily_summarys` (
  `id`                     bigint   unsigned  not null auto_increment,
  `seller_id` bigint(20) DEFAULT NULL COMMENT '商家ID',
  `seller_type` bigint(20) DEFAULT NULL COMMENT '商家类型',
  `seller_name` varchar(512) DEFAULT NULL COMMENT '商家名称',
  `order_count` bigint(20) DEFAULT NULL COMMENT '订单数',
  `refund_order_count` bigint(20) DEFAULT NULL COMMENT '退款单数',
  `origin_fee` bigint(20) DEFAULT NULL COMMENT '应收货款',
  `refund_fee` bigint(20) DEFAULT NULL COMMENT '退款金额',
  `seller_discount` bigint(20) DEFAULT NULL COMMENT '商家优惠',
  `platform_discount` bigint(20) DEFAULT NULL COMMENT '电商平台优惠',
  `ship_fee` bigint(20) DEFAULT NULL COMMENT '运费',
  `ship_fee_discount` bigint(20) DEFAULT NULL COMMENT '运费优惠',
  `actual_pay_fee` bigint(20) DEFAULT NULL COMMENT '实收货款',
  `gateway_commission` bigint(20) DEFAULT NULL COMMENT '支付平台佣金',
  `platform_commission` bigint(20) DEFAULT NULL COMMENT '电商平台佣金',
  `seller_receivable_fee` bigint(20) DEFAULT NULL COMMENT '商家应收',
  `summary_type` int(11) DEFAULT NULL COMMENT '汇总类型: 0-所有, 1-正向, 2-逆向',
  `sum_at` datetime DEFAULT NULL COMMENT '汇总时间（该数据是某一天的）',
  `extra_json` text DEFAULT NULL COMMENT '附加字段',
  `diff_fee` bigint(20) DEFAULT NULL COMMENT '差价',
  `commission1` bigint(20) DEFAULT NULL COMMENT '附加佣金1',
  `commission2` bigint(20) DEFAULT NULL COMMENT '附加佣金2',
  `commission3` bigint(20) DEFAULT NULL COMMENT '附加佣金3',
  `commission4` bigint(20) DEFAULT NULL COMMENT '附加佣金4',
  `commission5` bigint(20) DEFAULT NULL COMMENT '附加佣金5',
  `trans_status` int(11) DEFAULT NULL COMMENT '打款状态',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '最新更新时间',
  PRIMARY KEY (`id`)
)COMMENT='三力士商家日汇总';
