/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.user;

import com.google.common.collect.Lists;
import com.sanlux.common.enums.VegaUserType;
import com.sanlux.user.dto.AdminUserDto;
import com.sanlux.user.dto.UserRank;
import com.sanlux.user.service.RankReadService;
import com.sanlux.user.service.ShopUserReadService;
import com.sanlux.user.service.UserRankReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.EncryptUtil;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Created by liangfujie on 16/8/10
 */

@Slf4j
@RestController
@RequestMapping("/api/admin/user")
public class AdminUsers {

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private UserWriteService<User> userWriteService;
    @RpcConsumer
    private UserRankReadService userRankReadService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private ShopUserReadService shopUserReadService;

    /**
     * 平台用户信息分页
     *
     * @param id          用户ID
     * @param username    用户名
     * @param email       前端不需要关注此参数
     * @param mobile      手机号
     * @param status      前端不需要关注此参数
     * @param type        前端不需要关注此参数
     * @param createdFrom 起始时间
     * @param createdTo   截止时间
     * @param pageNo      pageNo
     * @param pageSize    pageSize
     * @return Paging<AdminUserDto>
     */

    @RequestMapping(value = "/user-paging", method = RequestMethod.GET, produces = {"application/json"})
    public Paging<AdminUserDto> paging(@RequestParam(required = false) Long id, @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String email, @RequestParam(required = false) String mobile,
                                       @RequestParam(required = false) Integer status, @RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdFrom,
                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdTo,
                                       @RequestParam(required = false) Integer pageNo, @RequestParam(required = false) Integer pageSize) {
        List<AdminUserDto> lists = Lists.newArrayList();

        Date ctf = null;
        if (createdFrom != null) {
            ctf = LocalDate.fromDateFields(createdFrom).toDate();
        }
        Date ctt = null;
        if (createdTo != null) {
            ctt = LocalDate.fromDateFields(createdTo).plusDays(1).toDate();
        }
        Response<Paging<User>> resp = userReadService.paging(id, username, email, mobile, status, type, ctf, ctt, pageNo, pageSize);
        if (!resp.isSuccess()) {
            log.warn("paging user failed, error={}", resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Paging<User> paging = resp.getResult();

        Paging<AdminUserDto> adminUserDtoPaging = new Paging<AdminUserDto>();

        for (User user : paging.getData()) {
            List<String> userRole=user.getRoles();
            if (CollectionUtils.isEmpty(userRole)) {
                userRole=Lists.newArrayList();
            }
            user.setPassword(null);
            //包装数据
            AdminUserDto adminUserDto = new AdminUserDto();
            adminUserDto.setId(user.getId());
            adminUserDto.setName(user.getName());
            adminUserDto.setStatus(user.getStatus());
            adminUserDto.setEmail(user.getEmail());
            adminUserDto.setMobile(user.getMobile());
            adminUserDto.setCreatedAt(user.getCreatedAt());
            adminUserDto.setUserTypeName(getUserRoleName(userRole));//用户类型
            if (Arguments.isNull(user.getExtraJson())) {
                adminUserDto.setGrowthValue(0L);
            } else {
                String userRankJson = user.getExtraJson();
                UserRank userRank = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(userRankJson, UserRank.class);
                adminUserDto.setGrowthValue(userRank.getGrowthValue());
                adminUserDto.setIntegration(userRank.getIntegration());

            }

            lists.add(adminUserDto);
        }
        adminUserDtoPaging.setData(lists);
        adminUserDtoPaging.setTotal(paging.getTotal());

        return adminUserDtoPaging;
    }


    @RequestMapping(value = "/reset-password",method = RequestMethod.POST)
    public Boolean resetPassword(Long userId, String resetPassword) {
        List<String> roles = UserUtil.getCurrentUser().getRoles();
        if (!roles.contains("ADMIN")){
            log.error("user can not reset password,cause{}",roles);
            throw new JsonResponseException("reset.password.failed");
        }else {
            if (Arguments.isNull(userId)) {
                log.error("user id is null, userId{}", userId);
                throw new JsonResponseException("user.id.null");
            } else if (!resetPassword.matches("[\\s\\S]{6,16}")) {
                log.warn("password syntax error");
                throw new JsonResponseException(500, "user.password.6to16");
            } else {
                log.debug("user {} want to change password at {}", userId, new Date());
                Response<User> userResponse = this.userReadService.findById(userId);
                if (!userResponse.isSuccess()) {
                    log.error("find user by id failed, cause{}", userResponse.getError());
                    throw new JsonResponseException("find.user.by.id.failed");

                }
                User user = userResponse.getResult();
                User toUpdate = new User();
                toUpdate.setId(userId);
                toUpdate.setPassword(EncryptUtil.encrypt(resetPassword));
                Response result = this.userWriteService.update(toUpdate);
                if (result.isSuccess()) {
                    return (Boolean) result.getResult();
                } else {
                    log.warn("failed to change password for user id={},error code:{}", userId, result.getError());
                    throw new JsonResponseException(500, result.getError());
                }

            }
        }
    }

    /**
     * 根据用户角色获取用户类型
     * add by lujm on 2017/3/28
     * @param roles 用户角色
     * @return 用户类型
     */
    public String getUserRoleName(List<String> roles) {

        if (roles.contains(VegaUserType.SUPPLIER.name())) {
            return VegaUserType.SUPPLIER.toString();
        }
        if (roles.contains(VegaUserType.DEALER_FIRST.name())) {
            return VegaUserType.DEALER_FIRST.toString();
        }
        if (roles.contains(VegaUserType.DEALER_SECOND.name())) {
            return VegaUserType.DEALER_SECOND.toString();
        }
        if (roles.size() == 1
                && (roles.contains(VegaUserType.ADMIN.name()))) {
            return VegaUserType.ADMIN.toString();
        }
        if (roles.size() == 1
                && (roles.contains(VegaUserType.OPERATOR.name()))) {
            return VegaUserType.OPERATOR.toString();
        }
        if (roles.size() == 1
                && (roles.contains(VegaUserType.BUYER.name()))) {
            return VegaUserType.BUYER.toString();
        } else {
            return VegaUserType.OTHERS.toString();
        }
    }
}

