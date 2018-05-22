package com.sanlux.category.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sanlux.category.impl.dao.CategoryAutheDao;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.CategoryAutheReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 经销商授权类目读服务实现类
 * Date: 2016-08-04
 */
@Slf4j
@Service
@RpcProvider
public class CategoryAutheReadServiceImpl implements CategoryAutheReadService {

    private final CategoryAutheDao categoryAutheDao;

    @Autowired
    public CategoryAutheReadServiceImpl(CategoryAutheDao categoryAutheDao) {
        this.categoryAutheDao = categoryAutheDao;
    }

    @Override
    public Response<Optional<CategoryAuthe>> findCategoryAutheById(Long categoryAutheId) {
        try {
            CategoryAuthe categoryAuthe = categoryAutheDao.findById(categoryAutheId);
            return Response.ok(Optional.fromNullable(categoryAuthe));
        } catch (Exception e) {
            log.error("find categoryAuthe by id:{} failed, cause:{}",
                    categoryAutheId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.authe.find.fail");
        }
    }

    @Override
    public Response<Optional<CategoryAuthe>> findCategoryAutheByShopId (Long shopId) {
        try {

            CategoryAuthe categoryAuthe = categoryAutheDao.findByShopId(shopId).orNull();
            return Response.ok(Optional.fromNullable(categoryAuthe));
        } catch (Exception e) {
            log.error("find categoryAuth by shopId:{} failed, cause:{}", shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("category.auth.find.fail");
        }
    }

    @Override
    public Response<List<CategoryAuthe>> findByShopIds (List<Long> shopIds) {
        try {
            List<CategoryAuthe> categoryAuthes = categoryAutheDao.findByShopIds(shopIds);
            return Response.ok(categoryAuthes);
        }catch (Exception e) {
            log.error("find categoryAuth by shopIds:{} failed, cause:{}", shopIds, Throwables.getStackTraceAsString(e));
            return Response.fail("category.auth.find.fail");
        }
    }



}