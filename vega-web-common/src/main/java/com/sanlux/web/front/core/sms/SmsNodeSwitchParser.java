package com.sanlux.web.front.core.sms;

import com.google.common.base.Throwables;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.shop.dto.SmsNode;
import com.sanlux.shop.enums.CreditSmsNodeEnum;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.exception.MsgException;
import io.terminus.msg.service.MsgService;
import io.terminus.msg.util.MsgContext;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/12/16
 * Time: 3:37 PM
 */
@Component
@Slf4j
public class SmsNodeSwitchParser {

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private UserReadService<User> userUserReadService;

    @Autowired
    private MsgService msgService;


    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();


    /**
     * 判断信用额度 短信节点是否打开
     * @param nodeEnum 节点
     * @return 是否打开
     */
    public Response<Boolean> creidtIsOpen(CreditSmsNodeEnum nodeEnum){
        Response<Shop> shopRes = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (!shopRes.isSuccess()) {
            log.error("failed to find shop by id: ({}) ,error:{}", DefaultId.PLATFROM_SHOP_ID, shopRes.getError());
            return Response.fail(shopRes.getError());
        }

        Shop shop = shopRes.getResult();

        Response<List<SmsNode>> smsNodesRes = getSmsNodeFromData(shop.getTags().get(SystemConstant.CREDIT_SMS_NODE));
        if(!smsNodesRes.isSuccess()){
            return Response.fail(smsNodesRes.getError());

        }
        for (SmsNode node : smsNodesRes.getResult()){
            if(node.getNodeName().equals(nodeEnum.getName())){
                return node.getIsChecked() == 1 ? Response.ok(Boolean.TRUE) : Response.ok(Boolean.FALSE);
            }
        }

        return Response.ok(Boolean.FALSE);

    }



    /**
     * 判断交易 短信节点是否打开
     * @param nodeEnum 节点
     * @return 是否打开
     */
    public Response<Boolean> tradeIsOpen(TradeSmsNodeEnum nodeEnum){
        Response<Shop> shopRes = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (!shopRes.isSuccess()) {
            log.error("failed to find shop by id: ({}) ,error:{}", DefaultId.PLATFROM_SHOP_ID, shopRes.getError());
            return Response.fail(shopRes.getError());
        }

        Shop shop = shopRes.getResult();

        Response<List<SmsNode>> smsNodesRes = getSmsNodeFromData(shop.getTags().get(SystemConstant.TRADE_SMS_NODE));

        for (SmsNode node : smsNodesRes.getResult()){
            if(node.getNodeName().equals(nodeEnum.getName())){
                if(node.getIsChecked()>0){
                    return Response.ok(Boolean.TRUE);
                }else {
                    return Response.ok(Boolean.FALSE);
                }
            }
        }

        return Response.ok(Boolean.FALSE);

    }

    public Response<User> getShopUserByShopId(Long shopId){

        Response<Shop> shopRes = shopReadService.findById(shopId);
        if (!shopRes.isSuccess()) {
            log.error("fail to find shop by shop id:{},cause:{}", shopId, shopRes.getError());
            return Response.fail(shopRes.getError());
        }

        Shop shop = shopRes.getResult();

        Response<User> userRes = userUserReadService.findById(shop.getUserId());
        if (!userRes.isSuccess()) {
            log.error("fail to find user by  id:{},cause:{}", shop.getUserId(), userRes.getError());
            return Response.fail(userRes.getError());
        }

        return userRes;

    }

    public Response<User> getBuyerByOrderId(ShopOrder shopOrder){

        Response<User> userRes = userUserReadService.findById(shopOrder.getBuyerId());
        if (!userRes.isSuccess()) {
            log.error("fail to find user by  id:{},cause:{}", shopOrder.getBuyerId(), userRes.getError());
            return Response.fail(userRes.getError());
        }

        return userRes;
    }



    public void doSendSms(String mobile, String template, Map<String,String> params){
        try{
            String result=msgService.send(mobile, template, MsgContext.of(params), null);
            log.info("sendSms result={}, mobile={}, message={}", result, mobile,params);
        }catch (MsgException e){
            log.error("sms send failed, mobile={}, cause:{}", mobile, Throwables.getStackTraceAsString(e));
            //throw new JsonResponseException("sms.send.fail");
        }
    }



    private Response<Shop> getShopById(Long shopId){
        return shopReadService.findById(shopId);

    }

    private Response<List<SmsNode>> getSmsNodeFromData(String data) {
        try {
            return Response.ok(JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(List.class, SmsNode.class)));
        }catch (Exception e) {
            log.error("fail to get sms node info from data={},cause:{}",data, Throwables.getStackTraceAsString(e));
        }
        return Response.fail("parser.sms.node.info.fail");
    }
}
