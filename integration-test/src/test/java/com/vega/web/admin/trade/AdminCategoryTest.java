package com.vega.web.admin.trade;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.category.dto.VegaCategoryAuthDto;
import com.vega.web.BaseWebTest;
import com.vega.web.configuration.admin.AdminWebConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by cuiwentao
 * on 16/9/8
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AdminWebConfiguration.class)
public class AdminCategoryTest extends BaseWebTest {

    @Test
    public void testCreateAuth() {
        List<VegaCategoryAuthDto> categorys = Lists.newArrayList();
        VegaCategoryAuthDto category1 = new VegaCategoryAuthDto();
        category1.setCategoryId(2352L);
        category1.setCategoryName("半成品");
        categorys.add(category1);
        VegaCategoryAuthDto category2 = new VegaCategoryAuthDto();
        category2.setCategoryId(2353L);
        category2.setCategoryName("原料");
        categorys.add(category2);
        Map<String, Object> form = Maps.newHashMap();
        form.put("shopId", 69L);
        form.put("shopName","");
        form.put("id", 5L);
        form.put("authList",categorys);
        Long categoryId = postFormForObject("/api/admin/category/auth", form, Long.class);
        assertTrue(categoryId > 0);
    }
}
