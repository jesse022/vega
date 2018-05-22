package com.sanlux.web.front.controller.user;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.user.dto.scope.DeliveryScopeCityDto;
import com.sanlux.user.dto.scope.DeliveryScopeDto;
import com.sanlux.user.dto.scope.DeliveryScopeRegionDto;
import com.sanlux.user.model.DeliveryScope;
import com.sanlux.user.service.DeliveryScopeReadService;
import com.sanlux.user.service.DeliveryScopeWriteService;
import com.sanlux.web.front.core.events.DeliveryScopeUpdateEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.user.address.model.Address;
import io.terminus.parana.user.address.service.AddressReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 一级经销商设置二级经销商供货区域Control
 * Created by lujm on 2017/3/31.
 */
@RestController
@Slf4j
@RequestMapping("/api/delivery/scope")
public class VegaDeliveryScope {

    @RpcConsumer
    private DeliveryScopeReadService deliveryScopeReadService;

    @RpcConsumer
    private DeliveryScopeWriteService deliveryScopeWriteService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private AddressReadService addressReadService;

    @Autowired
    private EventBus eventBus;

    /**
     * 获取二级经销商供货区域信息
     *
     * @param shopId shopId
     * @return 供货区域信息
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public DeliveryScope readScopeByShopId(@RequestParam Long shopId) {
        Response<Optional<DeliveryScope>> readScopeResp = deliveryScopeReadService.findDeliveryScopeByShopId(shopId);
        if (!readScopeResp.isSuccess()) {
            log.error("read scope fail, shopId:{}, cause:{}", shopId, readScopeResp.getError());
            return null;
        } else {
            if (readScopeResp.getResult().isPresent()) {
                return readScopeResp.getResult().get();
            } else {
                log.debug("delivery scope is empty, shopId:{}", shopId);
                return null;
            }
        }
    }

    /**
     * 二级经销商店铺供货区域设置
     *
     * @return 配送范围表ID
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Long writeScope(@RequestBody DeliveryScope scope) {

        Long shopId = scope.getShopId();

        if (scope.getId() != null) {
            eventBus.post(DeliveryScopeUpdateEvent.from(shopId));
        }
        Response<VegaShop> vegaShopResponse = vegaShopReadService.findByShopId(shopId);
        if (!vegaShopResponse.isSuccess()) {
            log.error("write scope fail, shopId:{}, cause:{}", shopId, vegaShopResponse.getError());
            throw new JsonResponseException(vegaShopResponse.getError());
        }
        scope.setShopName(vegaShopResponse.getResult().getShop().getName());
        scope.setPId(vegaShopResponse.getResult().getShopExtra().getShopPid());
        Response<Long> resp = deliveryScopeWriteService.createOrUpdateDeliveryScope(scope);
        if (!resp.isSuccess()) {
            log.error("write scope fail, shopId:{}, cause:{}", scope.getShopId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 改写获取地址接口,这里只用于一级经销商设置二级经销商供货区域使用
     * 原先接口地址:/api/address/{id}/children
     *
     * @param addressId 上级地址ID
     * @param shopId    二级经销商ID
     * @return 地址List
     */
    @RequestMapping(value = "/address/{id}/children", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Address> getChildAddressControl(@PathVariable("id") Integer addressId, @RequestParam Long shopId) {
        //根据上级ID查询供货区域地址信息
        Response<List<Address>> response = addressReadService.childAddressOf(addressId);
        if (!response.isSuccess()) {
            log.error("fail to get child address by address id {}, error code:{}", addressId, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<Address> addresses = response.getResult();
        Integer level = 1;
        if (addresses.size() > 0) {
            level = addresses.get(0).getLevel();// 1:省级 2:市级 3:区县级
        } else {
            return Collections.emptyList();
        }

        //根据一级经销商的供货区域进行过滤
        Response<Optional<VegaShop>> shopResp = vegaShopReadService.finParentShopById(shopId);
        if (!shopResp.isSuccess()) {
            log.error("failed to find shop by id = {}, cause : {}", shopId, shopResp.getError());
            return Collections.emptyList();
        } else {
            Response<Optional<DeliveryScope>> ScopeResp = deliveryScopeReadService.findDeliveryScopeByShopId(shopResp.getResult().get().getShop().getId());
            if (ScopeResp.isSuccess()) {
                if (ScopeResp.getResult().isPresent()) {
                    List<Integer> existAddressIds = Lists.newArrayList();
                    List<DeliveryScopeDto> deliveryScopeDtos = ScopeResp.getResult().get().getScopeJson();
                    if (!Objects.isNull(deliveryScopeDtos)) {
                        for (DeliveryScopeDto deliveryScopeDto : deliveryScopeDtos) {
                            if (level == 1) {
                                //省级单位过滤
                                existAddressIds.add(deliveryScopeDto.getProvinceId());
                            }
                            if (level == 2 || level == 3) {
                                List<DeliveryScopeCityDto> deliveryScopeCityDtos = deliveryScopeDto.getCitiesMap();
                                if (!Objects.isNull(deliveryScopeCityDtos)) {
                                    for (DeliveryScopeCityDto deliveryScopeCityDto : deliveryScopeCityDtos) {
                                        if (level == 2) {
                                            //市级单位过滤
                                            existAddressIds.add(deliveryScopeCityDto.getCityId());//城市
                                        }
                                        if (level == 3) {
                                            //区县级单位过滤
                                            List<DeliveryScopeRegionDto> deliveryScopeRegionDtos = deliveryScopeCityDto.getRegionMap();
                                            if (!Objects.isNull(deliveryScopeRegionDtos)) {
                                                existAddressIds.addAll(Lists.transform(deliveryScopeRegionDtos, DeliveryScopeRegionDto::getRegionId));//区县
                                            } else {
                                                //县级单位全选的情况
                                                List<Integer> addressIds=getChildAddress(deliveryScopeCityDto.getCityId(),3);
                                                if(!Objects.isNull(addressIds)){
                                                    existAddressIds.addAll(addressIds);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    //市级单位全选的情况
                                    List<Integer> addressIds=getChildAddress(deliveryScopeDto.getProvinceId(),2);
                                    if(!Objects.isNull(addressIds)){
                                        existAddressIds.addAll(addressIds);
                                    }
                                }
                            }
                        }
                    }
                    addresses.removeIf(address -> !existAddressIds.contains(address.getId()));//删除其他区域信息
                } else {
                    log.debug("delivery scope is empty, shopId:{}", shopId);
                    return Collections.emptyList();
                }
            } else {
                log.error("read scope fail, shopId:{}, cause:{}", shopId, shopResp.getError());
                return Collections.emptyList();
            }
        }
        return addresses;
    }

    /**
     * 获取下级地址信息
     *
     * @param addressId 上级地址ID
     * @return 地址信息ID
     */
    public List<Integer> getChildAddress(Integer addressId,Integer level) {
        List<Integer> ReturnAddressIds=Lists.newArrayList();
        Response<List<Address>> response = addressReadService.childAddressOf(addressId);
        if (!response.isSuccess()) {
            log.error("fail to get child address by address id {}, error code:{}", addressId, response.getError());
            return null;
        }
        List<Integer> addressIds=Lists.transform(response.getResult(), Address::getId);
        ReturnAddressIds.addAll(addressIds);
        if(level == 2){
            //市级单位全选的情况
            for(Integer addId :addressIds ){
                Response<List<Address>> resp = addressReadService.childAddressOf(addId);
                if (!resp.isSuccess()) {
                    log.error("fail to get child address by address id {}, error code:{}", addId, resp.getError());
                }
                ReturnAddressIds.addAll(Lists.transform(resp.getResult(), Address::getId));
            }

        }
        return ReturnAddressIds;
    }
}
