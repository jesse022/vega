package com.sanlux.web.front.test;

import io.terminus.msg.service.MsgService;
import io.terminus.msg.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DATE: 16/8/17 下午5:47 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@RestController
@Slf4j
@Profile({"dev", "test"})
public class MsgSendController {

    @Autowired
    private MsgService msgService;

    /**
     * @param toes
     * @param template
     * @param request
     * @return
     */
    @RequestMapping("/api/msg/send")
    public String send(@RequestParam("toes") String toes,
                       @RequestParam(value = "content", required = false) String content,
                       @RequestParam(value = "template", required = false) String template,
                       HttpServletRequest request){
        Map<String, Serializable> map=new HashMap<>();
        for(String key: request.getParameterMap().keySet()){
            map.put(key,request.getParameter(key));
        }
        if(template!=null) {
            return msgService.send(toes, template, map, JsonUtil.JSON.toJson(map));
        }else{
            return msgService.send(toes, null, content, JsonUtil.JSON.toJson(map));
        }
    }
}
