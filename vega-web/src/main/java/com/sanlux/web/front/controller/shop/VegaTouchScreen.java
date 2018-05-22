package com.sanlux.web.front.controller.shop;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.shop.model.VegaShopExtraBasicInfo;
import com.sanlux.shop.service.VegaShopExtraBasicInfoReadService;
import com.sanlux.shop.service.VegaShopExtraBasicInfoWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 触摸屏处理control
 * Created by lujm on 2017/12/20.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/touch-screen")
public class VegaTouchScreen {
    @RpcConsumer
    private VegaShopExtraBasicInfoReadService vegaShopExtraBasicInfoReadService;
    @RpcConsumer
    private VegaShopExtraBasicInfoWriteService vegaShopExtraBasicInfoWriteService;
    @Autowired
    private ShopCacher shopCacher;

    /**
     * 触摸屏本地信息维护(背景图片|公司介绍|推荐品牌(admin)|视频信息 )
     *
     * @param basicInfo         背景图片地址|公司介绍|视频信息|推荐品牌Ids macAddress(必输项,客户端浏览器唯一标识(浏览器canvas指纹)
     * @return 主键Id
     */
    @RequestMapping(method = RequestMethod.POST)
    public Long createOrUpdateVegaShopBasicInfo(@RequestBody VegaShopExtraBasicInfo basicInfo) {
        // 获取当前用户店铺ID
        ParanaUser user = UserUtil.getCurrentUser();
        Long shopId;
        if (user.getRoles().contains(UserRole.ADMIN.name())) {
            shopId = DefaultId.PLATFROM_SHOP_ID;
        } else {
            shopId = user.getShopId();
        }
        Shop shop = shopCacher.findShopById(shopId);

        if (shop == null) {
            log.error("failed to create shop extra basic info, cause shop is null");
            throw new JsonResponseException(500, "shop.is.null");
        }

        Response<VegaShopExtraBasicInfo> basicInfoResp = vegaShopExtraBasicInfoReadService.findByShopId(shopId);
        if (!basicInfoResp.isSuccess()) {
            log.error("failed to find shop extra basic info shopId : ({}), cause : {}", shopId, basicInfoResp.getError());
            throw new JsonResponseException(500, basicInfoResp.getError());
        }
        VegaShopExtraBasicInfo vegaShopExtraBasicInfo = basicInfoResp.getResult();
        Map<String, String> extraMap = Maps.newHashMap();

        if(StringUtils.hasText(basicInfo.getRecommendedBrands())) {
            extraMap.put(SystemConstant.TOUCH_SCREEN_RECOMMENDED_BRANDS, basicInfo.getRecommendedBrands());
        }
        if (Arguments.isNull(vegaShopExtraBasicInfo)) {
            vegaShopExtraBasicInfo = new VegaShopExtraBasicInfo();
        }
        vegaShopExtraBasicInfo.setBackgroundPicture(basicInfo.getBackgroundPicture());
        vegaShopExtraBasicInfo.setDetail(basicInfo.getDetail());
        vegaShopExtraBasicInfo.setExtra(extraMap);
        vegaShopExtraBasicInfo.setVideos(basicInfo.getVideos());
        vegaShopExtraBasicInfo.setMacAddress(basicInfo.getMacAddress());

        if (Objects.equal(vegaShopExtraBasicInfo.getShopId(), shopId)) {
            // 修改
            vegaShopExtraBasicInfo.setShopName(shop.getName());
            vegaShopExtraBasicInfo.setUserName(user.getName());
            vegaShopExtraBasicInfo.setShopStatus(shop.getStatus());
            vegaShopExtraBasicInfo.setShopType(shop.getType());
            Response<Long> updateResp = vegaShopExtraBasicInfoWriteService.updateByShopId(vegaShopExtraBasicInfo);
            if (!updateResp.isSuccess()) {
                log.error("failed to update shop extra basic info shopId : ({}), cause : {}", shopId, updateResp.getError());
                throw new JsonResponseException(500, updateResp.getError());
            }
            return updateResp.getResult();
        }

        vegaShopExtraBasicInfo.setShopId(shop.getId());
        vegaShopExtraBasicInfo.setShopName(shop.getName());
        vegaShopExtraBasicInfo.setUserId(user.getId());
        vegaShopExtraBasicInfo.setUserName(user.getName());
        vegaShopExtraBasicInfo.setShopStatus(shop.getStatus());
        vegaShopExtraBasicInfo.setShopType(shop.getType());
        Response<Long> createResp = vegaShopExtraBasicInfoWriteService.create(vegaShopExtraBasicInfo);
        if (!createResp.isSuccess()) {
            log.error("failed to update shop extra basic info shopId : ({}), cause : {}", shopId, createResp.getError());
            throw new JsonResponseException(500, createResp.getError());
        }
        return createResp.getResult();
    }

    /**
     * 根据店铺Id获取服务商基础信息扩展表信息
     * @return
     */
    @RequestMapping(value = "/find-by-shop/{shopId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaShopExtraBasicInfo findShopBasicInfoByShopId(@PathVariable(value = "shopId") Long shopId) {
        Response<VegaShopExtraBasicInfo> resp = vegaShopExtraBasicInfoReadService.findByShopId(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop extra basic info by shopId: ({}), cause : {}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 根据店铺Id获取服务商基础信息扩展表信息
     * @return
     */
    @RequestMapping(value = "/find-by-mac", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaShopExtraBasicInfo findShopBasicInfoByMacAddress(@RequestParam(value = "macAddress", required = true) String macAddress) {
        Response<VegaShopExtraBasicInfo> resp = vegaShopExtraBasicInfoReadService.findByMacAddress(macAddress);
        if (!resp.isSuccess()) {
            log.error("failed to find shop extra basic info by macAddress: ({}), cause : {}", macAddress, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }
}
