package com.sanlux.category.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.category.impl.dao.FrontCategoryExtDao;
import com.sanlux.category.service.FrontCategoryExtReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * Created by jesselu on 2017/2/10.
 */
@Slf4j
@Service
@RpcProvider
public class FrontCategoryExtReadServiceImp implements FrontCategoryExtReadService {
    private final FrontCategoryExtDao frontCategoryExtDao;

    @Autowired
    public FrontCategoryExtReadServiceImp(FrontCategoryExtDao frontCategoryExtDao) {
        this.frontCategoryExtDao = frontCategoryExtDao;
    }

    @Override
    public Response<List<FrontCategory>> findCategoryByIdAndHasChildren (Long id){
        try {
            List<FrontCategory> frontCategories = frontCategoryExtDao.findCategoryByIdAndHasChildren(id);
            return Response.ok(frontCategories);
        }catch (Exception e) {
            log.error("find category info by id:{} failed, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("find.category.info.failed");
        }
    }

    @Override
    public Response<Long> checkCategoryByIdAndLevelAndHasChildren (Map<String, Object> criteria){
        try {
            return Response.ok(frontCategoryExtDao.checkCategoryByIdAndLevelAndHasChildren(criteria));
        }catch (Exception e) {
            log.error("find category info by criteria:{} failed, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("find.category.info.failed");
        }
    }
}
