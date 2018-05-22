package com.sanlux.category.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.category.impl.dao.FrontCategoryExtDao;
import com.sanlux.category.service.FrontCategoryExtWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.category.impl.dao.FrontCategoryDao;
import io.terminus.parana.category.model.FrontCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;


/**
 * Created by jesselu on 2017/2/12.
 */
@Slf4j
@Service
@RpcProvider
public class FrontCategoryExtWriteServiceImp implements FrontCategoryExtWriteService {
    private final FrontCategoryExtDao frontCategoryExtDao;
    private final FrontCategoryDao frontCategoryDao;

    @Autowired
    public FrontCategoryExtWriteServiceImp(FrontCategoryExtDao frontCategoryExtDao,FrontCategoryDao frontCategoryDao) {
        this.frontCategoryExtDao = frontCategoryExtDao;
        this.frontCategoryDao = frontCategoryDao;
    }

    @Override
    public Response<Boolean> updatePidAndLevel (Long pid, Integer level, Long id){
        try {
            FrontCategory frontCategory = frontCategoryExtDao.findById(id);
            Boolean isSuccess = frontCategoryExtDao.updatePidAndLevel(pid,level,id);
            if(isSuccess){
                List<FrontCategory> siblings = frontCategoryDao.findChildren(frontCategory.getPid());
                if(CollectionUtils.isEmpty(siblings)) {
                    frontCategoryDao.updateHasChildren(frontCategory.getPid(), Boolean.FALSE);
                }
            }
            return Response.ok(isSuccess);
        }catch (Exception e) {
            log.error("update category info by id:{}  level:{} pid:{} failed, cause:{}", id,level,pid,Throwables.getStackTraceAsString(e));
            return Response.fail("update.category.info.failed");
        }
    }

}
