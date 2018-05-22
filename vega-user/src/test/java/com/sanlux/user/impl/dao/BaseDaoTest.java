/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.user.impl.dao;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * 交易模块测试
 *
 * Author  : panxin
 * Date    : 3:30 PM 3/3/16
 * Mail    : panxin@terminus.io
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DaoConfiguration.class)
@Transactional
@Rollback
@ActiveProfiles("test")
public abstract class BaseDaoTest {
}
