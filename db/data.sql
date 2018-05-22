
-- 新增运费优惠和店铺满减营销工具
INSERT INTO `parana_promotion_defs` (`name`, `type`, `status`, `user_scope_key`, `sku_scope_key`, `behavior_key`, `created_at`, `updated_at`)
VALUES
	('运费优惠', 21, 1, 'all', 'all', 'shipment-discount', now(), now()),
	('店铺满减', 22, 1, 'all', 'all', 'shop-condition-reduction', now(), now());

--满多少包邮初始化语句
INSERT INTO `parana_promotions` (`shop_id`, `name`, `promotion_def_id`, `type`, `status`, `start_at`, `end_at`, `user_scope_params_json`, `sku_scope_params_json`, `condition_params_json`, `behavior_params_json`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(0, '全场满500包邮', 1, 21, 2, '2017-08-25 13:00:00', '2050-08-25 13:00:00', NULL, NULL, NULL, '{\"freeShipping\":\"50000\"}', NULL, '2017-08-25 13:00:00', '2017-08-25 15:55:02');

--业务经理提成费率定义初始化语句
INSERT INTO `vega_rate_defs` (`id`, `name`, `type`, `describe`, `rate_key`, `rate_base`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(1, 'newMemberOrderCommission', 1, '新会员收取订单金额提成费率', 1, 100, NULL, '2017-11-16 10:12:56', '2017-11-16 10:12:56'),
	(2, 'oldMemberOrderCommission', 1, '老会员收取订单金额提成费率', 3, 1000, NULL, '2017-11-16 10:12:56', '2017-11-16 10:12:56');

--运营销售销售报表导出类目和后台类目对应关系初始化信息,具体对应关系根据实际修改
INSERT INTO `vega_report_month_sale` (`id`, `year`, `month`, `status`, `order_count`, `order_fee`, `order_member`, `visitor_json`, `category_json`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(5, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, '{\"三角带\":\"1,412,414\",\"轴承\":\"419,420,504\",\"脚轮\":\"506,516,559,561,595,603,649,658,660\",\"其他\":\"667,674,682,689\"}', '2018-01-08 13:10:31', '2018-01-08 13:30:17');

--土猫网数据同步配置表初始化
INSERT INTO `vega_toolmall_item_sync` (`id`, `type`, `next_begin_time`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(1, 1, '', NULL, '2018-04-19 15:11:00', '2018-04-19 15:11:00'),
	(2, 2, '', NULL, '2018-04-19 15:11:00', '2018-04-19 15:11:00'),
	(3, 3, '', NULL, '2018-04-19 15:11:00', '2018-04-19 15:11:00');
