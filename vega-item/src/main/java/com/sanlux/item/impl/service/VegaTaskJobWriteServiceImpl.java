package com.sanlux.item.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.item.impl.dao.TaskJobRedisDao;
import com.sanlux.item.model.TaskJob;
import com.sanlux.item.service.VegaTaskJobWriteService;
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
public class VegaTaskJobWriteServiceImpl implements VegaTaskJobWriteService {

    private final TaskJobRedisDao taskJobRedisDao;

    @Autowired
    public VegaTaskJobWriteServiceImpl(TaskJobRedisDao taskJobRedisDao) {
        this.taskJobRedisDao = taskJobRedisDao;
    }

    @Override
    public Response<String> create(TaskJob taskJob) {
        try {
            return Response.ok(taskJobRedisDao.setTaskJob(taskJob));
        } catch (Exception e) {
            log.error("create task job :{} fail, cause:{}",
                    taskJob, Throwables.getStackTraceAsString(e));
            return Response.fail("create.task.job.fail");
        }
    }
}
