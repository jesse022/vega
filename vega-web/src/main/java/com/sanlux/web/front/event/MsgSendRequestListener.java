package com.sanlux.web.front.event;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.msg.service.MsgService;
import io.terminus.parana.web.core.events.msg.MsgSendRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * DATE: 17/1/18 下午3:22 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
@Component
public class MsgSendRequestListener {

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    private void init(){
        eventBus.register(this);
    }

    @Autowired
    private MsgService msgService;

    @Subscribe
    public void onMsgSendRequest(MsgSendRequestEvent event){
        try{
            if(!Strings.isNullOrEmpty(event.getTemplate())){
                msgService.send(event.getToes(), event.getTemplate(), event.getContext(), event.getExtra());
            }else{
                msgService.send(event.getToes(), event.getTitle(), event.getContent(), event.getExtra());
            }
        }catch (Exception e){
            log.error("fail to handle MsgSendRequestEvent={}, cause={}", event, Throwables.getStackTraceAsString(e));
        }
    }
}
