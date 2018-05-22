package com.sanlux.web.front.controller.item;

import com.sanlux.item.dto.IntegrationItemCriteria;
import com.sanlux.item.enums.IntegrationItemStatus;
import com.sanlux.item.model.IntegrationItem;
import com.sanlux.item.service.IntegrationItemReadService;
import com.sanlux.item.service.IntegrationItemWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by cuiwentao
 * on 16/11/7
 */
@Slf4j
@RestController
@RequestMapping("/api/integration-item")
public class VegaIntegrationItem {

    @RpcConsumer
    private IntegrationItemWriteService integrationItemWriteService;

    @RpcConsumer
    private IntegrationItemReadService integrationItemReadService;

    @RequestMapping(value = "/paging", method = RequestMethod.GET)
    public Paging<IntegrationItem> integrationItemPaging (
            @RequestParam(value = "integration", required = false) Integer integration,
            @RequestParam(value = "sortType", required = true) Integer sortType,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        IntegrationItemCriteria criteria = new IntegrationItemCriteria();
        criteria.setIntegrationPrice(integration);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setStatus(IntegrationItemStatus.ONSHELF.value());
        criteria.setSortBy("integrationPrice");
        criteria.setSortType(sortType);

        Response<Paging<IntegrationItem>> response = integrationItemReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("paging integration item for web fail, criteria:{}, cause:{}",
                    criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

}
