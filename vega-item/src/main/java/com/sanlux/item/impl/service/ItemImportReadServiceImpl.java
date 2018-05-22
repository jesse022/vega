package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.ItemImportDao;
import com.sanlux.item.model.ItemImport;
import com.sanlux.item.service.ItemImportReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lujm on 2018/3/20.
 */
@Slf4j
@Service
@RpcProvider
public class ItemImportReadServiceImpl implements ItemImportReadService {

    private final ItemImportDao itemImportDao;

    @Autowired
    public ItemImportReadServiceImpl(ItemImportDao itemImportDao) {
        this.itemImportDao = itemImportDao;
    }

    @Override
    public Response<ItemImport> findById(Long id) {
        try {
            return Response.ok(itemImportDao.findById(id));
        } catch (Exception e) {
            log.error("find item import by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.import.find.fail");
        }
    }
}
