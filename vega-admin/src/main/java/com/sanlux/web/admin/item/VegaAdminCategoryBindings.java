package com.sanlux.web.admin.item;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.CategoryBindingReadService;
import io.terminus.parana.category.service.CategoryBindingWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 前台类目批量绑定后台类目Control
 * Created by lujm on 2017/3/28.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/categoryBindings")
public class VegaAdminCategoryBindings {
    @RpcConsumer
    private CategoryBindingWriteService categoryBindingWriteService;

    @RpcConsumer
    private CategoryBindingReadService categoryBindingReadService;

    /**
     * 前台类目批量绑定后台类目
     *
     * @param frontCategoryId 前台类目id
     * @param backCategoryIds 后台类目ids
     * @return 是否成功
     */
    @RequestMapping(value = "/multi/bind", method = RequestMethod.GET)
    public Boolean multiBind(@RequestParam(value = "fid", required = true) Long frontCategoryId,
                             @RequestParam(value = "bids", required = true) List<Long> backCategoryIds) {

        backCategoryIds = backCategoryFilter(frontCategoryId, backCategoryIds);

        if (Objects.isNull(backCategoryIds) || backCategoryIds.isEmpty()) {
            throw new JsonResponseException("category.duplicated.binding");
        }

        Response<Boolean> booleanResponse = categoryBindingWriteService.multiBind(frontCategoryId, backCategoryIds);

        if (!booleanResponse.isSuccess()) {
            log.error("failed to bind front category(id={}) with back category(ids={}),error code:{}", frontCategoryId, backCategoryIds,
                    booleanResponse.getError());
            throw new JsonResponseException(booleanResponse.getError());
        }
        return booleanResponse.getResult();
    }

    /**
     * 剔除已经绑定过的后台类目
     *
     * @param frontCategoryId 前台类目Id
     * @param backCategoryIds 后台类目Ids
     * @return 后台类目Ids
     */
    private List<Long> backCategoryFilter(Long frontCategoryId, List<Long> backCategoryIds) {

        Response<List<BackCategory>> listResponse = categoryBindingReadService.findByFrontCategoryId(frontCategoryId);
        if (!listResponse.isSuccess()) {
            return backCategoryIds;
        }
        List<Long> existBackCategory = Lists.transform(listResponse.getResult(), BackCategory::getId);

        backCategoryIds.removeIf(backCategoryId -> existBackCategory.contains((Long) backCategoryId));

        return backCategoryIds;
    }

}
