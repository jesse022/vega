package com.sanlux.web.admin.item;

import com.sanlux.item.model.VegaBrandExtra;
import com.sanlux.item.service.VegaBrandExtraReadService;
import com.sanlux.item.service.VegaBrandExtraWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Created by lujm on 2018/1/22.
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/admin/brand")
public class VegaAdminBrand {
    @RpcConsumer
    private VegaBrandExtraWriteService vegaBrandExtraWriteService;

    @RpcConsumer
    private VegaBrandExtraReadService vegaBrandExtraReadService;

    /**
     * 创建品牌扩展详情
     * @param brandId 品牌ID
     * @param detail 品牌介绍详情
     * @return 详情Id
     */
    @RequestMapping(method = RequestMethod.POST)
    public Boolean createVegaBrandExtra(@RequestParam("brandId") Long   brandId,
                                     @RequestParam("detail")  String detail) {

        VegaBrandExtra brandExtra = new VegaBrandExtra();
        brandExtra.setBrandId(brandId);
        brandExtra.setDetail(detail);

        if (Arguments.isNull(getBrandExtra(brandId).getResult().getId())) {
            Response<Long> brandResp = vegaBrandExtraWriteService.create(brandExtra);
            if (!brandResp.isSuccess()) {
                log.error("failed to create brand extra : ({}), cause : {}", brandExtra, brandResp.getError());
                throw new JsonResponseException(500, brandResp.getError());
            }
            return Arguments.isNull(brandResp.getResult()) ?  Boolean.FALSE : Boolean.TRUE;
        }

        Response<Boolean> brandResp = vegaBrandExtraWriteService.updateByBrandId(brandId, detail);
        if (!brandResp.isSuccess()) {
            log.error("failed to update brand extra by brandId : ({}), cause : {}", brandId, brandResp.getError());
            throw new JsonResponseException(500, brandResp.getError());
        }
        //失效缓存
        vegaBrandExtraWriteService.invalidByBranId(brandId);

        return brandResp.getResult();
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
