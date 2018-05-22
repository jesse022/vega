package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.TaskJobRedisDao;
import com.sanlux.item.model.TaskJob;
import com.sanlux.item.service.VegaTaskJobReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
@Service
@RpcProvider
@Slf4j
public class VegaTaskJobReadServiceImpl implements VegaTaskJobReadService {

    private final TaskJobRedisDao taskJobRedisDao;

    @Autowired
    public VegaTaskJobReadServiceImpl(TaskJobRedisDao taskJobRedisDao) {
        this.taskJobRedisDao = taskJobRedisDao;
    }

    @Override
    public Response<TaskJob> findByKey(String key) {
        try {
            return Response.ok(taskJobRedisDao.getTaskJob(key));
        } catch (Exception e) {
            log.error("find task job by key:{} fail, cause:{}",
                    key, Throwables.getStackTraceAsString(e));
            return Response.fail("task.job.find.fail");
        }
    }
}
