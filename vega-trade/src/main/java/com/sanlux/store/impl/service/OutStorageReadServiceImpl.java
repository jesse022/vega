package com.sanlux.store.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.store.service.OutStorageReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.mapper.entity.Example;
import io.terminus.parana.store.impl.dao.SonLeaveGodownMapper;
import io.terminus.parana.store.model.SonLeaveGodown;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2017/3/1.
 */
@Service
@RpcProvider
@Slf4j
public class OutStorageReadServiceImpl implements OutStorageReadService {
    private final SonLeaveGodownMapper sonLeaveGodownMapper;

    @Autowired
    public OutStorageReadServiceImpl(SonLeaveGodownMapper sonLeaveGodownMapper){
        this.sonLeaveGodownMapper = sonLeaveGodownMapper;
    }

    @Override
    public Response<Paging<SonLeaveGodown>> pagingByLeaveGodownId(Long leaveGodownId, Integer pageNo, Integer pageSize) {
        try{
            SonLeaveGodown sonLeaveGodown = new SonLeaveGodown();
            sonLeaveGodown.setLeaveGodownId(leaveGodownId);
            Example example = new Example(SonLeaveGodown.class);
            example.createCriteria();
            example.getOredCriteria().get(0).andEqualTo(sonLeaveGodown);
            Paging<SonLeaveGodown> paging = sonLeaveGodownMapper.pagingByExample(example, pageNo, pageSize).getPaging();
            return Response.ok(paging);
        }catch (Exception e){
            log.error("paging sonEntryGodown failed by entryGodownId={},pageNo={}, size={}, cause:{}", leaveGodownId,pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("find.sonEntryGodown.fail");
        }
    }

    @Override
    public Response<Paging<SonLeaveGodown>> pagingBySonLeaveGodown(SonLeaveGodown sonLeaveGodown, Integer pageNo, Integer pageSize) {
        try{
            Example example = new Example(SonLeaveGodown.class);
            example.createCriteria();
            example.getOredCriteria().get(0).andEqualTo(sonLeaveGodown);
            Paging<SonLeaveGodown> paging = sonLeaveGodownMapper.pagingByExample(example, pageNo, pageSize).getPaging();
            return Response.ok(paging);
        }catch (Exception e){
            log.error("paging sonEntryGodown failed by sonLeaveGodown={},pageNo={}, size={}, cause:{}", sonLeaveGodown,pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("find.sonEntryGodown.fail");
        }
    }
}
