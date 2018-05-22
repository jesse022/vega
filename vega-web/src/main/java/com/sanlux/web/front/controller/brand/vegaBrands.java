package com.sanlux.web.front.controller.brand;

import com.sanlux.item.model.VegaBrandExtra;
import com.sanlux.item.service.VegaBrandExtraReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.cache.BrandCacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 品牌处理control类
 *
 * Created by lujm on 2017/8/10.
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/brands")
public class vegaBrands {

    @RpcConsumer
    private BrandCacher brandCacher;

    @RpcConsumer
    private VegaBrandExtraReadService vegaBrandExtraReadService;


    /**
     * 通过Ids获取品牌信息,Ids装修时录入find-by-ids
     *
     * @param brandIds 品牌Ids
     * @return 品牌信息
     */
    @RequestMapping(value = "/find-by-ids", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<Brand>> getBrand(@RequestParam("brandIds") String brandIds){
        if (!StringUtils.hasText(brandIds)) {
            return Response.ok(Collections.emptyList());
        }
        List<Brand> brandList = brandCacher.findBrandByIds(brandIds);
        return Arguments.isNull(brandList) ? Response.ok(Collections.emptyList()) : Response.ok(brandList);
    }

    /**
     * 根据brandId获取品牌扩展信息
     * @param brandId 品牌Id
     * @return 品牌扩展详情
     */
    @RequestMapping(value = "/find-detail/{brandId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<VegaBrandExtra> getBrandExtra(@PathVariable("brandId") Long brandId){
        Response<VegaBrandExtra> rspBrand = vegaBrandExtraReadService.findBrandExtraByCacher(brandId);
        if (!rspBrand.isSuccess()) {
            log.error("fail to find brand extra by brandId:{}, cause:{}",  brandId, rspBrand.getError());
            return Response.fail("brand.extra.find.fail");
        }
        return Response.ok(Arguments.isNull(rspBrand.getResult()) ? new VegaBrandExtra() : rspBrand.getResult());
    }

}
