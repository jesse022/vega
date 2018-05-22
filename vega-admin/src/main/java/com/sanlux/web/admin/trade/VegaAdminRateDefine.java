package com.sanlux.web.admin.trade;

import com.sanlux.trade.model.VegaRateDefs;
import com.sanlux.trade.service.VegaRateDefsReadService;
import com.sanlux.trade.service.VegaRateDefsWriteService;
import com.sanlux.user.dto.criteria.PagingCriteria;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


/**
 * 三力士费率定义Control
 *
 * Created by lujm on 2017/11/17.
 */

@RestController
@Slf4j
@RequestMapping("/api/admin/rate")
public class VegaAdminRateDefine {

    @RpcConsumer
    private VegaRateDefsWriteService vegaRateDefsWriteService;
    @RpcConsumer
    private VegaRateDefsReadService vegaRateDefsReadService;

    /**
     * 修改费率值
     *
     * @param id 主键Id
     * @param rateKey 费率值
     * @return 是否成功
     */
    @RequestMapping(value = "/update/{id}",method = RequestMethod.POST)
    public Boolean updateStatus(@PathVariable Long id,
                                @RequestParam Long rateKey) {
        Response<Boolean> resp = vegaRateDefsWriteService.updateRateKey(id, rateKey);
        if (!resp.isSuccess()) {
            log.error("failed to update rate define id = ({}),rateKey = ({}) cause : {}",
                    id, rateKey, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 根据主键Id获取详情
     *
     * @param id  主键Id
     * @return 详情
     */
    @RequestMapping(value = "/find/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaRateDefs findById(@PathVariable(value = "id") Long id) {
        Response<VegaRateDefs> resp = vegaRateDefsReadService.findById(id);
        if (!resp.isSuccess()) {
            log.error("failed to find rate define by id = {}, cause : {}", id, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 分页查询
     *
     * @param pagingCriteria 分页查询条件
     * @return 分页信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<VegaRateDefs> vegaUserPaging(PagingCriteria pagingCriteria) {
        Response<Paging<VegaRateDefs>> resp = vegaRateDefsReadService.paging(pagingCriteria.getPageNo(), pagingCriteria.getPageSize());
        if (!resp.isSuccess()) {
            log.error("failed to paging rate define, cause : {}", resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }


}
