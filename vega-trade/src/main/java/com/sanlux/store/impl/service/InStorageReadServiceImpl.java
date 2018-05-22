package com.sanlux.store.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.store.service.InStorageReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.mapper.entity.Example;
import io.terminus.parana.store.impl.dao.SonEntryGodownMapper;
import io.terminus.parana.store.model.SonEntryGodown;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Created by lujm on 2017/3/1.
 */
@Service
@RpcProvider
@Slf4j
public class InStorageReadServiceImpl implements InStorageReadService {

    private final SonEntryGodownMapper sonEntryGodownMapper;

    @Autowired
    public InStorageReadServiceImpl(SonEntryGodownMapper sonEntryGodownMapper){
        this.sonEntryGodownMapper = sonEntryGodownMapper;
    }

    @Override
    public Response<Paging<SonEntryGodown>> pagingByEntryGodownId(Long entryGodownId,Integer pageNo, Integer pageSize) {
        try{
            SonEntryGodown sonEntryGodown = new SonEntryGodown();
            sonEntryGodown.setEntryGodownId(entryGodownId);
            Example example = new Example(SonEntryGodown.class);
            example.createCriteria();
            example.getOredCriteria().get(0).andEqualTo(sonEntryGodown);
            Paging<SonEntryGodown> paging = sonEntryGodownMapper.pagingByExample(example, pageNo, pageSize).getPaging();
            return Response.ok(paging);
        }catch (Exception e){
            log.error("paging sonEntryGodown failed by entryGodownId={},pageNo={}, size={}, cause:{}", entryGodownId,pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("find.sonEntryGodown.fail");
        }
    }
}
