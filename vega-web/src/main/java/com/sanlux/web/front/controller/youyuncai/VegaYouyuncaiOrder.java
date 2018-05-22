package com.sanlux.web.front.controller.youyuncai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.dto.RichSkuWithItem;
import com.sanlux.item.service.VegaSkuReadService;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.TradeSmsNodeEnum;
import com.sanlux.trade.enums.VegaOrderChannelEnum;
import com.sanlux.trade.enums.VegaPayType;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.model.YouyuncaiOrder;
import com.sanlux.trade.service.*;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import com.sanlux.web.front.component.item.VegaSkuPriceCacher;
import com.sanlux.web.front.core.events.trade.TradeSmsEvent;
import com.sanlux.web.front.core.events.trade.VegaOrderCreatedEvent;
import com.sanlux.web.front.core.util.ArithUtil;
import com.sanlux.web.front.core.youyuncai.order.constants.YouyuncaiConstants;
import com.sanlux.web.front.core.youyuncai.order.dto.*;
import com.sanlux.web.front.core.youyuncai.request.VegaYouyuncaiComponent;
import com.sanlux.web.front.core.youyuncai.request.YouyuncaiRequest;
import com.sanlux.web.front.core.youyuncai.token.JcforLoginVerification;
import com.sanlux.web.front.core.youyuncai.token.YouyuncaiToken;
import com.sanlux.youyuncai.dto.YouyuncaiReturnStatus;
import com.sanlux.youyuncai.enums.YouyuncaiApiType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserStatus;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.parana.order.api.RichOrderMaker;
import io.terminus.parana.order.dto.*;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.rule.OrderRuleEngine;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.promotion.component.OrderCharger;
import io.terminus.parana.promotion.model.Promotion;
import io.terminus.parana.promotion.service.PromotionReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserProfileWriteService;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import io.terminus.parana.web.core.events.user.LoginEvent;
import io.terminus.parana.web.core.events.user.UserRegisteredEvent;
import io.terminus.parana.web.core.util.ParanaUserMaker;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 友云采订单对接接口
 * Created by lujm on 2018/2/27.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/youyuncai")
public class VegaYouyuncaiOrder {
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JcforLoginVerification jcforLoginVerification;

    @Autowired
    private VegaYouyuncaiComponent vegaYouyuncaiComponent;

    @Autowired
    private RichOrderMaker richOrderMaker;

    @Autowired
    private ReceiveShopParser receiveShopParser;

    @Autowired
    private OrderRuleEngine orderRuleEngine;

    @Autowired
    private DeliveryFeeCharger deliveryFeeCharger;

    @Autowired
    private OrderCharger charger;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ObjectMapper objectMapper;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private UserWriteService<User> userWriteService;

    @RpcConsumer
    private UserProfileWriteService userProfileWriteService;

    @RpcConsumer
    private PurchaseOrderWriteService purchaseOrderWriteService;

    @RpcConsumer
    private PurchaseOrderReadService purchaseOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;

    @RpcConsumer
    private PromotionReadService promotionReadService;

    @RpcConsumer
    private YouyuncaiOrderWriteService youyuncaiOrderWriteService;

    @RpcConsumer
    private VegaOrderWriteService vegaOrderWriteService;

    @RpcConsumer
    private PurchaseSkuOrderReadService purchaseSkuOrderReadService;

    @RpcConsumer
    private VegaSkuReadService vegaSkuReadService;

    @Autowired
    private VegaSkuPriceCacher vegaSkuPriceCacher;

    @Value("${web.domain}")
    private  String redirectUrl ;


    /**
     * 友云采下单到集乘网接口
     *
     * @param submittedOrderDto submittedOrderDto
     * @return 返回信息
     */
    @RequestMapping(value = "/order-sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JcforReturnStatus orderCreate(@RequestBody SubmittedOrderDto submittedOrderDto) {
        JcforReturnStatus jcforReturnStatus = new JcforReturnStatus();
        jcforReturnStatus.setStatus(0);
        if (Arguments.isNull(submittedOrderDto.getHeader())
                || Arguments.isNull(submittedOrderDto.getBody())) {
            jcforReturnStatus.setErrormsg("请求参数获取错误,下单接口请求失败!");
            log.error("[YOU-YUN-CAI]:fail to order sync because parameter [header] or [body] is null");
            return jcforReturnStatus;
        }

        if (!jcforLoginVerification.isVerificationSuccess(submittedOrderDto.getHeader().getClientId(),
                submittedOrderDto.getHeader().getClientSecret())) {
            log.error("[YOU-YUN-CAI]:fail to login jcfor because verification fail");
            jcforReturnStatus.setErrormsg("集乘网认证账号或密码验证错误,下单接口请求失败!");
            return jcforReturnStatus;
        }

        YouyuncaiOrderDto youyuncaiOrderDto = submittedOrderDto.getBody();

        if (Strings.isNullOrEmpty(youyuncaiOrderDto.getOrderCode())
                || Strings.isNullOrEmpty(youyuncaiOrderDto.getCustUserCode())
                || Strings.isNullOrEmpty(youyuncaiOrderDto.getApprovedTime())
                || Strings.isNullOrEmpty(youyuncaiOrderDto.getTotalAmount())
                || Strings.isNullOrEmpty(youyuncaiOrderDto.getInvoiceState())
                || Arguments.isNull(youyuncaiOrderDto.getPurchaser())
                || Arguments.isNull(youyuncaiOrderDto.getConsignee())
                || Arguments.isNull(youyuncaiOrderDto.getDeliverAddress())
                || Arguments.isNull(youyuncaiOrderDto.getInvoiceInfo())
                || Arguments.isNull(youyuncaiOrderDto.getInvoiceReceiver())
                || Arguments.isNull(youyuncaiOrderDto.getInvoiceAddress())
                || Arguments.isNullOrEmpty(youyuncaiOrderDto.getOrderDetail())) {
            log.error("[YOU-YUN-CAI]:fail to order sync because some required filed is null");
            jcforReturnStatus.setErrormsg("订单信息有必填字段为空,下单接口请求失败!");
            return jcforReturnStatus;
        }

        if (Double.valueOf(youyuncaiOrderDto.getTotalAmount()) < Double.valueOf(getPlatfromOrderShipFee())) {
            log.error("[YOU-YUN-CAI]:fail to order sync because total fee lt free shipping fee");
            jcforReturnStatus.setErrormsg("下单金额不满足集乘网包邮条件,下单接口请求失败!");
            return jcforReturnStatus;
        }

        ParanaUser paranaUser = getUserById(Long.valueOf(youyuncaiOrderDto.getCustUserCode()));
        UserUtil.putCurrentUser(paranaUser);
        if (Arguments.isNull(paranaUser)) {
            log.error("[YOU-YUN-CAI]:fail to order sync because user find fail");
            jcforReturnStatus.setErrormsg("集乘网未找到下单用户信息,下单接口请求失败!");
            return jcforReturnStatus;
        }


        // 创建集乘网订单
        Response<Boolean> rsp = jcforOrderCreate(youyuncaiOrderDto, paranaUser);
        if (rsp.isSuccess()) {
            jcforReturnStatus.setStatus(1);
            return jcforReturnStatus;
        } else {
            jcforReturnStatus.setErrormsg(rsp.getError());
            return jcforReturnStatus;
        }
            
    }

    /**
     * 用户同步接口
     * @param submittedUserSyncDto submittedUserSyncDto
     * @return 返回信息
     */
    @RequestMapping(value = "/user-sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JcforReturnStatus> userSync(@RequestBody SubmittedUserSyncDto submittedUserSyncDto) {
        List<JcforReturnStatus> jcforReturnStatusList = Lists.newArrayList();

        JcforReturnStatus wholeReturnStatus = new JcforReturnStatus();
        wholeReturnStatus.setStatus(0);
        if (Arguments.isNull(submittedUserSyncDto.getHeader())
                || Arguments.isNull(submittedUserSyncDto.getBody())) {
            log.error("[YOU-YUN-CAI]:fail to user sync because parameter [header] or [body] is null");
            wholeReturnStatus.setErrormsg("请求参数获取错误,用户同步失败!");
            jcforReturnStatusList.add(wholeReturnStatus);

            return jcforReturnStatusList;
        }

        if (!jcforLoginVerification.isVerificationSuccess(submittedUserSyncDto.getHeader().getClientId(),
                submittedUserSyncDto.getHeader().getClientSecret())) {
            log.error("[YOU-YUN-CAI]:fail to login jcfor because verification fail");
            wholeReturnStatus.setErrormsg("集乘网认证账号或密码验证错误,用户同步失败!");
            jcforReturnStatusList.add(wholeReturnStatus);

            return jcforReturnStatusList;
        }

        List<YouyuncaiUserDto> youyuncaiUserDtos = submittedUserSyncDto.getBody();

        if (Arguments.isNull(youyuncaiUserDtos) || youyuncaiUserDtos.size() > 100) {
            log.error("[YOU-YUN-CAI]:fail to login jcfor because [body] is null or [body] gt 100");
            wholeReturnStatus.setErrormsg("需要同步的数据为空或大于100条,用户同步失败!");
            jcforReturnStatusList.add(wholeReturnStatus);

            return jcforReturnStatusList;
        }

        for (YouyuncaiUserDto youyuncaiUserDto : youyuncaiUserDtos) {

            JcforReturnStatus jcforReturnStatus = new JcforReturnStatus();
            jcforReturnStatus.setStatus(0);
            Map<String, Object> dataMap = Maps.newHashMap();

            if (Strings.isNullOrEmpty(youyuncaiUserDto.getUserMobile())
                    || Strings.isNullOrEmpty(youyuncaiUserDto.getYouyuncaiUserCode())
                    || Strings.isNullOrEmpty(youyuncaiUserDto.getYouyuncaiGroupCode())
                    || Strings.isNullOrEmpty(youyuncaiUserDto.getYouyuncaiGroupName())
                    || Strings.isNullOrEmpty(youyuncaiUserDto.getYouyuncaiOrgCode())
                    || Strings.isNullOrEmpty(youyuncaiUserDto.getYouyuncaiOrgName())) {
                log.error("[YOU-YUN-CAI]:fail to user sync because some required filed is null");
                jcforReturnStatus.setErrormsg("用户信息有必填字段为空,用户同步失败!");
                dataMap.put(YouyuncaiConstants.USER_CODE, youyuncaiUserDto.getYouyuncaiUserCode());
                jcforReturnStatus.setData(dataMap);
                jcforReturnStatusList.add(jcforReturnStatus);
                continue;
            }

            if (!isChinaPhoneLegal(youyuncaiUserDto.getUserMobile()) || findMobileExists(youyuncaiUserDto.getUserMobile())) {
                log.error("[YOU-YUN-CAI]:fail to user sync because mobile : {} is exists", youyuncaiUserDto.getUserMobile());
                jcforReturnStatus.setErrormsg("手机号" + youyuncaiUserDto.getUserMobile() + "格式不对或集乘网中已经存在,用户同步失败!");
                dataMap.put(YouyuncaiConstants.USER_CODE, youyuncaiUserDto.getYouyuncaiUserCode());
                jcforReturnStatus.setData(dataMap);
                jcforReturnStatusList.add(jcforReturnStatus);
                continue;
            }

            Long userId = createUser(youyuncaiUserDto);
            if (Arguments.isNull(userId)) {
                log.error("[YOU-YUN-CAI]:fail to user sync because system error");
                dataMap.put(YouyuncaiConstants.USER_CODE, youyuncaiUserDto.getYouyuncaiUserCode());
                jcforReturnStatus.setErrormsg("系统内部错误,用户同步失败!");
                jcforReturnStatus.setData(dataMap);
                jcforReturnStatusList.add(jcforReturnStatus);
                continue;
            }

            jcforReturnStatus.setStatus(1);
            dataMap.put(YouyuncaiConstants.USER_CODE, youyuncaiUserDto.getYouyuncaiUserCode());
            dataMap.put(YouyuncaiConstants.CUST_USER_CODE, userId);
            dataMap.put(YouyuncaiConstants.CUST_GROUP_CODE, youyuncaiUserDto.getYouyuncaiGroupCode());
            dataMap.put(YouyuncaiConstants.CUST_ORG_CODE, youyuncaiUserDto.getYouyuncaiOrgCode());
            jcforReturnStatus.setData(dataMap);
            jcforReturnStatusList.add(jcforReturnStatus);

            //初始化会员等级信息
            eventBus.post(new UserRegisteredEvent(userId));
        }

        return jcforReturnStatusList;
    }


    /**
     * 根据skuId,userId获取价格接口
     * @param submittedUserSkuPriceDto  submittedUserSkuPriceDto
     * @return 返回信息
     */
    @RequestMapping(value = "/get/sku-user-price", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JcforReturnStatus> getSkuUserSync(@Valid @RequestBody SubmittedUserSkuPriceDto submittedUserSkuPriceDto, BindingResult bindingResult) {
        List<JcforReturnStatus> jcforReturnStatusList = Lists.newArrayList();

        JcforReturnStatus wholeReturnStatus = new JcforReturnStatus();
        wholeReturnStatus.setStatus(0);

        if (bindingResult.hasErrors()) {
            log.error("fail to get user sku price api sync, submittedUserSkuPriceDto:{},cause:{}",
                    submittedUserSkuPriceDto, bindingResult.getFieldError().getDefaultMessage());
            wholeReturnStatus.setErrormsg("必填项验证失败!,错误信息:" + bindingResult.getFieldError().getDefaultMessage() + ",获取sku价格失败");
            jcforReturnStatusList.add(wholeReturnStatus);

            return jcforReturnStatusList;
        }

        if (!jcforLoginVerification.isVerificationSuccess(submittedUserSkuPriceDto.getHeader().getClientId(),
                submittedUserSkuPriceDto.getHeader().getClientSecret())) {
            wholeReturnStatus.setErrormsg("集乘网认证账号或密码验证错误,获取sku价格失败!");
            log.error("[YOU-YUN-CAI]:fail to login jcfor because verification fail");
            jcforReturnStatusList.add(wholeReturnStatus);
            return jcforReturnStatusList;
        }

        List<YouyuncaiUserSkuPriceDto> youyuncaiUserSkuPriceDtos = submittedUserSkuPriceDto.getBody();

        if (Arguments.isNull(youyuncaiUserSkuPriceDtos) || youyuncaiUserSkuPriceDtos.size() > 100) {
            wholeReturnStatus.setErrormsg("数据为空或大于100条,获取sku价格失败!");
            log.error("[YOU-YUN-CAI]:fail to login jcfor because [body] is null or [body] gt 100");
            jcforReturnStatusList.add(wholeReturnStatus);
            return jcforReturnStatusList;
        }

        for (YouyuncaiUserSkuPriceDto youyuncaiUserSkuPriceDto : youyuncaiUserSkuPriceDtos) {

            JcforReturnStatus jcforReturnStatus = new JcforReturnStatus();
            YouyuncaiUserSkuPriceDto dataReturn = new YouyuncaiUserSkuPriceDto();
            jcforReturnStatus.setStatus(0);

            Long receiveShopId = DefaultId.PLATFROM_SHOP_ID;
            Response<Long> receiveRsp = receiveShopParser.findReceiveShopIdByUserId(Long.valueOf(youyuncaiUserSkuPriceDto.getCustUserCode()));
            if (receiveRsp.isSuccess()) {
                receiveShopId = receiveRsp.getResult();
            }

            dataReturn.setCustUserCode(youyuncaiUserSkuPriceDto.getCustUserCode());
            dataReturn.setSkuCode(youyuncaiUserSkuPriceDto.getSkuCode());
            Integer price = vegaSkuPriceCacher.findByKey(youyuncaiUserSkuPriceDto.getSkuCode() + "@" + receiveShopId + "@" + youyuncaiUserSkuPriceDto.getCustUserCode());
            if (!Arguments.isNull(price)) {
                jcforReturnStatus.setStatus(1);
                dataReturn.setPrice(price);
            } else {
                jcforReturnStatus.setErrormsg("系统内部错误,价格获取失败");
            }
            jcforReturnStatus.setData(objectMapper.convertValue(dataReturn, Map.class));

            jcforReturnStatusList.add(jcforReturnStatus);
        }

        return jcforReturnStatusList;
    }

    @RequestMapping(value = "/check-in", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JcforReturnStatus checkIn(@RequestBody SubmittedCheckInDto submittedCheckInDto, HttpServletResponse response) throws Exception {
        String targetUrl = redirectUrl +"/"; //默认返回首页
        JcforReturnStatus jcforReturnStatus = new JcforReturnStatus();
        jcforReturnStatus.setStatus(0);
        if (Arguments.isNull(submittedCheckInDto.getHeader())
                || Arguments.isNull(submittedCheckInDto.getBody())) {
            log.error("[YOU-YUN-CAI]:fail to check in because parameter [header] or [body] is null");
            jcforReturnStatus.setErrormsg("请求参数获取错误,check in接口调用失败!");
            return jcforReturnStatus;
        }

        YouyuncaiCheckInDto youyuncaiCheckInDto = submittedCheckInDto.getBody();
        if (Strings.isNullOrEmpty(youyuncaiCheckInDto.getBuyerCookie())
                || Strings.isNullOrEmpty(youyuncaiCheckInDto.getCustUserCode())
                || Strings.isNullOrEmpty(youyuncaiCheckInDto.getCheckoutRedirectUrl())) {
            log.error("[YOU-YUN-CAI]:fail to check in because some required filed is null");
            jcforReturnStatus.setErrormsg("请求参数有必填字段为空,check in接口调用失败!");
            return jcforReturnStatus;
        }
        Long userId = Long.valueOf(youyuncaiCheckInDto.getCustUserCode().trim());

        if (!jcforLoginVerification.isLoginSuccess(submittedCheckInDto.getHeader().getClientId(),
                submittedCheckInDto.getHeader().getClientSecret(), userId)) {
            log.error("[YOU-YUN-CAI]:fail to check in because verification fail");
            jcforReturnStatus.setErrormsg("集乘网认证账号,密码或用户Id验证错误,check in接口调用失败!");
            return jcforReturnStatus;
        }

        if (!createOrUpdatePurchaseOrder(userId, youyuncaiCheckInDto)) {
            log.error("[YOU-YUN-CAI]:fail to check in because create or update purchase order fail");
            jcforReturnStatus.setErrormsg("集乘网内部错误,check in接口调用失败!");
            return jcforReturnStatus;
        }

        if (!Strings.isNullOrEmpty(youyuncaiCheckInDto.getCheckinRedirectUrl())) {
            // // TODO: 2018/3/2 是否对地址进行合法性判断?
            targetUrl = youyuncaiCheckInDto.getCheckinRedirectUrl();
        }

        //成功,重定向到指定网页
        response.sendRedirect(redirectUrl+"/user/check?loginBy="+ userId +"&target=" + targetUrl);
        jcforReturnStatus.setStatus(1);
        return jcforReturnStatus;
    }

    @RequestMapping(value = "/check-test", method = RequestMethod.GET)
    public void checkIntest(HttpServletResponse response) throws Exception {
        String targetUrl = redirectUrl +"/"; //默认返回首页
        if (!jcforLoginVerification.isLoginSuccess("youyuncai",
                "E86364E227C62B0B", 503L)) {
            log.error("[YOU-YUN-CAI]:fail to check in because verification fail");
            return;
        }
        response.sendRedirect(redirectUrl+"/user/check?loginBy=503&target=" +targetUrl);
    }

    @RequestMapping(value = "/check-out", method = RequestMethod.POST)
    public Boolean checkOut() {
        PurchaseOrder purchaseOrder = getPurchaseOrderForYouyuncai();
        Long purchaseOrderId = purchaseOrder.getId();

        // TODO: 2018/3/5 采购单数据暂定从后端获取,待确认
        Response<List<PurchaseSkuOrder>> purchaseSkuOrderRsp = purchaseSkuOrderReadService.findByPurchaseOrderId(purchaseOrderId);
        if (!purchaseSkuOrderRsp.isSuccess()) {
            log.error("fail to find purchase sku order by purchaseOrderId = {},cause:{}", purchaseOrderId, purchaseSkuOrderRsp.getError());
            throw new JsonResponseException(purchaseSkuOrderRsp.getError());
        }

        if (Arguments.isNull(purchaseSkuOrderRsp.getResult())) {
            log.warn("purchase sku order not exist purchase order id:{}", purchaseOrderId);
            throw new JsonResponseException("purchase.sku.order.not.exist");
        }

        List<PurchaseSkuOrder> purchaseSkuOrders = purchaseSkuOrderRsp.getResult();
        // 删除数量为0的SKU
        purchaseSkuOrders.removeIf(purchaseSkuOrder1 -> purchaseSkuOrder1.getQuantity() == 0);

        List<Long> skuIds = Lists.transform(purchaseSkuOrders, PurchaseSkuOrder::getSkuId);

        List<RichSkuWithItem> richSkuWithItemList = findSkusWithItemAndShopSkuPrice(skuIds);
        Map<Long, RichSkuWithItem> richSkuWithItemMap = Maps.uniqueIndex(richSkuWithItemList, RichSkuWithItem::getSkuId);


        YouyuncaiCheckOutDto youyuncaiCheckOutDto = new YouyuncaiCheckOutDto();
        List<YouyuncaiCheckOutOrderDetailDto> orderDetailDtos = Lists.newArrayList();

        int index = 1;
        Integer amount = 0;
        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders) {
            YouyuncaiCheckOutOrderDetailDto youyuncaiCheckOutOrderDetailDto = new YouyuncaiCheckOutOrderDetailDto();

            Item item = richSkuWithItemMap.get(purchaseSkuOrder.getSkuId()).getItem();
            Sku sku = richSkuWithItemMap.get(purchaseSkuOrder.getSkuId()).getSku();
            Integer price = richSkuWithItemMap.get(purchaseSkuOrder.getSkuId()).getShopSkuPrice();

            youyuncaiCheckOutOrderDetailDto.setLineNumber(Integer.toString(index));
            youyuncaiCheckOutOrderDetailDto.setSkuCode(sku.getId().toString());

            String attrs = "";
            List<SkuAttribute> skuAttributes = sku.getAttrs();
            if (!CollectionUtils.isEmpty(skuAttributes)) {
                //规格
                for (SkuAttribute skuAttribute : skuAttributes) {
                    String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                    attrs += attr;
                }
            }
            youyuncaiCheckOutOrderDetailDto.setProductName(item.getName() + " " + attrs);
            youyuncaiCheckOutOrderDetailDto.setClassification(item.getCategoryId().toString());
            youyuncaiCheckOutOrderDetailDto.setBrand(Arguments.isNull(item.getBrandName()) ? "无" : item.getBrandName());
            youyuncaiCheckOutOrderDetailDto.setLeadTime(YouyuncaiConstants.leadTime.toString());

            Map<String, String> extraMap = item.getExtra();
            youyuncaiCheckOutOrderDetailDto.setUnitOfMeasure(Arguments.isNull(extraMap.get("unit")) ? "无" : extraMap.get("unit"));
            youyuncaiCheckOutOrderDetailDto.setQuantity(purchaseSkuOrder.getQuantity().toString());
            youyuncaiCheckOutOrderDetailDto.setNakedPrice(NumberUtils.formatPrice(price));
            youyuncaiCheckOutOrderDetailDto.setPrice(NumberUtils.formatPrice(price));
            youyuncaiCheckOutOrderDetailDto.setTaxRate(YouyuncaiConstants.taxRate);
            youyuncaiCheckOutOrderDetailDto.setProductDetailURL(redirectUrl+"/items/"+sku.getItemId());
            youyuncaiCheckOutOrderDetailDto.setImgUrl(Arguments.isNull(item.getMainImage())? "无" :"http:" + item.getMainImage().trim().replaceAll("http:", ""));

            orderDetailDtos.add(youyuncaiCheckOutOrderDetailDto);
            amount += price * purchaseSkuOrder.getQuantity();
            index ++;
        }

        Map<String, String> extraMap = purchaseOrder.getExtra();
        youyuncaiCheckOutDto.setAmount(NumberUtils.formatPrice(amount));
        youyuncaiCheckOutDto.setCookieId(extraMap.get(YouyuncaiConstants.BUYER_COOKIE));
        youyuncaiCheckOutDto.setCustUserCode(UserUtil.getCurrentUser().getId().toString());
        youyuncaiCheckOutDto.setOrderDetail(orderDetailDtos);



        YouyuncaiToken youyuncaiToken = new YouyuncaiToken();
        YouyuncaiRequest youyuncaiRequest = YouyuncaiRequest.build(youyuncaiToken);
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(3);
        YouyuncaiHeaderDto youyuncaiHeaderDto = new YouyuncaiHeaderDto();
        youyuncaiHeaderDto.setClientId(youyuncaiToken.getClientId());
        youyuncaiHeaderDto.setClientSecret(youyuncaiToken.getClientSecret());
        params.put(YouyuncaiConstants.HEADER,vegaYouyuncaiComponent.objectToJson(youyuncaiHeaderDto));
        params.put(YouyuncaiConstants.BODY,vegaYouyuncaiComponent.objectToJson(youyuncaiCheckOutDto));
        params.put(YouyuncaiConstants.CHECKOUT_REDIRECT_URL,extraMap.get(YouyuncaiConstants.CHECKOUT_REDIRECT_URL));

        log.info("[YOU-YUN-CAI]:order check out begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();

        YouyuncaiReturnStatus youyuncaiReturnStatus = youyuncaiRequest.youyuncaiApi(params, YouyuncaiApiType.ORDER_CHECK_OUT.value());

        stopwatch.stop();
        log.info("[YOU-YUN-CAI]:order check out done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return vegaYouyuncaiComponent.youyuncaiOrderReturn(youyuncaiReturnStatus, purchaseOrderId, YouyuncaiApiType.ORDER_CHECK_OUT.value());
    }

    /**
     * 友云采用户单点登录验证(不验证密码)
     * @param userId 用户Id
     * @return 用户信息
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParanaUser login(@RequestParam Long userId, HttpServletRequest request, HttpServletResponse response) {
        User user = findUserById(userId);
        if (Arguments.isNull(user)) {
            log.error("[YOU-YUN-CAI]:failed login because user not find userId={} ", userId);
            return null;
        }

        if (!jcforLoginVerification.isYouyuncaiUser(userId)) {
            log.error("[YOU-YUN-CAI]:try login user failed, userId={}, because this user is not you yun cai user", userId);
            return null;
        }

        ParanaUser paranaUser = buildParanaUser(user);
        this.eventBus.post(new LoginEvent(request, response, paranaUser));
        return paranaUser;
    }

    /**
     * 根据友云采订单请求创建集乘网订单
     * @param youyuncaiOrderDto 友云采订单请求
     * @param paranaUser 用户信息
     * @return 是否成功
     */
    private Response<Boolean> jcforOrderCreate(YouyuncaiOrderDto youyuncaiOrderDto, ParanaUser paranaUser) {
        try {
            List<YouyuncaiOrderDetailDto> orderDetails = youyuncaiOrderDto.getOrderDetail();
            if(Arguments.isNullOrEmpty(orderDetails)) {
                log.error("[YOU-YUN-CAI]:fail to order sync because total sku is null");
                return Response.fail("下单SKU信息不能为空,下单接口请求失败!");
            }

            List<Long> skuIds = Lists.transform(orderDetails, new Function<YouyuncaiOrderDetailDto, Long>() {
                @Nullable
                @Override
                public Long apply(@Nullable YouyuncaiOrderDetailDto input) {
                    if (Arguments.isNull(input) || Arguments.isNull(input.getSkuCode())) {
                        return null;
                    }
                    return Long.valueOf(input.getSkuCode().trim());
                }
            });

            Response<List<Sku>> skuRes = skuReadService.findSkusByIds(skuIds);
            if(!skuRes.isSuccess()
                    || !Objects.equals(skuIds.size(), skuRes.getResult().size())  ){
                log.error("fail to find sku  by ids {} for order paging error:{}",
                        skuIds, skuRes.getError());
                return Response.fail("下单SKU信息集乘网验证失败,下单接口请求失败!");
            }


            SubmittedOrder submittedOrder = assemblySubmittedSkusByYouyuncaiOrder(youyuncaiOrderDto);

            RichOrder richOrder = richOrderMaker.full(submittedOrder, paranaUser);

            // 虚拟一个收获地址,满足下单验证
            ReceiverInfo result = new ReceiverInfo();
            result.setId(1L);
            result.setStatus(1);
            richOrder.setReceiverInfo(result);


            Long receiveShopId;
            Response<Long> receiveRsp = receiveShopParser.findReceiveShopIdByUserId(paranaUser.getId());
            if (!receiveRsp.isSuccess()) {
                log.error("failed to get shop id userId = {}, error code:{}", paranaUser.getId(), receiveRsp.getError());
                receiveShopId = DefaultId.PLATFROM_SHOP_ID;
            } else {
                receiveShopId = receiveRsp.getResult();
            }

            /**
             * 修改sku价格为最终价格 并把供货价放在extraPrice Map中
             * 修改sku库存(由于下单不判断库存,为了兼容现有的下单检查规则这里强制把sku库存塞为下单数量确保不会出现库存不足的情况)
             *
             * 重要:友云采的订单目前固定位平台接单,这里只根据会员计算价格
             */
            changeSkuPriceAndStock(richOrder, paranaUser, receiveShopId);

            //检查用户是否具有购买资格
            orderRuleEngine.canBuy(richOrder);

            //检查用户是否可以享受对应的营销, 如果可以, 则计算营销, 需要实付的金额, 以及运费等
            charger.charge(richOrder, null);

            //计算运费,考虑到有运费营销,这个可能要放到charger.charge之前
            deliveryFeeCharger.charge(richOrder.getRichSkusByShops(),richOrder.getReceiverInfo());

            //// TODO: 2018/3/8  友云采订单全部满足包邮条件
            for (RichSkusByShop skusByShop : richOrder.getRichSkusByShops()) {
                skusByShop.setShipFee(0); // 实际邮费替换为0
                skusByShop.setFee(skusByShop.getOriginFee()); // 实际订单金额为原始金额

                // 订单状态设置为"买家友云采已付款,待平台审核"
                skusByShop.setOrderStatus(VegaOrderStatus.YC_PAID_WAIT_CHECK.getValue());
            }

            // // TODO: 2018/3/8 其他验证待定.....保持和正常下单一致

            Long jcforTotalFee = richOrder.getRichSkusByShops().get(0).getFee(); // 只有一个接单店铺
            Long youyuncaiTotalFee = (long)ArithUtil.sub(ArithUtil.mul(Double.valueOf(youyuncaiOrderDto.getTotalAmount()), 100),
                    ArithUtil.mul(Double.valueOf(Strings.isNullOrEmpty(youyuncaiOrderDto.getFreight()) ? "0" : youyuncaiOrderDto.getFreight()), 100));

            if (!Objects.equals(jcforTotalFee, youyuncaiTotalFee)) {
                // 下单总金额对比(单价*数量求和)
                log.info("[YOU-YUN-CAI]:jcfor total fee = {}, youyuncai total fee = {}", jcforTotalFee, youyuncaiTotalFee);
                log.error("[YOU-YUN-CAI]:fail to order sync because fee not equals");
                return Response.fail("下单总金额(扣除运费)验证失败,下单接口请求失败!");
            }

            Shop platformShop = getShopById(DefaultId.PLATFROM_SHOP_ID);
            Map<String, String> extra = richOrder.getExtra();
            if (CollectionUtils.isEmpty(extra)) {
                extra = Maps.newHashMap();
            }
            extra.put(SystemConstant.PLATFORM_FORM_SHOP_NAME, platformShop.getName());
            extra.put(SystemConstant.YOUYUNCAI_ORDER_ID, youyuncaiOrderDto.getOrderCode().trim()); // 友云采订单ID
            extra.put(SystemConstant.YOUYUNCAI_ORDER_FROM, SystemConstant.YOUYUNCAI_ORDER_FROM_NAME); // 友云采订单来源名称
            richOrder.setExtra(extra);

            Response<List<Long>> rOrder = orderWriteService.create(richOrder);

            if (!rOrder.isSuccess()) {
                log.error("failed to create {}, error code:{}", submittedOrder, rOrder.getError());
                return Response.fail("系统内部错误,下单接口请求失败!");
            }
            final List<Long> shopOrderIds = rOrder.getResult();

            if (!youyucaiOrderCreate(youyuncaiOrderDto, shopOrderIds)) {
                // 友云采订单创建失败,集乘网订单状态更新为"超时关闭状态"
                vegaOrderWriteService.shopOrderStatusChanged(shopOrderIds.get(0), VegaOrderStatus.NOT_PAID_PLATFORM.getValue(), VegaOrderStatus.TIMEOUT_CANCEL.getValue());
                log.error("failed to create youyuncai order, youyuncaiOrderDto = {}", youyuncaiOrderDto);
                return Response.fail("系统内部错误,下单接口请求失败!");
            }


            for (Long shopOrderId : shopOrderIds) {
                //抛出事件
                eventBus.post(new VegaOrderCreatedEvent(shopOrderId, UserRole.BUYER.name(), null));
                //短信提醒事件
                eventBus.post(new TradeSmsEvent(shopOrderId, TradeSmsNodeEnum.CREATE));
            }
            return Response.ok();
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("[YOU-YUN-CAI]:failed to create order = {}, cause:{}", youyuncaiOrderDto, Throwables.getStackTraceAsString(e));
            return Response.fail("系统内部错误,下单接口请求失败!");
        }
    }

    /**
     * 友云采订单扩展信息创建
     * @param youyuncaiOrderDto
     * @param shopOrderIds 集乘网订单Ids
     * @return 是否成功
     */
    private Boolean youyucaiOrderCreate(YouyuncaiOrderDto youyuncaiOrderDto, List<Long> shopOrderIds) {
        YouyuncaiOrder youyuncaiOrder = new YouyuncaiOrder();

        // 友云采过来的订单请求只创建一个订单
        youyuncaiOrder.setOrderId(shopOrderIds.get(0));
        youyuncaiOrder.setUserId(Long.valueOf(youyuncaiOrderDto.getCustUserCode()));
        youyuncaiOrder.setOrderCode(youyuncaiOrderDto.getOrderCode());
        youyuncaiOrder.setApprovedTime(youyuncaiOrderDto.getApprovedTime());
        youyuncaiOrder.setFreight(youyuncaiOrderDto.getFreight());
        youyuncaiOrder.setTotalAmount(youyuncaiOrderDto.getTotalAmount());
        youyuncaiOrder.setInvoiceState(youyuncaiOrderDto.getInvoiceState());
        youyuncaiOrder.setOrderPriceMode(youyuncaiOrderDto.getOrderPriceMode());
        youyuncaiOrder.setPayment(youyuncaiOrderDto.getPayment());
        youyuncaiOrder.setHasInvoiced(YouyuncaiConstants.HAS_INVOICED);
        youyuncaiOrder.setPurchaserJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getPurchaser()));
        youyuncaiOrder.setConsigneeJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getConsignee()));
        youyuncaiOrder.setDeliverAddressJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getDeliverAddress()));
        youyuncaiOrder.setInvoiceAddressJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getInvoiceAddress()));
        youyuncaiOrder.setInvoiceInfoJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getInvoiceInfo()));
        youyuncaiOrder.setInvoiceReceiverJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getInvoiceReceiver()));
        youyuncaiOrder.setOrderDetailJson(JsonMapper.nonEmptyMapper().toJson(youyuncaiOrderDto.getOrderDetail()));

        Response<Boolean> rsp = youyuncaiOrderWriteService.create(youyuncaiOrder);
        if (!rsp.isSuccess()) {
            log.error("failed to create youyuncai order, youyuncaiOrderDto = {}, error code:{}", youyuncaiOrderDto, rsp.getError());
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    /**
     * 创建或修改友云采专属采购单
     */
    private Boolean createOrUpdatePurchaseOrder(Long userId, YouyuncaiCheckInDto youyuncaiCheckInDto) {
        User user = findUserById(userId);
        if (Arguments.isNull(user)) {
            return Boolean.FALSE;
        }

        //封装采购单基础信息
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setBuyerId(userId);
        purchaseOrder.setBuyerName(Arguments.isNull(user.getName()) ? user.getMobile() : user.getName());
        purchaseOrder.setSkuQuantity(1);//正常默认为0,友云采专属采购单固定为1
        purchaseOrder.setIsTemp(Boolean.FALSE);//非临时采购单
        purchaseOrder.setName("友云采专属采购单");
        purchaseOrder.setUpdatedAt(new Date());

        Map<String, String> extraMap = Maps.newHashMap();
        extraMap.put(YouyuncaiConstants.BUYER_COOKIE, youyuncaiCheckInDto.getBuyerCookie());
        extraMap.put(YouyuncaiConstants.USER_CODE, youyuncaiCheckInDto.getUserCode());
        extraMap.put(YouyuncaiConstants.USER_NAME, youyuncaiCheckInDto.getUserName());
        extraMap.put(YouyuncaiConstants.CHECKOUT_REDIRECT_URL, youyuncaiCheckInDto.getCheckoutRedirectUrl());
        purchaseOrder.setExtra(extraMap);

        List<PurchaseOrder> purchaseOrders = getPurchaseOrders(userId);
        if (!Arguments.isNullOrEmpty(purchaseOrders)) {
            purchaseOrder.setId(purchaseOrders.get(0).getId());

            Response<Boolean> response = purchaseOrderWriteService.updatePurchaseOrder(purchaseOrder);
            if (!response.isSuccess()) {
                log.error("update purchase order :{} fail,error:{}", purchaseOrder, response.getError());
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        purchaseOrder.setCreatedAt(new Date());
        Response<Long> response = purchaseOrderWriteService.createPurchaseOrder(purchaseOrder);
        if (!response.isSuccess()) {
            log.error("create purchase order :{} fail,error:{}", purchaseOrder, response.getError());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * 获取用户友云采专属采购单
     * @return 结果
     */
    public List<PurchaseOrder> getPurchaseOrders(Long userId) {
        Response<List<PurchaseOrder>> response = purchaseOrderReadService.findByBuyerIdNotTemp(userId, 1);
        if(!response.isSuccess()){
            log.error("find purchase order by buyer id:{} fail,error:{}",userId,response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 创建用户
     * @param youyuncaiUserDto 友云采用户信息
     * @return 集乘网用户Id
     */
    private Long createUser(YouyuncaiUserDto youyuncaiUserDto) {
        String mobile = youyuncaiUserDto.getUserMobile();
        String password = youyuncaiUserDto.getUserPassword();

        try {
            // 1.用户基础表信息保存
            User user = new User();
            user.setStatus(UserStatus.NORMAL.value());
            user.setName(youyuncaiUserDto.getUserName());
            if (Strings.isNullOrEmpty(password)){
                password = mobile.substring(mobile.length() - 6);
            }
            user.setPassword(password);
            user.setMobile(mobile);
            user.setType(UserType.NORMAL.value());
            user.setRoles(Arrays.asList(UserRole.BUYER.name()));

            Response<Long> userResp = userWriteService.create(user);
            if (!userResp.isSuccess()) {
                log.error("[YOU-YUN-CAI]:failed to create an user by you yun cai mobile = ({}), cause : {}", mobile, userResp.getError());
                return null;
            }


            //2.用户详情表信息保存
            UserProfile userProfile = new UserProfile();
            userProfile.setUserId(userResp.getResult());
            userProfile.setRealName(youyuncaiUserDto.getUserName());
            userProfile.setAvatar("");

            youyuncaiUserDto.setUserPassword(null);
            youyuncaiUserDto.setUserMobile(null);
            youyuncaiUserDto.setUserName(null);
            String userProfileExtraJson = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(youyuncaiUserDto);
            userProfile.setExtraJson(userProfileExtraJson);

            Response<Boolean> userProfileResp = userProfileWriteService.createProfile(userProfile);
            if (!userProfileResp.isSuccess()) {
                log.error("[YOU-YUN-CAI]:failed to create an user profile by you yun cai mobile = ({}), cause : {}", mobile, userProfileResp.getError());
                return null;
            }

            return userResp.getResult();

        } catch (Exception e) {
            log.error("[YOU-YUN-CAI]:failed to create an user by you yun cai mobile = ({}), cause : {}", mobile, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    /**
     * 友云采下单数据转化为集乘网订单提交格式
     * @param youyuncaiOrderDto 友云采订单提交格式
     * @return 集乘网订单提交格式                         
     */
    private SubmittedOrder assemblySubmittedSkusByYouyuncaiOrder(YouyuncaiOrderDto youyuncaiOrderDto) throws Exception {
        //// TODO: 2018/3/8 字段对应关系待检查 

        SubmittedOrder submittedOrder = new SubmittedOrder();
        submittedOrder.setChannel(VegaOrderChannelEnum.YOU_YUN_CAI.value());// 订单来源为友云采
        submittedOrder.setPayType(VegaPayType.ONLINE_PAYMENT.value()); // 固定为在线支付

        SubmittedSkusByShop submittedSkusByShop = new SubmittedSkusByShop();
        submittedSkusByShop.setShopId(DefaultId.PLATFROM_SHOP_ID);

        List<YouyuncaiOrderDetailDto> orderDetails = youyuncaiOrderDto.getOrderDetail();
        List<SubmittedSku> submittedSkus = Lists.newArrayListWithExpectedSize(orderDetails.size());
        for (YouyuncaiOrderDetailDto youyuncaiOrderDetailDto : orderDetails) {
            SubmittedSku submittedSku = new SubmittedSku();
            submittedSku.setSkuId(Long.valueOf(youyuncaiOrderDetailDto.getSkuCode().trim()));
            submittedSku.setQuantity(Integer.valueOf(youyuncaiOrderDetailDto.getQuantity().trim()));
            submittedSkus.add(submittedSku);
        }

        submittedSkusByShop.setSubmittedSkus(submittedSkus);
        List<SubmittedSkusByShop> submittedSkusByShops = Lists.newArrayListWithExpectedSize(1);
        submittedSkusByShops.add(submittedSkusByShop);

        submittedOrder.setSubmittedSkusByShops(submittedSkusByShops);
        return submittedOrder;
    }

    /**
     * 修改订单价格,库存和订单状态
     *
     * @param richOrder     richOrder
     * @param user          user
     * @param receiveShopId 接单店铺id(即当前商品所属人)
     */
    private void changeSkuPriceAndStock(RichOrder richOrder, ParanaUser user, Long receiveShopId) throws Exception {


        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(user);
        for (RichSkusByShop skusByShop : richOrder.getRichSkusByShops()) {
            for (RichSku richSku : skusByShop.getRichSkus()) {
                // 订单状态设置为"买家友云采已付款,待平台审核"
                richSku.setOrderStatus(VegaOrderStatus.YC_PAID_WAIT_CHECK.getValue());
                final Sku sku = richSku.getSku();

                Response<Integer> skuPriceResp = receiveShopParser.findSkuPrice(sku.getId(), receiveShopId,
                        user.getId(), orderUserType);
                if (!skuPriceResp.isSuccess()) {
                    log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                            sku.getId(), skusByShop.getShop().getId(), user.getId(), skuPriceResp.getError());
                    throw new JsonResponseException(skuPriceResp.getError());
                }

                Map<String, Integer> extraPrice = sku.getExtraPrice();
                if(CollectionUtils.isEmpty(extraPrice)){
                    extraPrice = Maps.newHashMap();
                }
                extraPrice.put(SystemConstant.ORDER_SKU_SELLER_PRICE,sku.getPrice());//供货价
                Integer firstSellerPrice = receiveShopParser.findSkuCostPrice(sku, receiveShopId, OrderUserType.DEALER_FIRST ); // 一级经销商成本价
                Integer secondSellerPrice = receiveShopParser.findSkuCostPrice(sku, receiveShopId, OrderUserType.DEALER_SECOND ); // 二级经销商成本价
                if (!Arguments.isNull(firstSellerPrice)) {
                    extraPrice.put(SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE, firstSellerPrice);
                }
                if (!Arguments.isNull(secondSellerPrice)) {
                    extraPrice.put(SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE, secondSellerPrice);
                }
                sku.setExtraPrice(extraPrice);
                sku.setPrice(skuPriceResp.getResult());
                sku.setStockQuantity(richSku.getQuantity());
            }
        }
    }

    /**
     * 获取集乘网全场包邮金额
     */
    public String getPlatfromOrderShipFee() {

        Response<List<Promotion>> listResponse = promotionReadService.findOngoingPromotion();
        if (!listResponse.isSuccess() || Arguments.isNullOrEmpty(listResponse.getResult())) {
            log.error("fail to find platform promotionInfo or promotionInfo is null ,cause:{}", listResponse.getError());
            return YouyuncaiConstants.FREE_SHIPPING;
        }
        Promotion promotion = listResponse.getResult().get(0);

        if (!Arguments.isNull(promotion)) {
            Map<String, String> behaviorParamsMap = promotion.getBehaviorParams();
            if (behaviorParamsMap.containsKey("freeShipping") &&
                    !Arguments.isNull(behaviorParamsMap.get("freeShipping"))) {
                return NumberUtils.formatPrice(Long.valueOf(behaviorParamsMap.get("freeShipping"))) ;
            }
        }

        return YouyuncaiConstants.FREE_SHIPPING;
    }

    /**
     * 获取用户友云采专属采购单
     * @return 结果
     */
    private PurchaseOrder getPurchaseOrderForYouyuncai() {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        Response<List<PurchaseOrder>> response = purchaseOrderReadService.findByBuyerIdNotTemp(paranaUser.getId(), 1);
        if(!response.isSuccess()){
            log.error("find you yun cai purchase order by buyer id:{} fail,error:{}",paranaUser.getId(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<PurchaseOrder> purchaseOrders = response.getResult();

        if(Arguments.isNullOrEmpty(purchaseOrders)){
            log.warn("current user: {} not exist you yun cai purchase order");
            throw new JsonResponseException("purchase.order.not.exist");
        }

        return purchaseOrders.get(0);
    }

    /**
     * 根据skuIds查询带店铺sku价格及item的skus信息
     * @param skuIds skuIds
     * @return 带店铺sku价格的sku信息
     */
    private List<RichSkuWithItem> findSkusWithItemAndShopSkuPrice(List<Long> skuIds) {
        Response<List<RichSkuWithItem>> findResp = vegaSkuReadService.findSkusWithItemAndShopSkuPrice(skuIds);
        if (!findResp.isSuccess()) {
            log.error("fail to find skus with item and shop sku price by shopId=0, skuIds={},cause:{}",
                    skuIds, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }

        ParanaUser paranaUser = UserUtil.getCurrentUser();
        Long receiveShopId = DefaultId.PLATFROM_SHOP_ID;
        Response<Long> receiveRsp = receiveShopParser.findReceiveShopIdByUserId(paranaUser.getId());
        if (receiveRsp.isSuccess()) {
            receiveShopId = receiveRsp.getResult();
        }

        List<RichSkuWithItem> richSkuWithItems = findResp.getResult();
        for (RichSkuWithItem richSkuWithItem : richSkuWithItems) {
            Response<Integer> skuPriceResp = receiveShopParser.findSkuPrice(richSkuWithItem.getSkuId(), receiveShopId, paranaUser.getId(), OrderUserType.NORMAL_USER);

            if (!skuPriceResp.isSuccess()) {
                log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}", richSkuWithItem.getSkuId(), receiveShopId, paranaUser.getId(), skuPriceResp.getError());
                throw new JsonResponseException(skuPriceResp.getError());
            }

            richSkuWithItem.setShopSkuPrice(skuPriceResp.getResult());
        }


        return richSkuWithItems;
    }

    private ParanaUser buildParanaUser(User user) {
        if (java.util.Objects.equals(user.getStatus(), UserStatus.DELETED.value())) {
            throw new JsonResponseException("user.not.found");
        } else if (java.util.Objects.equals(user.getStatus(), UserStatus.FROZEN.value())) {
            throw new JsonResponseException("user.status.frozen");
        } else if (java.util.Objects.equals(user.getStatus(), UserStatus.LOCKED.value())) {
            throw new JsonResponseException("user.status.locked");
        }
        ParanaUser paranaUser = ParanaUserMaker.from(user);

        Map<String, String> tags = paranaUser.getTags();
        if (Arguments.isNull(tags)) {
            tags = Maps.newHashMap();
        }
        // 用户加入友云采标识
        tags.put(YouyuncaiConstants.USER_TAGS, VegaOrderChannelEnum.YOU_YUN_CAI.toString());
        paranaUser.setTags(tags);


        return paranaUser;
    }

    /**
     * 判断用户名是否存在
     *
     * @param mobile 用户名
     * @return 存在: true, 不存在: false
     */
    private Boolean findMobileExists(String mobile) {
        Response<User> resp = userReadService.findBy(mobile, LoginType.MOBILE);
        if (!resp.isSuccess()) {
            // 不存在
            log.error("user mobile = ({}) not exists", mobile);
            return Boolean.FALSE;
        }
        // 存在
        if (Arguments.notNull(resp.getResult())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * 查找用户信息
     *
     * @param userId 用户ID
     * @return       用户信息
     */
    private User findUserById(Long userId) {
        Response<User> resp = userReadService.findById(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find user by id = {}, cause : {}", userId, resp.getError());
            return new User(); // let it go.
        }
        return resp.getResult();
    }

    /**
     * 根据用户Id组装ParanaUser
     * @param userId 用户Id
     * @return ParanaUser
     */
    private ParanaUser getUserById(Long userId) {
        Response<User> userR = userReadService.findById(userId);
        if (!userR.isSuccess()) {
            log.error("fail to find user by id {}, error code:{}",
                    userId, userR.getError());
            return null;
        }
        return ParanaUserMaker.from(userR.getResult());
    }

    private Shop getShopById(Long shopId) {
        Response<Shop> shopRes = shopReadService.findById(shopId);
        if (!shopRes.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", shopId, shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }
        return shopRes.getResult();
    }

    /**
     * 大陆手机号码11位数，匹配格式：前三位固定格式+后8位任意数
     * 此方法中前三位格式有：
     * 13+任意数
     * 15+除4的任意数
     * 18+除1和4的任意数
     * 17+除9的任意数
     * 147
     */
    private  boolean isChinaPhoneLegal(String str) throws PatternSyntaxException {
        //String regExp = "^((13[0-9])|(15[^4])|(18[0,2,3,5-9])|(17[0-8])|(147))\\d{8}$";
        // 同前端保持一致
        String regExp = "^(13[012356789][0-9]{8}|14[579][0-9]{8}|15[012356789][0-9]{8}|17[01235678][0-9]{8}|18[0123456789][0-9]{8})$";

        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(str);
        return m.matches();
    }

}
