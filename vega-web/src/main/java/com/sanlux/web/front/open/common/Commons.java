package com.sanlux.web.front.open.common;

import com.google.common.collect.ImmutableMap;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.core.OPEventDispatcher;
import io.terminus.pampas.openplatform.core.SecurityManager;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2016-04-23 4:39 PM  <br>
 * Author: xiao
 */
@Slf4j
//@OpenBean
public class Commons {

    @Autowired
    private OPEventDispatcher dispatcher;

    @Autowired
    private SecurityManager securityManager;

    //@OpenMethod(key="server.time")
    public Map<String, String> serverTime() {
        return ImmutableMap.of("time", DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")));
    }

}
