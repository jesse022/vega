package com.sanlux.web.front.controller.nation;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.sanlux.user.model.Nation;
import com.sanlux.user.service.NationReadService;
import com.sanlux.web.front.core.utils.AddressUtils;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.pay.util.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


/**
 * Created by lujm on 2017/2/24.
 */
@Slf4j
@RestController
@RequestMapping("/api/nation")
public class VegaNation {
    @RpcConsumer
    private NationReadService nationReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;

    @RequestMapping(value = "/get-qiyu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getQiYuInfo(HttpServletRequest request) {
        Map<String, Object> returnMap = Maps.newHashMap();
        try {
            String ip=AddressUtils.getIpAddress(request);
            log.debug("ip address======="+ip);
            String addressCode = AddressUtils.getAddresses("ip=" + ip, "utf-8");
            if(!Arguments.isNull(addressCode)) {
                Response<Optional<Nation>> findAll = nationReadService.findByCode(addressCode);
                if (!findAll.isSuccess()) {
                    log.error("fail to find nation, cause:{}", findAll.getError());
                    returnMap.put("nation", null);
                }
                if (!findAll.getResult().isPresent()) {
                    log.info("qiYu info null, addressCode:{}", addressCode);
                    returnMap.put("nation", null);
                } else {
                    returnMap.put("nation", findAll.getResult().get());
                }
            }else{
                returnMap.put("nation", null);
            }
            //获取用户信息
            ParanaUser paranaUser = UserUtil.getCurrentUser();
            if(!Arguments.isNull(paranaUser)){
                Response<User> userResponse = userReadService.findById(paranaUser.getId());
                if (!userResponse.isSuccess()) {
                    log.error("find User by id:{} fail,cause:{}", paranaUser.getId(), userResponse.getError());
                    returnMap.put("user", null);
                }else {
                    returnMap.put("user", userResponse.getResult());
                }
            }else{
                returnMap.put("user", null);
            }
            return returnMap;
        } catch (Exception e) {
            log.error("fail to get qiYu info, cause:{}", Throwables.getStackTraceAsString(e));
            return returnMap;
        }
    }
}
