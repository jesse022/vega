package com.sanlux.web.front.controller.user;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.enums.ServiceManagerType;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.user.dto.criteria.ServiceManagerUserCriteria;
import com.sanlux.user.model.ServiceManager;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.user.service.*;
import com.sanlux.web.front.core.trade.VegaOrderExportExcel;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 业务经理专属会员操作Controller类
 * <p/>
 * Created by lujm on 2017/5/25.
 */
@RestController
@Slf4j
@RequestMapping("/api/service/manager/user")
public class ServiceManagerUsers {

    @RpcConsumer
    private ServiceManagerUserReadService serviceManagerUserReadService;
    @RpcConsumer
    private ServiceManagerUserWriteService serviceManagerUserWriteService;
    @RpcConsumer
    private ServiceManagerReadService serviceManagerReadService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @Autowired
    private VegaOrderExportExcel vegaOrderExportExcel;


    /**
     * 添加新的会员到业务经理用户表
     *
     * @param mobile 用户手机号
     * @return 是否添加成功
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Boolean addServiceManagerUser(@RequestParam("mobile") String mobile,
                                         @RequestParam(value = "remark", required = false) String remark) {
        Response<Optional<ServiceManager>> resp = serviceManagerReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" service manager find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        if (!resp.getResult().isPresent()) {
            log.error(" service manager find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        ServiceManager serviceManager = resp.getResult().get();


        //检查用户是否被业务经理添加
        Response<List<ServiceManagerUser>> serviceManagerUsersResponse = serviceManagerUserReadService.findListByMobile(mobile);
        if (serviceManagerUsersResponse.isSuccess()) {
            List<ServiceManagerUser> serviceManagerUsers = serviceManagerUsersResponse.getResult();
            if(serviceManagerUsers.size() > 0){
                for (ServiceManagerUser serviceManagerUser:serviceManagerUsers){

                    if(serviceManagerUser.getServiceManagerId().longValue() == serviceManager.getId()){
                        //被自己添加的提示
                        log.error("add service manager user fail because service manager user exist ,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), "service manager user exist");
                        throw new JsonResponseException("service.manager.user.exist.fail");
                    } else {
                        if (Objects.equal(serviceManager.getType(), ServiceManagerType.PLATFORM.value())) {// 平台业务经理添加
                            if (Objects.equal(serviceManagerUser.getType(), serviceManager.getType())) {
                                //被其他业务经理添加并提示该业务经理信息
                                log.error("add service manager fail because service manager user exist ,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), "service manager user exist");
                                throw new InvalidException(500, "{0}.service.manager.user.exist.fail", serviceManagerUser.getServiceManagerName());
                            }
                        } else {// 一级 二级业务经理
                            if (!Objects.equal(serviceManagerUser.getType(), ServiceManagerType.PLATFORM.value())) {
                                //被其他业务经理添加并提示该业务经理信息
                                log.error("add service manager fail because service manager user exist ,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), "service manager user exist");
                                throw new InvalidException(500, "{0}.service.manager.user.exist.fail", serviceManagerUser.getServiceManagerName());
                            }

                        }
                    }

                }
            }
        }

        Response<Boolean> response = serviceManagerUserWriteService.addServiceManagerUser(mobile, serviceManager.getId(), serviceManager.getName(), serviceManager.getType(), remark);
        if (!response.isSuccess()) {
            log.error("add service manager user fail,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/add-by-h5", method = RequestMethod.POST)
    public Boolean addServiceManagerUser(@RequestParam("mobile") String mobile,
                                         @RequestParam("userId") Long userId) {
        Response<Optional<ServiceManager>> resp = serviceManagerReadService.findByUserId(userId);
        if (!resp.isSuccess()) {
            log.error(" service manager find by userId fail,userId{},error{}", userId, resp.getError());
            log.warn("The business manager adds members through two-dimensional code fail, because:[service manager find by userId fail] ");
            return Boolean.FALSE;
        }
        if (!resp.getResult().isPresent()) {
            log.error(" service manager find by userId fail,userId{},error{}", userId, resp.getError());
            log.warn("The business manager adds members through two-dimensional code fail, because:[service manager find by userId fail] ");
            return Boolean.FALSE;
        }
        ServiceManager serviceManager = resp.getResult().get();

        //检查用户是否被业务经理添加
        Response<List<ServiceManagerUser>> serviceManagerUsersResponse = serviceManagerUserReadService.findListByMobile(mobile);
        if (serviceManagerUsersResponse.isSuccess()) {
            List<ServiceManagerUser> serviceManagerUsers = serviceManagerUsersResponse.getResult();
            if(serviceManagerUsers.size() > 0){
                for (ServiceManagerUser serviceManagerUser:serviceManagerUsers){

                    if(serviceManagerUser.getServiceManagerId().longValue() == serviceManager.getId()){
                        //被自己添加的提示
                        log.error("add service manager user fail because service manager user exist ,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), "service manager user exist");
                        log.warn("The business manager adds members through two-dimensional code fail, because:[Members already added] ");
                        return Boolean.FALSE;
                    } else {
                        if (Objects.equal(serviceManager.getType(), ServiceManagerType.PLATFORM.value())) {// 平台业务经理添加
                            if (Objects.equal(serviceManagerUser.getType(), serviceManager.getType())) {
                                //被其他业务经理添加并提示该业务经理信息
                                log.error("add service manager fail because service manager user exist ,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), "service manager user exist");
                                log.warn("The business manager adds members through two-dimensional code fail, because:[Members have been other businessManager:{} added] ", serviceManager.getName());
                                return Boolean.FALSE;
                            }
                        } else {// 一级 二级业务经理
                            if (!Objects.equal(serviceManagerUser.getType(), ServiceManagerType.PLATFORM.value())) {
                                //被其他业务经理添加并提示该业务经理信息
                                log.error("add service manager fail because service manager user exist ,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), "service manager user exist");
                                log.warn("The business manager adds members through two-dimensional code fail, because:[Members have been other businessManager:{} added] ", serviceManager.getName());
                                return Boolean.FALSE;
                            }

                        }
                    }

                }
            }
        }

        Response<Boolean> response = serviceManagerUserWriteService.addServiceManagerUser(mobile, serviceManager.getId(), serviceManager.getName(), serviceManager.getType(), null);
        if (!response.isSuccess()) {
            log.error("add service manager user fail,mobile{},serviceManagerId{},serviceManagerName{},error{}", mobile, serviceManager.getId(), serviceManager.getName(), response.getError());
            log.warn("The business manager adds members through two-dimensional code fail, because:[{}] ", response.getError());
            return Boolean.FALSE;
        }
        return response.getResult();
    }


    /**
     * 业务经理专属会员信息分页查询
     *
     * @param criteria 查询条件
     * @return 专属会员分页信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ServiceManagerUser> pagingServiceManagerUser(ServiceManagerUserCriteria criteria) {

        Response<Optional<ServiceManager>> resp = serviceManagerReadService.findByUserId(UserUtil.getUserId());
        if (!resp.isSuccess()) {
            log.error(" service manager find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        if (!resp.getResult().isPresent()) {
            log.error(" service manager find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        ServiceManager serviceManager = resp.getResult().get();

        criteria.setServiceManagerId(serviceManager.getId());
        Response<Paging<ServiceManagerUser>> response = serviceManagerUserReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("service manager user paging fail,criteria{},error{}", criteria, response.getError());
            throw new JsonResponseException("service.manager.user.paging.fail");
        }
        return response.getResult();
    }

    /**
     * 根据指定的ID删除会员
     *
     * @param id 业务经理专属会员表主键
     * @return 是否删除成功
     */
    @RequestMapping(value = "/delete", method = RequestMethod.PUT)
    public Boolean deleteShopUser(@RequestParam("id") Long id) {
        Response<Boolean> response = serviceManagerUserWriteService.delete(id);
        if (!response.isSuccess()) {
            log.error("delete service manager user  fail,id{},error{}", id, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 同步业务经理专属会员用户信息,让会员表和用户表信息同步
     *
     * @param userId 用户ID
     * @return 同步是否成功
     */

    @RequestMapping(value = "/refresh", method = RequestMethod.PUT)
    public Boolean refreshShopUser(@RequestParam("userId") Long userId) {
        Response<Boolean> response = serviceManagerUserWriteService.refreshServiceManagerUserByUserId(userId);
        if (!response.isSuccess()) {
            log.error("refresh service manager user fail,userId{},error{}", userId, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 业务经理专属会员业务统计
     *
     * @param buyerIds 会员userIds
     * @param startAt  起始时间
     * @param endAt    截止时间
     * @param response http
     */
    @RequestMapping(value = "/download-excel", method = RequestMethod.GET)
    public void downLoadOrderDetailToExcel(@RequestParam(value = "buyerIds", required = true) List<Long> buyerIds,
                                           @RequestParam(value = "startAt", required = true) String startAt,
                                           @RequestParam(value = "endAt", required = true) String endAt,
                                           HttpServletResponse response) {
        try {
            Response<Optional<ServiceManager>> resp = serviceManagerReadService.findByUserId(UserUtil.getUserId());
            if (!resp.isSuccess()) {
                log.error(" service manager find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            if (!resp.getResult().isPresent()) {
                log.error(" service manager find by userId fail,userId{},error{}", UserUtil.getUserId(), resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            String serviceManagerName = resp.getResult().get().getName();
            Map<Long, ServiceManagerUser> serviceManagerUserMap = Maps.newHashMap();
            for (Long buyerId : buyerIds) {
                ServiceManagerUser serviceManagerUser = new ServiceManagerUser();
                serviceManagerUser.setServiceManagerName(serviceManagerName);
                serviceManagerUserMap.put(buyerId, serviceManagerUser);
            }

            Response<List<ShopOrder>> shopOrderList = vegaOrderReadService.findShopOrderByBuyerIds(startDate(startAt), endDate(endAt), buyerIds);
            if (!shopOrderList.isSuccess()) {
                log.error("failed to find shop order, startAt={}, endAt={}, criteria={}, cause:{}",
                        startAt, endAt, buyerIds, shopOrderList.getError());
                throw new JsonResponseException(500, shopOrderList.getError());
            }
            List<Long> orderIds = Lists.transform(shopOrderList.getResult(), ShopOrder::getId);

            String xlsFileName = URLEncoder.encode("订单汇总", "UTF-8") + ".xlsx";
            response.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            vegaOrderExportExcel.batchGetOrderDetailTemplateFile(orderIds, serviceManagerUserMap, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of OrderDetail failed, serviceManager userId:{}, buyerIds:{}, startAt:{}, endAt:{}",
                    UserUtil.getUserId(), buyerIds, startAt, endAt);
            throw new JsonResponseException("download.OrderDetail.excel.fail");
        }
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

}
