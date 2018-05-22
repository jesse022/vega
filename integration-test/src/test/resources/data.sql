INSERT INTO `parana_back_categories`
(`pid`,`name`,`level`,`status`,`has_children`,`has_spu`,`outer_id`,`created_at`,`updated_at`)
VALUES
  (0, 'back-level1-cat1',1, 1, FALSE ,FALSE ,null,now(),now()),
  (0, 'back-level1-cat2',1, 1, FALSE ,FALSE ,null,now(),now()),
  (0, 'back-level1-cat3',1, 1, FALSE ,FALSE ,null,now(),now());

INSERT INTO `parana_category_attributes`
(`category_id`,  `attr_key`, `group`, `index`, `status`, `attr_metas_json`, `attr_vals_json`, `created_at`,`updated_at`)
VALUES
  (1, '颜色', '基本参数', 0, 1, '{"SKU_CANDIDATE":"true","REQUIRED":"true","IMPORTANT":"false","VALUE_TYPE":"STRING","USER_DEFINED":"false"}', '["红色","黑色","绿色","黄色"]', now(), now()),
  (1, '尺码', '基本参数', 1, 1, '{"SKU_CANDIDATE":"true","REQUIRED":"true","IMPORTANT":"false","VALUE_TYPE":"NUMBER_cm","USER_DEFINED":"true"}', '["110","120"]', now(), now()),
  (1, '等级', '其他参数', 2, 1, '{"REQUIRED":"false","IMPORTANT":"false","USER_DEFINED":"false"}', '["高","中","低"]', now(), now()),
  (1, '重量', '其他参数', 2, 1, '{"REQUIRED":"false","IMPORTANT":"false","VALUE_TYPE":"NUMBER","USER_DEFINED":"true"}', null, now(), now()),
  (1, '产地', '其他参数', 2, 1, '{"REQUIRED":"true","IMPORTANT":"false","USER_DEFINED":"true"}', null, now(), now());

INSERT INTO `parana_front_categories`
(`pid`,`name`,`level`,`has_children`,`created_at`,`updated_at`)
VALUES
  (0, 'front-level1-cat1',1,  FALSE ,now(),now()),
  (0, 'front-level1-cat2',1,  FALSE ,now(),now()),
  (0, 'front-level1-cat3',1,  FALSE ,now(),now());


-- init category binding table
INSERT INTO
  parana_category_bindings (`front_category_id`, `back_category_id`, `created_at`, `updated_at`)
VALUES
  (1, 1, now(), now()),
  (1, 2, now(), now()),
  (2, 3, now(), now()),
  (3, 2, now(), now());

-- init shop_category_items table

INSERT INTO
  parana_shop_category_items
  (`shop_id`, `item_id`,  `shop_category_id`, `created_at`, `updated_at`)
VALUES
  (1, 1, 1, now(), now()),
  (1, 2, 2,  now(), now()),
  (1, 3,  1,  now(), now()),
  (-1, 1, -1,  now(), now());

INSERT INTO
  parana_brands
  (`name`, `unique_name`, `en_name`, `en_cap`, `logo`, `description`, `status`, `created_at`, `updated_at`)
VALUES
  ('Nike', 'nike','nike','N',null,'nike is good',1, now(), now());




INSERT INTO `parana_spus` (`id`, `spu_code`, `category_id`, `brand_id`, `brand_name`, `name`,
                           `main_image`, `low_price`, `high_price`, `stock_type`, `stock_quantity`, `status`,
                           `advertise`, `specification`, `type`, `reduce_stock_type`, `extra_json`,
                           `spu_info_md5`, `created_at`, `updated_at`)
VALUES
  (1,NULL,1,1,'nike','testSpu1','http://taobao.com',6000,6000,0,24,1,NULL,NULL,1,1,'{"unit":"双"}',
   'c268a27a7fcbd244f413875d34421ea7',now(),now());


INSERT INTO `parana_spu_details` (`spu_id`, `images_json`, `detail`, `packing_json`, `service`, `created_at`, `updated_at`)
VALUES
  (1,'[{"name":"xx","url":"http://xx0oo.com"}]','i am detail','{"包邮":"true"}',NULL,now(),now());


INSERT INTO `parana_spu_attributes` (`spu_id`, `sku_attributes`, `other_attributes`, `created_at`, `updated_at`)
VALUES
  (1,'[{"attrKey":"尺码","skuAttributes":[{"attrKey":"尺码","attrVal":"120","unit":"cm"}]}]',
   '[{"group":"其他参数","otherAttributes":[{"attrKey":"产地","attrVal":"杭州"}]}]',now(),now());

INSERT INTO `parana_sku_templates` (`id`, `sku_code`, `spu_id`, `specification`, `status`,
                                    `image`, `thumbnail`, `name`, `extra_price_json`, `price`, `attrs_json`,
                                    `stock_type`, `stock_quantity`, `extra`, `created_at`, `updated_at`)
VALUES
  (1,'skuCode1',1,'ZUC-CW000-RED',1,'image1','thumbnail1','sku1','{"originPrice":3,"platformPrice":4}',
     6000,'[{"attrKey":"尺码","attrVal":"120","unit":"cm"}]',0,830869938,NULL,now(),now());



INSERT INTO `parana_shops` (`id`, `outer_id`, `user_id`, `user_name`, `name`, `status`, `type`, `phone`,
`business_id`, `image_url`, `address`, `extra_json`, `tags_json`, `created_at`, `updated_at`)
VALUES
	(0,'',1,'admin','平台店铺',1,0,'',NULL,'//terminus-designer.oss-cn-hangzhou.aliyuncs.com/2016/07/11/610f7eb0-aba4-4484-918c-f2cb53e7219c.png',NULL,
	'','{"integralScale":"10000","creditInterest":"12"}','2016-03-15 21:14:27','2016-09-07 17:30:06'),
	(28,NULL,42,'supplier1','supplier1',1,1,NULL,NULL,NULL,NULL,NULL,NULL,'2016-08-15 09:20:19','2016-09-07 15:57:48'),
	(53,NULL,63,'dealer1-10','dealer1-10',1,2,NULL,NULL,NULL,NULL,NULL,NULL,'2016-08-18 09:45:09','2016-09-07 14:27:43'),
	(56,NULL,64,'dealer2-10','dealer2-10',1,3,NULL,NULL,NULL,NULL,NULL,NULL,'2016-08-18 09:49:30','2016-09-07 15:40:05');



INSERT INTO `vega_shop_extras` (`id`, `shop_id`, `shop_pid`, `shop_parent_name`, `shop_name`, `shop_status`, `shop_type`,
 `user_id`, `user_name`, `purchase_discount`, `discount_lower_limit`, `member_discount_json`, `available_credit`,
 `total_credit`, `credit_payment_days`, `is_credit_available`, `bank_account`, `contact_name`, `contact_phone`,
  `province_id`, `province`, `city_id`, `city`, `region_id`, `region`, `street`, `postcode`, `created_at`, `updated_at`)
VALUES
	(15,53,0,NULL,'dealer1-10',1,2,63,NULL,100,80,'{"1":180,"2":160,"3":150,"4":140,"5":130}',10000000,10000000,10,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2016-08-18 09:45:09','2016-09-07 15:00:42'),
	(16,56,53,'dealer1-10','dealer2-10',1,3,64,NULL,120,100,'{"1":"0","2":"0","3":"0","4":"0","5":"0"}',700000,700000,10,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2016-08-18 09:49:30','2016-09-07 16:33:37');



INSERT INTO
  parana_users
  (id, `name`, `email`, `mobile`, `password`, `type`, `status`, `roles_json`, `extra_json`, `tags_json`, `created_at`, `updated_at`)
VALUES
  (1, 'jlchen','i@terminus.io', '18888888888', '9f8c@a97758b955efdaf60fe4', 2, 1, null, '{"seller":"haha"}', '{"good":"man"}', now(), now()),
  (42,'supplier1',NULL,NULL,'e4b9@9aa7f95f663caac23e25',2,1,'["BUYER","SUPPLIER","SELLER"]',NULL,NULL,'2016-08-15 09:20:19','2016-09-06 09:40:44'),
  (63,'dealer1-10',NULL,NULL,'d99a@f11b0ebd4c83676dc3c4',2,1,'["BUYER","SELLER","DEALER_FIRST"]',NULL,NULL,'2016-08-18 09:45:09','2016-09-07 15:44:24'),
	(64,'dealer2-10',NULL,NULL,'0928@defd864c8683b559ad2d',2,1,'["DEALER_SECOND","BUYER","SELLER"]',NULL,NULL,'2016-08-18 09:49:30','2016-09-07 15:41:22');



-- init item and sku
INSERT INTO `parana_items` (`category_id`, `shop_id`, `shop_name`, `brand_id`, `brand_name`, `name`, `main_image`, `low_price`,
`high_price`, `stock_type`, `stock_quantity`, `sale_quantity`, `status`, `on_shelf_at`, `reduce_stock_type`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(1, 1, 'Zcy Test Shop', 1, 'nike', 'A Test Item', 'http://zcy-test.img-cn-hangzhou.aliyuncs.com/users/2/20160114110114277.jpg', 18000,
	 19000, 0, 198, 0, 1, now(), 1, '{"unit":"件"}', now(), now());

INSERT INTO `parana_skus` (id, `item_id`, `shop_id`, `status`, `specification`, `outer_sku_id`, `outer_shop_id`, `image`, `name`,
`extra_price_json`, `price`, `attrs_json`, `stock_type`, `stock_quantity`, `extra`, `created_at`, `updated_at`, `thumbnail`)
VALUES
	(1, 1, 1, 1, NULL, NULL, NULL, NULL, NULL,
	'{"origin_price":"20000"}', 18000, '[{"attrKey": "长度", "attrVal": "18cm"}]', 0, 99, NULL, now(), now(), NULL),
	(2, 1, 1, 1, NULL, NULL, NULL, NULL, NULL,
	'{"origin_price":"22000"}', 20000, '[{"attrKey": "长度", "attrVal": "19cm"}]', 0, 99, NULL, now(), now(), NULL),
	(3, 1, 1, -1, NULL, NULL, NULL, NULL, NULL,
	'{"origin_price":"25000"}', 25000, '[{"attrKey": "长度", "attrVal": "20cm"}]', 0, 99, NULL, now(), now(), NULL);


INSERT INTO `parana_refunds` (id, `fee`, `shop_id`, `shop_name`,`buyer_id`, `buyer_name`, `out_id`, `integral`, `balance`, `status`,
`refund_serial_no`, `payment_id`, `pay_serial_no`, `refund_account_no`, `channel`, `promotion_id`, `buyer_note`, `seller_note`,
`extra_json`, `tags_json`, `refund_at`, `created_at`, `updated_at`)
VALUES
  (1, 100, 1,'shop1', 1, 'buyer', NULL , NULL , NULL , -3, NULL , NULL , NULL , NULL , NULL , NULL , 'buyer note', 'seller note',
  '', '', now(), now(), now()),
   (2, 100, 1,'shop1', 1, 'buyer', NULL , NULL , NULL , -3, NULL , NULL , NULL , NULL , NULL , NULL , 'buyer note', 'seller note',
  '', '', now(), now(), now());

INSERT INTO `parana_order_refunds` (`id`, `refund_id`, `order_id`, `order_type`, `status`, `created_at`, `updated_at`)
VALUES
  (1, 1, 1, 2, 0, now(), now()),
  (2, 2, 1, 2, 0, now(), now());

INSERT INTO `parana_sku_orders` (`id`, `sku_id`,`quantity`,`fee`,`status`, `order_id`,`buyer_id`,`out_id`,`buyer_name`,`out_buyer_id`,`item_id`,
        `item_name`,`sku_image`,`shop_id`,`shop_name`,`out_shop_id`,`company_id`,`out_sku_id`,
        `sku_attributes`,`channel`,`pay_type`,`shipment_type`,`origin_fee`,`discount`,
        `ship_fee`,`ship_fee_discount`,`integral`,`balance`,`promotion_id`,`item_snapshot_id`,`has_refund`,`invoiced`,
        `extra_json`,`tags_json`,`created_at`,`updated_at`)
VALUES
  (1, 1, 1, 100, 1, 1, 1, NULL , 'buyer', NULL , 1, 'item', NULL , 1, 'shop', NULL , NULL ,NULL , NULL ,
  1, NULL , NULL , 200, 100, 0, 0, NULL , NULL , NULL , NULL , 0, NULL , NULL , NULL , now(), now()),
  (2, 1, 1, 100, 0, 2, 1, NULL , 'buyer', NULL , 1, 'item', NULL , 1, 'shop', NULL , NULL ,NULL , NULL ,
  1, NULL , NULL , 200, 100, 0, 0, NULL , NULL , NULL , NULL , 0, NULL , NULL , NULL , now(), now()),
  (3, 1, 1, 100, 0, 3, 1, NULL , 'buyer', NULL , 1, 'item', NULL , 1, 'shop', NULL , NULL ,NULL , NULL ,
  1, NULL , NULL , 200, 100, 0, 0, NULL , NULL , NULL , NULL , 0, NULL , NULL , NULL , now(), now());

INSERT INTO `parana_payments` (`id`, `fee`,`out_id`,`origin_fee`,`discount`,`integral`,`balance`,`status`,`pay_serial_no`,`pay_account_no`,
        `channel`,`promotion_id`, `extra_json`,`tags_json`,`paid_at`,`created_at`,`updated_at`)
VALUES
  (1, 100, NULL , 100, 0, NULL , NULL , 1, NULL ,NULL , 'alipay', NULL , NULL , NULL , now(), now(), now());


INSERT INTO `parana_order_payments` (`id`, `payment_id`,`order_id`,`order_type`,`status`,`created_at`,`updated_at`)
VALUES
  (1, 1, 1, 2, 1, now(), now());

INSERT INTO `parana_express_companies` (`id`, `code`, `name`, `status`, `created_at`, `updated_at`)
VALUES
	(1, 'shunfeng', '顺丰速运', 1, now(), now());

INSERT INTO `parana_shop_orders` (`id`, `shop_id`, `buyer_id`, `fee`, `status`, `buyer_name`, `out_buyer_id`, `shop_name`, `out_shop_id`, `company_id`, `origin_fee`, `discount`, `ship_fee`, `integral`, `balance`, `promotion_id`, `shipment_type`, `pay_type`, `channel`, `has_refund`, `buyer_note`, `extra_json`, `tags_json`, `out_id`, `out_from`, `created_at`, `updated_at`)
VALUES
	(1, 1, 1, 0, 1, 'buyer', NULL, '', NULL, NULL, NULL, NULL, 0,NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, now(), now()),
  (2, 1, 1, 0, 0, 'buyer', NULL, '', NULL, NULL, NULL, NULL, 0,NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, now(), now()),
  (3, 1, 1, 0, 0, 'buyer', NULL, '', NULL, NULL, NULL, NULL, 0,NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, now(), now());

INSERT INTO `parana_order_receiver_infos` (`id`, `order_id`, `order_type`, `receiver_info_json`, `created_at`, `updated_at`)
VALUES
	(1, 1, 1, '{"userId":2,"mobile":"18999999999"}', now(), now());

INSERT INTO `parana_delivery_fee_templates` (`id`, `shop_id`, `name`, `is_free`, `deliver_method`, `charge_method`, `fee`, `init_amount`, `init_fee`, `incr_amount`, `incr_fee`, `is_default`, `created_at`, `updated_at`)
VALUES
	(1, 1, '测试模板', 0, 0, 0, NULL, 1, 5, 2, 2, 0, now(), now());

INSERT INTO `parana_item_delivery_fees` (`id`, `item_id`, `delivery_fee`, `delivery_fee_template_id`, `created_at`, `updated_at`)
VALUES
	(1, 1, NULL, 1, now(),now());

INSERT INTO `parana_item_delivery_fees` (`id`, `item_id`, `delivery_fee`, `delivery_fee_template_id`, `created_at`, `updated_at`)
VALUES
	(2, 2, NULL, 1, now(),now());



INSERT INTO `vega_purchase_orders` (`id`, `name`, `buyer_id`, `buyer_name`, `sku_quantity`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(1, '我的采购单', 1, 'buyer', 0, NULL, '2016-09-02 11:34:03', '2016-09-02 11:34:03');

INSERT INTO `parana_shop_orders` (`id`, `shop_id`, `buyer_id`, `fee`, `status`, `buyer_name`, `out_buyer_id`, `shop_name`, `out_shop_id`, `company_id`, `origin_fee`, `discount`, `ship_fee`, `origin_ship_fee`, `shipment_promotion_id`, `integral`, `balance`, `promotion_id`, `shipment_type`, `pay_type`, `channel`, `has_refund`, `buyer_note`, `extra_json`, `tags_json`, `out_id`, `out_from`, `created_at`, `updated_at`, `commission_rate`)
VALUES
	(5, 0, 4, 462300, 0, 'ceshi', NULL, '平台店铺', '', NULL, 462300, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL, '', NULL, '{"platformShopName":"平台店铺","roleName":"BUYER"}', NULL, NULL, '2016-09-02 17:16:04', '2016-09-02 17:16:04', NULL);
INSERT INTO `parana_sku_orders` (`id`, `sku_id`, `quantity`, `fee`, `status`, `order_id`, `buyer_id`, `out_id`, `buyer_name`, `out_buyer_id`, `item_id`, `item_name`, `sku_image`, `shop_id`, `shop_name`, `out_shop_id`, `company_id`, `out_sku_id`, `sku_attributes`, `channel`, `pay_type`, `shipment_type`, `origin_fee`, `discount`, `ship_fee`, `ship_fee_discount`, `integral`, `balance`, `promotion_id`, `item_snapshot_id`, `has_refund`, `invoiced`, `commented`, `extra_json`, `tags_json`, `created_at`, `updated_at`)
VALUES
	(442, 5863, 23, 462300, 0, 5, 4, NULL, 'ceshi', NULL, 5070, '芳纶带3L32', '//sanlux-dev.oss-cn-hangzhou.aliyuncs.com/2016/08/31/a472025e-06bb-46b3-a176-212ea439a657.png', 0, '平台店铺', NULL, NULL, NULL, NULL, 1, 1, NULL, 462300, NULL, NULL, NULL, NULL, NULL, NULL, 111, 0, 0, NULL, NULL, '{"platformShopName":"平台店铺","roleName":"BUYER"}', '2016-09-02 17:16:04', '2016-09-02 17:16:04');




INSERT INTO `vega_ranks` (`id`, `pid`, `name`, `integration_start`, `integration_end`, `discount`, `extra_json`, `created_at`, `updated_at`)
VALUES
	(1, 0, '普通会员', 0, 1000, NULL, NULL, '2016-08-12 18:16:35', '2016-09-07 11:07:04'),
	(2, 1, '铜牌会员', 1001, 5000, NULL, NULL, '2016-08-12 18:16:35', '2016-09-07 11:07:04'),
	(3, 2, '银牌会员', 5001, 10000, NULL, NULL, '2016-08-12 18:16:35', '2016-09-07 11:07:04'),
	(4, 3, '金牌会员', 10001, 20000, NULL, NULL, '2016-08-12 18:16:35', '2016-09-07 11:07:04'),
	(5, 4, '钻石会员', 20001, 100023, NULL, NULL, '2016-08-12 18:16:35', '2016-09-07 11:07:04');


INSERT INTO `vega_category_authes` (`id`, `shop_id`, `shop_name`, `discount_lower_limit`, `category_auth_list`, `category_discount_list`, `created_at`, `updated_at`)
VALUES
	(5, 69, '', 100, '[{\"categoryId\":2352,\"categoryName\":\"半成品\"},{\"categoryId\":2353,\"categoryName\":\"原料\"}]', '[{\"categoryId\":2352,\"categoryName\":\"半成品\",\"categoryPid\":0,\"categoryLevel\":1,\"children\":[{\"categoryId\":2354,\"categoryName\":\"帆布\",\"categoryPid\":2352,\"categoryLevel\":2,\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]},{\"categoryId\":2355,\"categoryName\":\"底胶\",\"categoryPid\":2352,\"categoryLevel\":2,\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]}],\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]},{\"categoryId\":2353,\"categoryName\":\"原料\",\"categoryPid\":0,\"categoryLevel\":1,\"children\":[{\"categoryId\":2356,\"categoryName\":\"氨水\",\"categoryPid\":2353,\"categoryLevel\":2,\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]},{\"categoryId\":2357,\"categoryName\":\"甲醛\",\"categoryPid\":2353,\"categoryLevel\":2,\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]},{\"categoryId\":2358,\"categoryName\":\"五金\",\"categoryPid\":2353,\"categoryLevel\":2,\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]}],\"categoryMemberDiscount\":[{\"memberLevelId\":1,\"memberLevelName\":\"普通会员\",\"discount\":0},{\"memberLevelId\":2,\"memberLevelName\":\"铜牌会员\",\"discount\":0},{\"memberLevelId\":3,\"memberLevelName\":\"银牌会员\",\"discount\":0},{\"memberLevelId\":4,\"memberLevelName\":\"金牌会员\",\"discount\":0},{\"memberLevelId\":5,\"memberLevelName\":\"钻石会员\",\"discount\":0}]}]', '2016-08-20 13:00:40', '2016-09-08 11:15:44');
