package com.sanlux.store.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.store.service.VegaRepertoryToLocationLevelsReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.mapper.entity.Example;
import io.terminus.parana.store.impl.dao.RepertoryToLocationLevelsMapper;
import io.terminus.parana.store.model.RepertoryToLocationLevel;
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
public class VegaRepertoryToLocationLevelsReadServiceImpl implements VegaRepertoryToLocationLevelsReadService {
    private final RepertoryToLocationLevelsMapper repertoryToLocationLevelsMapper;
    @Autowired
    public VegaRepertoryToLocationLevelsReadServiceImpl(RepertoryToLocationLevelsMapper repertoryToLocationLevelsMapper) {
        this.repertoryToLocationLevelsMapper = repertoryToLocationLevelsMapper;
    }
    /**
     * 查找某一仓库/库区/库组/货架的子级
     * @param pid
     * @param repertoryId
     * @return
     */
    @Override
    public Response<List<RepertoryToLocationLevel>> findChildrenByIds(Long pid, Long repertoryId,List<Long> ids) {
        RepertoryToLocationLevel repertoryToLocationLevel = new RepertoryToLocationLevel();
        repertoryToLocationLevel.setPid(pid);
        repertoryToLocationLevel.setRepertoryId(repertoryId);
        Example example = new Example(RepertoryToLocationLevel.class);
        example.setOrderByClause("id desc");
        example.createCriteria().andEqualTo(repertoryToLocationLevel).andIn("id",ids);
        try {
            return Response.ok(repertoryToLocationLevelsMapper.selectByExample(example));
        } catch (Exception e) {
            log.error("repertoryToLocationLevel findByExample failed, repertoryToLocationLevel = {}, error = {}",
                    repertoryToLocationLevel, Throwables.getStackTraceAsString(e));
            return Response.fail("repertoryToLocationLevel.paging.failed");
        }
    }
}
