package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.cache.VegaFrontCategoriesCacher;
import com.sanlux.item.service.VegaFrontCategoriesCacherService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by syf on 2017/6/29.
 */

@Service
@RpcProvider
@Slf4j
public class VegaFrontCategoriesCacherServiceImpl implements VegaFrontCategoriesCacherService{

    private final VegaFrontCategoriesCacher vegaFrontCategoriesCacher;

    @Autowired
    public VegaFrontCategoriesCacherServiceImpl(VegaFrontCategoriesCacher vegaFrontCategoriesCacher) {
        this.vegaFrontCategoriesCacher = vegaFrontCategoriesCacher;
    }


    @Override
    public Response<List<FrontCategory>> findAncestorsFromCatch(Long id) {
        try {
            List<FrontCategory> categories = vegaFrontCategoriesCacher.findByCategoryId(id);
            if (categories == null) {
                log.error("find categoryIds by id:{} empty",id);
                return Response.fail("front.category.find.empty");
            }
            return Response.ok(categories);
        }catch (Exception e) {
            log.error("fail to find category ids by id:{},cause:{}",  id, Throwables.getStackTraceAsString(e));
            return Response.fail("front.category.find.fail");
        }
    }
}
