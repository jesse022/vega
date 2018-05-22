package com.sanlux.store.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.sanlux.store.service.VegaLocationReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.mapper.entity.Example;
import io.terminus.parana.store.impl.dao.LocationMapper;
import io.terminus.parana.store.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lujm on 2017/3/6.
 */
@Service
@RpcProvider
@Slf4j
public class VegaLocationReadServiceImpl implements VegaLocationReadService {
    private final LocationMapper locationMapper;
    @Autowired
    public VegaLocationReadServiceImpl(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    /**
     * 根据repertoryId,skuId获取满足分配出库的库位库存信息
     * @param repertoryId 仓储ID
     * @param skuId skuID
     * @return 库位库存信息
     */
    @Override
    public Response<List<Location>> findByRepertoryAndSku(Long repertoryId, Long skuId) {
        try{
            Example example = new Example(Location.class);
            example.createCriteria().andEqualTo("repertoryId", repertoryId).andEqualTo("skuId",skuId);
            List<Location> resultList =  locationMapper.selectByExample(example);
            List<Location> iterable = FluentIterable.from(resultList).filter(location -> location.getStatus() == 1 &&
                    location.getNowContent() - location.getPreOut() > 0).toList();
            return Response.ok(iterable);
        } catch (Exception e){
            log.error("locations find by skuId={} repertoryId={} failed, cause:{}", skuId,repertoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("locations.find.by.sku.failed");
        }
    }

    /**
     * 根据categoryIds,skuId获取满足分配出库的库位库存信息
     * @param categoryIds 类目IDs
     * @param skuIds skuIDs
     * @return 库位库存信息
     */
    @Override
    public Response<List<Location>> findByCategoryAndSku(List<Long> categoryIds, List<Long> skuIds,Long repertoryId) {
        try{
            Example example = new Example(Location.class);
            example.createCriteria().andIn("categoryId", categoryIds).andIn("skuId",skuIds).andEqualTo("repertoryId",repertoryId);
            List<Location> resultList =  locationMapper.selectByExample(example);
            List<Location> iterable = FluentIterable.from(resultList).filter(location -> location.getStatus() == 1 &&
                    location.getNowContent() + location.getPreIn() < location.getMaxContent()).toList();
            return Response.ok(iterable);
        } catch (Exception e){
            log.error("locations find by skuIds={} categoryIds={} failed, cause:{}", skuIds,categoryIds, Throwables.getStackTraceAsString(e));
            return Response.fail("locations.find.by.sku.failed");
        }
    }

    @Override
    public Response<List<Location>> findByCategoryAndSkuIsNull(Long repertoryId) {
        try{
            Example example = new Example(Location.class);
            example.createCriteria().andEqualTo("repertoryId",repertoryId).andIsNull("categoryId").andIsNull("skuId");
            List<Location> resultList =  locationMapper.selectByExample(example);
            List<Location> iterable = FluentIterable.from(resultList).filter(location -> location.getStatus() == 1 &&
                    location.getNowContent() + location.getPreIn() < location.getMaxContent()).toList();
            return Response.ok(iterable);
        } catch (Exception e){
            log.error("locations find by skuIds={} categoryIds={} failed, cause:{}", null,null, Throwables.getStackTraceAsString(e));
            return Response.fail("locations.find.by.sku.failed");
        }
    }
}
