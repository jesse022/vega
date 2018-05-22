package com.sanlux.user.impl.dao;

import com.sanlux.user.model.Nation;
import com.sanlux.user.model.Rank;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Created by lujm on 2017/2/24.
 */
public class NationDaoTest extends BaseDaoTest {
    @Autowired
    private NationDao nationDao;

    private Nation nation;

    @Before
    public void init() {
        nation = makeNation();
        nationDao.create(nation);
        assertNotNull(nation.getId());
    }

    @Test
    public void findById() {
        Nation nationExist = nationDao.findById(nation.getId());
        assertNotNull(nationExist);
    }


    @Test
    public void findByCode() {
        Nation nation = nationDao.findByCode("330100");
        assertNotNull(nation);
        assertTrue(nation.getCode().equals("330100"));
    }



    private Nation makeNation() {
        Nation nation = new Nation();
        nation.setCode("330100");
        nation.setProvince("浙江");
        nation.setCity("杭州");
        nation.setDistrict("");
        nation.setParent("1001");
        nation.setStaffId("61145");
        nation.setGroupId("88888");
        return nation;
    }

}
