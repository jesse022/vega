package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.cache.VegaCategoryByItemIdCacher;
import com.sanlux.item.service.VegaCategoryByItemIdCacherService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by cuiwentao
 * on 16/8/19
 */
@Service
@RpcProvider
@Slf4j
public class VegaCategoryByItemIdCacherServiceImpl implements VegaCategoryByItemIdCacherService {

    private final VegaCategoryByItemIdCacher vegaCategoryByItemIdCacher;

    @Autowired
    public VegaCategoryByItemIdCacherServiceImpl(VegaCategoryByItemIdCacher vegaCategoryByItemIdCacher) {
        this.vegaCategoryByItemIdCacher = vegaCategoryByItemIdCacher;
    }

    @Override
    public Response<Long> findByItemId(Long itemId) {
        try {
            Long categoryId = vegaCategoryByItemIdCacher.findByItemId(itemId);

            if (categoryId == null) {
                log.error("find categoryIds by itemId:{} empty",itemId);
                return Response.fail("back.category.find.empty");

            }
            return Response.ok(categoryId);

        }catch (Exception e) {
            log.error("fail to find category ids by itemId:{},cause:{}",  itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("back.category.find.fail");
        }
    }
}
