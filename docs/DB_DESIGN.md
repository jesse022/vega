# 数据库设计规范


## 表命名

* 除了shard的表, 业务表名中只允许 **字母** 加上 **下划线** 的组合
* 命名采用 [工程名]_[模块名]_[说明] 的方式, 尾数采用单词的复数形式
        
        # 用户地址信息
        galaxy_user_addresses
        
        # 商品后台类目信息
        galaxy_item_back_categories
        
        


## 字段

* 对于每个库表，我们通常会有 created_at、 updated_at 字段用来记录数据记录变化的时间
* 新增的字段请放置在合适的位置, 原则上新增的字段都应置于created_at之前, 例如


        CREATE TABLE IF NOT EXISTS `galaxy_user_addresses` (
          `id`          BIGINT    NOT NULL  AUTO_INCREMENT COMMENT '自增主键',
          
          .......
          
          `created_at`  DATETIME  NULL      COMMENT '创建时间',
          `updated_at`  DATETIME  NULL      COMMENT '修改时间',
          PRIMARY KEY (`id`));

* 字符串类型长度统一采用2的指数形式,如 16/32/64 ...
* 如



## 字段长度参考附录


| 字段说明 | 建议命名 |  建议长度 |  备注 |
| ----- | --------| ------| ------|
| 用户名 | name    |  64   | 用户登录的账户  |
| 邮箱 | email | 512 | |
| 手机 | mobile | 32 ||
| 密码 | passwd | 32 ||



