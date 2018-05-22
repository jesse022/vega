package com.sanlux.item.impl.dao;

import com.sanlux.item.model.TaskJob;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.Jedis;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
@Repository
public class TaskJobRedisDao {

    @Autowired
    private JedisTemplate jedisTemplate;

    public String keyOfTaskJob(Long userId, String path) {
        return "cache:job:userId" + userId + ":path:" + path;
    }

    public TaskJob getTaskJob(final String key) {
        return jedisTemplate.execute(new JedisTemplate.JedisAction<TaskJob>() {
            @Override
            public TaskJob action(Jedis jedis) {
                String json = jedis.get(key);
                TaskJob taskJob = new TaskJob();
                if (Params.trimToNull(json) != null) {
                    taskJob = readTaskJobFromJson(json);
                }
                return taskJob;
            }
        });
    }

    public String setTaskJob(final TaskJob taskJob) {
        final String key = keyOfTaskJob(taskJob.getUserId(), taskJob.getExtra());
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                String json = JsonMapper.nonDefaultMapper().toJson(taskJob);
                jedis.setex(key, 60 * 60, json);
            }
        });
        return key;
    }

    public void invalidTaskJob(final Long userId, final String path) {
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.del(keyOfTaskJob(userId, path));
            }
        });
    }

    protected TaskJob readTaskJobFromJson(String json) {
        return JsonMapper.nonDefaultMapper().fromJson(json, TaskJob.class);
    }
}
