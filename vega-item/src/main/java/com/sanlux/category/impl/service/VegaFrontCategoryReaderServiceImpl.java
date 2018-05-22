package com.sanlux.category.impl.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sanlux.category.service.VegaFrontCategoryReaderService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.category.impl.dao.FrontCategoryDao;
import io.terminus.parana.category.impl.service.BackCategoryReadServiceImpl;
import io.terminus.parana.category.model.FrontCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Created by syf on 2017/6/28.
 */
@Service
@RpcProvider
public class VegaFrontCategoryReaderServiceImpl implements VegaFrontCategoryReaderService{


    private static final Logger log = LoggerFactory.getLogger(BackCategoryReadServiceImpl.class);

    @Autowired
    private FrontCategoryDao frontCategoryDao;


    @Override
    public Response<List<FrontCategory>> findAncestorsOf(Long id) {
        Long categoryId = MoreObjects.firstNonNull(id, Long.valueOf(0L));
        if (categoryId.longValue() == 0L) {
            return Response.ok(Collections.emptyList());
        } else {
            List<FrontCategory> ancestors = Lists.newArrayList();
            Long currentId = categoryId;

            try {
                while (currentId.longValue() > 0L) {
                    FrontCategory current = this.frontCategoryDao.findById(currentId);
                    if (current == null) {
                        log.error("no back category(categoryId={}) found", currentId);
                        return Response.fail("backCategory.not.found");
                    }

                    ancestors.add(current);
                    currentId = current.getPid();
                }

                return Response.ok(Lists.reverse(ancestors));
            } catch (Exception var6) {
                log.error("failed to find ancestors of front category(id={}), cause:{}", categoryId, Throwables.getStackTraceAsString(var6));
                return Response.fail("category.find.fail");
            }
        }
    }


}
