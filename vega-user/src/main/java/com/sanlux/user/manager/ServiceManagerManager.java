package com.sanlux.user.manager;

import com.sanlux.user.impl.dao.ServiceManagerDao;
import com.sanlux.user.model.ServiceManager;
import io.terminus.parana.common.utils.EncryptUtil;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户表/业务经理信息表Manager类
 *
 * Created by lujm on 2017/5/23.
 */
@Slf4j
@Component
public class ServiceManagerManager {

    private final UserDao userDao;
    private final ServiceManagerDao serviceManagerDao;

    @Autowired
    public ServiceManagerManager(UserDao userDao, ServiceManagerDao serviceManagerDao) {
        this.serviceManagerDao = serviceManagerDao;
        this.userDao = userDao;
    }

    @Transactional
    public Boolean create(User user, ServiceManager serviceManager) throws Exception {

        if(StringUtils.hasText(user.getPassword())) {
            //加密
            user.setPassword(EncryptUtil.encrypt(user.getPassword()));
        }
        Boolean tag = userDao.create(user);
        if (tag) {
            serviceManager.setUserId(user.getId());
            serviceManager.setUserName(user.getName());//用户名
            serviceManager.setMobile(user.getMobile());
            serviceManager.setStatus(user.getStatus());
            return serviceManagerDao.create(serviceManager);
        }
        return tag;
    }

    @Transactional
    public Boolean update(User user, ServiceManager serviceManager) throws Exception {
        Boolean tag = serviceManagerDao.update(serviceManager);
        if (tag) {
            if(StringUtils.hasText(user.getPassword())) {
                //加密
                user.setPassword(EncryptUtil.encrypt(user.getPassword()));
            }
            user.setMobile(serviceManager.getMobile());
            user.setName(serviceManager.getUserName());
            return userDao.update(user);
        }
        return tag;
    }

    @Transactional
    public Boolean updateStatus(Long id,Long userId, Integer status) throws Exception {
        Boolean tag = serviceManagerDao.updateStatus(id, status);
        if (tag) {
            return userDao.updateStatus(userId, status) > 0;
        }
        return tag;
    }
}
