/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.item;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Author:cp
 * Created on 8/2/16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DaoConfiguration.class)
@Transactional
@Rollback
@ActiveProfiles("test")
public abstract class BaseDaoTest {
}
