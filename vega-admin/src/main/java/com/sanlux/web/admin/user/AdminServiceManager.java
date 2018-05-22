package com.sanlux.web.admin.user;

import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.ServiceManagerType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.trade.dto.ServiceManagerOrderDto;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.user.dto.ServiceManagerDto;
import com.sanlux.user.dto.criteria.ServiceManagerCriteria;
import com.sanlux.user.dto.criteria.ServiceManagerUserCriteria;
import com.sanlux.user.model.ServiceManager;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.user.service.ServiceManagerReadService;
import com.sanlux.user.service.ServiceManagerUserReadService;
import com.sanlux.user.service.ServiceManagerWriteService;
import com.sanlux.web.front.core.trade.VegaOrderExportExcel;
import com.sanlux.web.front.core.trade.VegaServiceManagerOrderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserStatus;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.web.core.events.user.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.*;
import java.util.Objects;

/**
 * 业务经理信息管理Controller
 *
 * Created by lujm on 2017/5/23.
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/service/manager/user")
public class AdminServiceManager {

    @RpcConsumer
    private ServiceManagerReadService serviceManagerReadService;
    @RpcConsumer
    private ServiceManagerWriteService serviceManagerWriteService;
    @RpcConsumer
    private ServiceManagerUserReadService serviceManagerUserReadService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @Autowired
    private VegaOrderExportExcel vegaOrderExportExcel;
    @Autowired
    private VegaServiceManagerOrderComponent vegaServiceManagerOrderComponent;
    @Autowired
    private EventBus eventBus;

    /**
     * 创建用户(业务经理表/用户表)
     *
     * @param mobile 手机号码
     * @param userName 用户名
     * @param name 业务经理姓名
     * @param password 用户密码
     * @return 用户ID
     */
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public Long createServiceManagerUser(@RequestParam String mobile,
                               @RequestParam String userName,
                               @RequestParam String name,
                               @RequestParam String password) {
        //用户表信息
        User user = new User();
        user.setStatus(UserStatus.NORMAL.value());
        user.setName(userName);
        user.setPassword(password);
        user.setMobile(mobile);
        user.setType(UserType.NORMAL.value());
        user.setRoles(Arrays.asList(UserRole.BUYER.name(), VegaUserRole.SERVICE_MANAGER.name()));

        //业务经理表信息
        ServiceManager serviceManager = new ServiceManager();
        serviceManager.setName(name);//业务经理姓名
        serviceManager.setShopId(DefaultId.PLATFROM_SHOP_ID);
        serviceManager.setShopName(findShopById(DefaultId.PLATFROM_SHOP_ID).getName());
        serviceManager.setType(ServiceManagerType.PLATFORM.value());

        Response<Long> resp = serviceManagerWriteService.create(user, serviceManager);
        if (!resp.isSuccess()) {
            log.error("failed to create service manager userName = ({}),mobile = ({}),name = ({}) cause : {}",
                    userName, mobile, name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        Response<Optional<ServiceManager>> optionalResponse = serviceManagerReadService.findById(resp.getResult());
        if (!optionalResponse.isSuccess()) {
            log.error("failed to find service manager user by id = {}, cause : {}", resp.getResult(), optionalResponse.getError());
        }
        if(optionalResponse.getResult().isPresent()){
            //新建业务经理时,作为普通买家身份需要初始化会员等级信息
            eventBus.post(new UserRegisteredEvent(optionalResponse.getResult().get().getUserId()));
        }


        return resp.getResult();
    }


    /**
     * 修改用户(业务经理表/用户表)
     *
     * @param id 业务经理信息表主键Id
     * @param mobile 手机号码
     * @param userName 用户名
     * @param name 业务经理姓名
     * @param password 用户密码
     * @return 是否成功
     */
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public Boolean updateServiceManagerUser(@RequestParam Long id,
                                            @RequestParam Long userId,
                                            @RequestParam(required = false) String mobile,
                                            @RequestParam(required = false) String userName,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) String password) {
        //用户表信息
        User user = new User();
        if(!Strings.isNullOrEmpty(password)) {
            user.setPassword(password);
        }
        user.setId(userId);



        //业务经理表信息
        ServiceManager serviceManager = new ServiceManager();
        serviceManager.setId(id);
        if(!Strings.isNullOrEmpty(mobile)) {
            serviceManager.setMobile(mobile);
        }
        if(!Strings.isNullOrEmpty(userName)) {
            serviceManager.setUserName(userName);
        }
        if(!Strings.isNullOrEmpty(name)) {
            serviceManager.setName(name);
        }


        Response<Boolean> resp = serviceManagerWriteService.update(user, serviceManager);
        if (!resp.isSuccess()) {
            log.error("failed to update service manager id = ({}),userName = ({}),mobile = ({}),name = ({}) cause : {}",
                    id, userName, mobile, name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 修改用户状态(业务经理表/用户表)
     *
     * @param id 业务经理信息表主键Id
     * @param userId 用户Id
     * @param status 用户状态
     * @return 是否成功
     */
    @RequestMapping(value = "/update/status",method = RequestMethod.POST)
    public Boolean updateStatus(@RequestParam Long id,
                                @RequestParam Long userId,
                                @RequestParam Integer status) {
        if(!(Objects.equals(status, UserStatus.NORMAL.value()) || Objects.equals(status, UserStatus.FROZEN.value()))){
            log.error("failed to update service manager id = ({}),userId = ({}),status = ({}) cause : {}", id, userId, status);
            throw new JsonResponseException(500, "status.not.available");

        }
        Response<Boolean> resp = serviceManagerWriteService.updateStatus(id, userId, status);
        if (!resp.isSuccess()) {
            log.error("failed to update service manager id = ({}),userId = ({}),status = ({}) cause : {}",
                    id, userId, status, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }



    /**
     * 业务经理信息表分页查询
     *
     * @param criteria 分页查询条件
     * @return 分页信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ServiceManager> vegaUserPaging(ServiceManagerCriteria criteria) {
        if(Objects.nonNull(criteria.getId())) {
            if (!Strings.isNullOrEmpty(criteria.getId().toString())) {
                Response<Optional<ServiceManager>> userResp = serviceManagerReadService.findById(criteria.getId());
                if (!userResp.isSuccess()) {
                    // 查不到用户返回empty, 不报500
                    log.error("failed to find service manager by id = {}, cause : {}",
                            criteria.getId(), userResp.getError());
                    return Paging.empty();
                }
                if(userResp.getResult().isPresent()) {
                    return new Paging<>(1L, ImmutableList.of(userResp.getResult().get()));
                }else {
                    return new Paging<>(0L, Collections.emptyList());
                }
            }
        }
        Response<Paging<ServiceManager>> resp = serviceManagerReadService.paging(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging service manager by criteria : ({}), cause : {}", criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }


    /**
     * 业务经理提成功能查询接口
     * @param criteria 业务经理查询条件
     * @param startAt 统计开始时间
     * @param endAt 统计结束时间
     * @return 查询结果
     */
    @RequestMapping(value = "/paging-service-manager-commission", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ServiceManagerDto> pagingServiceManagerCommission(ServiceManagerCriteria criteria,
                                                                    @RequestParam(value = "startAt", required = true) String startAt,
                                                                    @RequestParam(value = "endAt", required = true) String endAt) {
        if(Objects.nonNull(criteria.getId())) {
            if (!Strings.isNullOrEmpty(criteria.getId().toString())) {
                Response<Optional<ServiceManager>> userResp = serviceManagerReadService.findById(criteria.getId());
                if (!userResp.isSuccess()) {
                    // 查不到用户返回empty, 不报500
                    log.error("failed to find service manager by id = {}, cause : {}",
                            criteria.getId(), userResp.getError());
                    return Paging.empty();
                }
                if(userResp.getResult().isPresent()) {
                    List<Long> ServicManagerIds = Lists.newArrayList();
                    ServicManagerIds.add(criteria.getId());
                    List<ServiceManagerOrderDto> serviceManagerOrderDtos = vegaServiceManagerOrderComponent.getServiceManagerOrder(ServicManagerIds, startAt, endAt);
                    ServiceManagerDto serviceManagerDto = new ServiceManagerDto();
                    serviceManagerDto.setServiceManager(userResp.getResult().get());
                    serviceManagerDto.setOldMemberCommission(serviceManagerOrderDtos.size() > 0 ?
                            serviceManagerOrderDtos.get(0).getOldMemberCommission() :0L);
                    serviceManagerDto.setNewMemberCommission(serviceManagerOrderDtos.size() > 0 ?
                            serviceManagerOrderDtos.get(0).getNewMemberCommission() :0L);
                    serviceManagerDto.setTotalMemberCommission(serviceManagerOrderDtos.size() > 0 ?
                            serviceManagerOrderDtos.get(0).getTotalMemberCommission() :0L);
                    serviceManagerDto.setStartAt(startAt);
                    serviceManagerDto.setEndAt(endAt);
                    return new Paging<>(1L, ImmutableList.of(serviceManagerDto));
                }
                return new Paging<>(0L, Collections.emptyList());
            }
        }
        if (Arguments.isNull(criteria.getShopId())) {
            criteria.setShopId(DefaultId.PLATFROM_SHOP_ID); // 默认查询平台店铺
        }
        Response<Paging<ServiceManager>> resp = serviceManagerReadService.paging(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging service manager by criteria : ({}), cause : {}", criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        List<Long> ServicManagerIds = Lists.transform(resp.getResult().getData(), ServiceManager::getId);
        List<ServiceManagerOrderDto> serviceManagerOrderDtos = vegaServiceManagerOrderComponent.getServiceManagerOrder(ServicManagerIds, startAt, endAt);
        Map<Long, ServiceManagerOrderDto> serviceManagerOrderMap = Maps.uniqueIndex(serviceManagerOrderDtos, ServiceManagerOrderDto::getServiceManagerId);

        List<ServiceManagerDto> serviceManagerDtos = Lists.newArrayList();

        for (ServiceManager serviceManager : resp.getResult().getData()) {
            ServiceManagerDto serviceManagerDto = new ServiceManagerDto();
            serviceManagerDto.setServiceManager(serviceManager);
            serviceManagerDto.setOldMemberCommission(!Arguments.isNull(serviceManagerOrderMap.get(serviceManager.getId())) ?
                    serviceManagerOrderMap.get(serviceManager.getId()).getOldMemberCommission() : 0L);
            serviceManagerDto.setNewMemberCommission(!Arguments.isNull(serviceManagerOrderMap.get(serviceManager.getId())) ?
                    serviceManagerOrderMap.get(serviceManager.getId()).getNewMemberCommission() : 0L);
            serviceManagerDto.setTotalMemberCommission(!Arguments.isNull(serviceManagerOrderMap.get(serviceManager.getId())) ?
                    serviceManagerOrderMap.get(serviceManager.getId()).getTotalMemberCommission() : 0L);
            serviceManagerDto.setStartAt(startAt);
            serviceManagerDto.setEndAt(endAt);
            serviceManagerDtos.add(serviceManagerDto);
        }

        return new Paging<>(resp.getResult().getTotal(), serviceManagerDtos);
    }


    /**
     * 根据ID查找业务经理表信息
     *
     * @param id 主键Id
     * @return 业务经理表信息
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceManager findById(@PathVariable(value = "id") Long id) {
        Response<Optional<ServiceManager>> resp = serviceManagerReadService.findById(id);
        if (!resp.isSuccess()) {
            log.error("failed to find service manager user by id = {}, cause : {}", id, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        if(resp.getResult().isPresent()){
            return resp.getResult().get();
        }
        return null;
    }

    /**
     * 业务经理专属会员信息分页查询
     *
     * @param serviceManagerId 业务经理Id
     * @param userName 专属会员用户Id
     * @param mobile 专属会员用户名称
     * @return 业务经理专属会员信息
     */
    @RequestMapping(value = "/paging-users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ServiceManagerUser> pagingServiceManagerUser(@RequestParam(value = "serviceManagerId", required = true) Long serviceManagerId,
                                                               @RequestParam(value = "userName", required = false) String userName,
                                                               @RequestParam(value = "mobile", required = false) String mobile,
                                                               @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
                                                               @RequestParam(value = "pageSize", required = false , defaultValue = "20") Integer pageSize) {
        ServiceManagerUserCriteria criteria = new ServiceManagerUserCriteria();
        criteria.setServiceManagerId(serviceManagerId);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        if (!Strings.isNullOrEmpty(userName)) {
            criteria.setUserName(userName);
        }
        if (!Strings.isNullOrEmpty(mobile)) {
            criteria.setMobile(mobile);
        }
        Response<Paging<ServiceManagerUser>> response = serviceManagerUserReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("service manager user paging fail,criteria{},error{}", criteria, response.getError());
            throw new JsonResponseException("service.manager.user.paging.fail");
        }
        return response.getResult();
    }

    /**
     * 业务经理专属会员业务统计
     *
     * @param serviceManagerIds 业务经理Ids
     * @param startAt 起始时间
     * @param endAt 截止时间
     * @param response http
     */
    @RequestMapping(value = "/download-excel", method = RequestMethod.GET)
    public void downLoadOrderDetailToExcel(@RequestParam(value = "serviceManagerIds", required = true) List<Long> serviceManagerIds,
                                           @RequestParam(value = "startAt", required = true) String startAt,
                                           @RequestParam(value = "endAt", required = true) String endAt,
                                           HttpServletResponse response) {
        try {
            Response<List<ServiceManagerUser>> listResponse = serviceManagerUserReadService.findByServiceManagerIds(serviceManagerIds);
            if (!listResponse.isSuccess()) {
                log.error("failed to find service manager user by serviceManagerIds = {}, cause : {}", serviceManagerIds, listResponse.getError());
                throw new JsonResponseException(500, listResponse.getError());
            }

            List<Long> buyerIds = Lists.transform(listResponse.getResult(), ServiceManagerUser::getUserId);

            Map<Long, ServiceManagerUser> serviceManagerUserMap = Maps.uniqueIndex(listResponse.getResult(), ServiceManagerUser::getUserId);

            List<Long> orderIds = Lists.newArrayList();
            if (!Arguments.isNullOrEmpty(buyerIds)) {
                Response<List<ShopOrder>> shopOrderList = vegaOrderReadService.findShopOrderByBuyerIds(startDate(startAt), endDate(endAt), buyerIds);
                if (shopOrderList.isSuccess()) {
                    orderIds = Lists.transform(shopOrderList.getResult(), ShopOrder::getId);
                } else {
                    log.error("failed to find shop order, startAt={}, endAt={}, criteria={}, cause:{}",
                            startAt, endAt, buyerIds, shopOrderList.getError());
                    throw new JsonResponseException(500, shopOrderList.getError());
                }
            }

            String xlsFileName = URLEncoder.encode("订单汇总", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            vegaOrderExportExcel.batchGetOrderDetailTemplateFile(orderIds, serviceManagerUserMap, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of OrderDetail failed, serviceManagerIds:{}, startAt:{}, endAt:{}",
                    serviceManagerIds, startAt, endAt);
            throw new JsonResponseException("download.OrderDetail.excel.fail");
        }
    }

    /**
     * 根据店铺订单号查询业务经理信息
     * @param shopOrderId 店铺订单号
     * @return 业务经理信息
     */
    @RequestMapping(value = "/find-by-order/{shopOrderId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ServiceManager> findByOrderId(@PathVariable(value = "shopOrderId") Long shopOrderId) {
        Response<ShopOrder> shopOrderResp = shopOrderReadService.findById(shopOrderId);
        if (!shopOrderResp.isSuccess()) {
            log.error("fail to find shop order by id:{},cause:{}", shopOrderId, shopOrderResp.getError());
            return Collections.emptyList();
        }

        List<ServiceManagerUser> serviceManagerUsers = findShopUserServiceManager(shopOrderResp.getResult().getBuyerId());
        if (Objects.isNull(serviceManagerUsers)) {
            return Collections.emptyList();
        }
        List<Long> serviceManagerIds = Lists.transform(serviceManagerUsers, ServiceManagerUser::getServiceManagerId);

        Response<List<ServiceManager>> resp = serviceManagerReadService.findByIds(serviceManagerIds);
        if (resp.isSuccess()) {
            return resp.getResult();
        }
        log.error("failed to find service manager user by ids = {}, cause : {}", serviceManagerIds, resp.getError());
        return Collections.emptyList();

    }

    /**
     * 根据用户Id获取会员所属业务经理信息
     * @param userId 会员用户Id
     * @return 所属业务经理信息
     */
    private List<ServiceManagerUser> findShopUserServiceManager(Long userId) {
        Response<List<ServiceManagerUser>> serviceManagerUserResponse = serviceManagerUserReadService.findByUserId(userId);
        if (!serviceManagerUserResponse.isSuccess()) {
            log.error("find shop user service manager by user id: ({}) fail ,error:{}", userId, serviceManagerUserResponse.getError());
            return null;
        }
        return serviceManagerUserResponse.getResult();
    }

    /**
     * 截止时间
     *
     * @param endAt 截止时间
     * @return date
     */
    private Date endDate(String endAt) {
        return Strings.isNullOrEmpty(endAt) ? null : DateTime.parse(endAt).plusDays(1).toDate();
    }

    /**
     * 起始时间
     *
     * @param startAt 起始时间
     * @return date
     */
    private Date startDate(String startAt) {
        return Strings.isNullOrEmpty(startAt) ? null : DateTime.parse(startAt).toDate();
    }

    /**
     * 查找店铺
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shopId) {
        Response<Shop> resp = shopReadService.findById(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by id = {}, cause : {}", shopId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

}
