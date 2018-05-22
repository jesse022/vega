package com.sanlux.web.admin.user;

import com.google.common.collect.Maps;
import com.sanlux.user.dto.criteria.RankCriteria;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.RankReadService;
import com.sanlux.user.service.RankWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.exception.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/8/4
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/rank")
public class AdminRanks {
    @RpcConsumer
    private RankWriteService rankWriteService;
    @RpcConsumer
    private RankReadService rankReadService;


    @RequestMapping(method = RequestMethod.POST)
    public Long createRank(@RequestBody Rank rank) {

        Response<Long> response = rankWriteService.createRank(rank);
        if (!response.isSuccess()) {
            log.error(" rank create fail,rank{},error{}", rank, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Boolean updateRank(@RequestBody Rank rank) {
        Response<Boolean> response = rankWriteService.updateRank(rank);
        if (!response.isSuccess()) {
            log.error(" rank update fail,rank{},error{}", rank, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();


    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public Boolean deleteRank(@PathVariable("id") Long id) {

        Response<Boolean> response = rankWriteService.deleteRankById(id);
        if (!response.isSuccess()) {
            log.error(" rank delete  fail,id{},error{}", id, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/rank-paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<Rank>> pagingRanks(RankCriteria rankCriteria) {
        Response<Paging<Rank>> response = rankReadService.paging(rankCriteria);
        if (!response.isSuccess()) {
            log.error(" rank paging  fail,rankCriteria{},error{}", rankCriteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response;
    }

    /**
     * 批量更新等级信息
     *
     * @param ranks 等级对象数组队列JSON格式数据
     * @return Boolean
     */
    @RequestMapping(value = "/updateRank", method = RequestMethod.PUT)
    public Boolean updateRank(@RequestBody final List<Rank> ranks) {

        Map<Long, Rank> rankMap = rankToMap(ranks);
        Map<Long, Rank> prankMap = prankToMap(ranks);
        for (Rank rank : ranks) {
            checkGrowthValueValid(rank, rankMap, prankMap);
        }

        for (Rank rank : ranks) {
            Response<Boolean> response = rankWriteService.updateRank(rank);
            if (!response.isSuccess()) {
                log.error(" update rank fail,rank{},error{}", rank, response.getError());
                throw new JsonResponseException(response.getError());
            }
        }
        return Boolean.TRUE;
    }

    private void checkGrowthValueValid(Rank rank, Map<Long, Rank> rankMap, Map<Long, Rank> prankMap) {
        if (rank.getGrowthValueStart() < 0L) {
            //初始值小于0
            log.error("fail to update rank because growthValueStart should be bigger than 0,growthValueStart={} ", rank.getGrowthValueStart());
            throw new JsonResponseException("rank.growthValue.start.less.zero");
        }
        if (rank.getGrowthValueStart() >= rank.getGrowthValueEnd()) {
            //起始值大于等于终止值
            log.error("fail to update rank because growthValueEnd should be small than next rank growthValueStart ,growthValueEnd={} ", rank.getGrowthValueEnd());
            throw new InvalidException(500, "{0}.growthValue.start.large.growthValue.end", rank.getName());

        }

        //与下级做比较
        Rank nextRank = prankMap.get(rank.getId());
        if (Arguments.notNull(nextRank)) {
            if (rank.getGrowthValueEnd() >= nextRank.getGrowthValueStart()) {
                //终止值大于下一级起始值
                log.error("fail to update rank because growthValueEnd should be small than next rank growthValueStart ,growthValueEnd={} ", rank.getGrowthValueEnd());
                throw new InvalidException(500, "{0}.growthValueEnd.large.{1}.growthValueStart", rank.getName(), nextRank.getName());
            }
            if (nextRank.getGrowthValueStart() - rank.getGrowthValueEnd() != 1L) {
                log.error("fail to update rank because growthValueEnd should be small than next rank growthValueStart and value must be small than 0  ,growthValueEnd={} ", rank.getGrowthValueEnd());
                throw new InvalidException(500, "{0}.growthValueEnd.small.one.{1}.growthValueStart", rank.getName(), nextRank.getName());
            }


        }

        //与上级级做比较
        Rank frontRank = rankMap.get(rank.getPid());
        if (Arguments.notNull(frontRank)) {
            if (rank.getGrowthValueStart() < frontRank.getGrowthValueEnd()) {
                //当前等级起始值小于上一级终止值
                log.error("fail to update rank because growthValueStart should be large than front rank growthValueEnd ,growthValueEnd={} ", rank.getGrowthValueEnd());
                throw new InvalidException("{0}.growthValueStart.less.{1}.growthValueEnd", rank.getName(), frontRank.getName());
            }

        }
    }

    private Map<Long, Rank> rankToMap(List<Rank> ranks) {
        Map<Long, Rank> map = Maps.newHashMap();
        for (Rank rank : ranks) {
            map.put(rank.getId(), rank);
        }
        return map;
    }

    private Map<Long, Rank> prankToMap(List<Rank> ranks) {
        Map<Long, Rank> map = Maps.newHashMap();
        for (Rank rank : ranks) {
            if (Arguments.notNull(rank.getPid())) {
                map.put(rank.getPid(), rank);
            }
        }
        return map;
    }

    /**
     * 返回所有等级信息
     *
     * @return Rank数组JSON数据
     */
    @RequestMapping(value = "/find-all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Rank> findAll() {
        Response<List<Rank>> response = rankReadService.findAll();
        if (!response.isSuccess()) {
            log.error(" rank find all  fail,error{}", response.getError());
            throw new JsonResponseException("rank.find.all.fail");
        }
        return response.getResult();
    }

}
