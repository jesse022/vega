package com.sanlux.web.front.controller.user;

/**
 * Created by liangfujie on 16/8/10
 */

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.user.dto.UserRank;
import com.sanlux.user.dto.UserRankDto;
import com.sanlux.user.dto.criteria.NotifyArticleCriteria;
import com.sanlux.user.model.NotifyArticle;
import com.sanlux.user.model.Rank;
import com.sanlux.user.service.NotifyArticleReadService;
import com.sanlux.user.service.RankReadService;
import com.sanlux.user.service.UserRankReadService;
import com.sanlux.user.service.UserRankWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.msg.exception.MsgException;
import io.terminus.msg.service.MsgService;
import io.terminus.msg.util.MsgContext;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserProfileReadService;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.iherus.codegen.qrcode.QrcodeConfig;
import org.iherus.codegen.qrcode.QrcodeGenerator;
import org.iherus.codegen.qrcode.SimpleQrcodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@RequestMapping("/api/vega/user")
public class VegaUsers {

    @RpcConsumer
    private UserReadService<User> userReadService;
    @RpcConsumer
    private UserWriteService<User> userWriteService;
    @RpcConsumer
    private RankReadService rankReadService;
    @RpcConsumer
    private UserRankReadService userRankReadService;
    @RpcConsumer
    private UserRankWriteService userRankWriteService;
    @RpcConsumer
    private ShopWriteService shopWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private NotifyArticleReadService notifyArticleReadService;
    @RpcConsumer
    private UserProfileReadService userProfileReadService;

    @Autowired
    private MsgService msgService;

    private static final String SMS_CODE_KEY = "sms-code-chg-mob";

    @Value("${m.web.domain: http://m.jcfor.com}")
    private String mobileDomain;

    /***
     * 获取当前用户的等级信息及所有等级规则
     * @param userId 用户Id
     * @return UserRankDto
     */
    @RequestMapping(value = "/my-rank", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserRankDto myRankInformation(@RequestParam("userId") Long userId) {
        Response<List<Rank>> response = rankReadService.findAll();
        if (!response.isSuccess()) {
            log.error("rank find all fail ,error{}", response.getError());
            throw new JsonResponseException("rank.find.all.fail");
        }
        Response<UserRank> resp = userRankReadService.findUserRankByUserId(userId);
        if (!resp.isSuccess()) {
            log.error("user rank find  fail ,user id{}, error{}", userId, resp.getError());
            throw new JsonResponseException("user.rank.find.fail");
        }
        UserRank userRank = resp.getResult();

        UserRankDto userRankDto = new UserRankDto();

        Shop shop = getShopById(DefaultId.PLATFROM_SHOP_ID);
        if (CollectionUtils.isEmpty(shop.getTags())) {
            userRankDto.setGrowthValueScale("");
        }else {
            userRankDto.setGrowthValueScale(shop.getTags().get(SystemConstant.GROWTH_VALUE));
        }

        if (CollectionUtils.isEmpty(shop.getTags())) {
            userRankDto.setIntegralScale("");
        }else {
            userRankDto.setIntegralScale(shop.getTags().get(SystemConstant.INTEGRAL_SCALE));
        }
        if (CollectionUtils.isEmpty(shop.getTags())) {
            userRankDto.setGrowthValueScale("");
        }else {
            userRankDto.setGrowthValueScale(shop.getTags().get(SystemConstant.GROWTH_VALUE));
        }
        List<Rank> ranks = response.getResult();
        userRankDto.setRanks(ranks);
        userRankDto.setUserRank(userRank);
        return userRankDto;
    }

    /**
     * Sanlux 修改手机号
     *
     * @param mobile  手机号
     * @param code    手机验证码
     * @param request 请求
     */
    @RequestMapping(value = "/change-mobile", method = RequestMethod.POST)
    public void changeMobile(@RequestParam String mobile, @RequestParam String code, HttpServletRequest request) {
        HttpSession session = request.getSession();
        verifySmsCode(session, SMS_CODE_KEY, code, mobile);
        Long userId = UserUtil.getUserId();
        if (userId == null) {
            log.error("failed to change mobile by userId = {}, cause current user Id is null.");
            throw new JsonResponseException("user.not.login");
        }
        doChangeMobile(userId, mobile);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public Integer getUserIntegration() {
        ParanaUser user = UserUtil.getCurrentUser();
        if (user == null || user.getId() == null) {
            log.error("user not log in");
            throw new JsonResponseException("user.not.log.in");
        }
        Response<User> userResponse = userReadService.findById(user.getId());
        if (!userResponse.isSuccess()) {
            log.error("find user by id:{} fail, cause:{}", user.getId(), userResponse.getError());
            throw new JsonResponseException(userResponse.getError());
        }
        Map<String, String> extra = userResponse.getResult().getExtra();
        if (Arguments.isNull(extra)) {
            log.error("user not normal buyer, user:{}", user);
            throw new JsonResponseException("user.not.normal.buyer");
        }
        return Integer.parseInt(extra.get("integration"));
    }


    /**
     * 修改手机号
     *
     * @param userId 用户ID
     * @param mobile 手机号
     */
    private void doChangeMobile(Long userId, String mobile) {
        User user = new User();
        user.setId(userId);
        user.setMobile(mobile);
        Response resp = userWriteService.update(user);
        if (!resp.isSuccess()) {
            log.error("change mobile for user(id={}) failed, error={}", userId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        Response<Shop> shopResp = shopReadService.findByUserId(userId);
        Shop shop = shopResp.getResult();
        // 查找不到就不用管
        if (!shopResp.isSuccess()) {
            log.warn("failed to update shop phone by userId = {}, cause user's shop is not exists.");
            return;
        }

        Shop toUpdateShop = new Shop();
        toUpdateShop.setId(shop.getId());
        toUpdateShop.setPhone(mobile);
        resp = shopWriteService.update(toUpdateShop);
        if (!resp.isSuccess()) {
            log.error("failed to update shop phone by userId = {}, mobile = {}, cause : {}",
                    userId, mobile, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
    }

    private Shop getShopById(Long shopId) {
        Response<Shop> shopResponse = shopReadService.findById(shopId);
        if (!shopResponse.isSuccess()) {
            log.error("failed to find shop by id: ({}) ,error:{}", shopId, shopResponse.getError());
            throw new JsonResponseException(500, shopResponse.getError());
        }

        return shopResponse.getResult();

    }


    /**
     * 验证手机验证码
     *
     * @param session session
     * @param codeKey key
     * @param code    验证码
     * @param mobile  手机号
     */
    private void verifySmsCode(HttpSession session, String codeKey, String code, String mobile) {
        String codeInSession = (String) session.getAttribute(codeKey);
        if (Strings.isNullOrEmpty(codeInSession)) {
            log.warn("sent sms code not in session, mobile={}", mobile);
            throw new JsonResponseException("invoke.invalid");
        }

        String expectedCode = Splitters.AT.splitToList(codeInSession).get(0);
        if (!Objects.equal(code, expectedCode)) {
            log.warn("sms code mismatch, for mobile={}", mobile);
            throw new JsonResponseException("sms.code.mismatch");
        }

        String expectedMobile = Splitters.AT.splitToList(codeInSession).get(2);
        if (!Objects.equal(mobile, expectedMobile)) {
            log.warn("mobile not match for sms code, intended={}, actual={}", expectedMobile, mobile);
            throw new JsonResponseException(400, "invoke.invalid");
        }

        session.removeAttribute(codeKey);
    }


    /**
     * 获取通知公告分页信息
     *
     * @return 通知公告分页信息
     */
    @RequestMapping(value = "/notify/articles/paging", method = RequestMethod.GET)
    public Paging<NotifyArticle> pagingNotifyArticles(NotifyArticleCriteria criteria) {

        List<String> roles = UserUtil.getCurrentUser().getRoles();
        if (roles.size() == 0) {
            log.error("user not login");
            throw new JsonResponseException("user.not.login");
        } else {
            Response<Paging<NotifyArticle>> response = null;
            criteria.setStatus(1);
            if (roles.contains("SUPPLIER")) {//供应商
               criteria.setNotifySupplier(1);
                response = notifyArticleReadService.paging(criteria);
            }
            if (roles.contains("DEALER_FIRST")) {//一级经销商
                criteria.setNotifyDealerFirst(1);
                response = notifyArticleReadService.paging(criteria);
            }
            if (roles.contains("DEALER_SECOND")) {//二级经销商
                criteria.setNotifyDealerSecond(1);
                response = notifyArticleReadService.paging(criteria);
            }
            if (Arguments.isNull(response)) {
                log.error("web paging notify article failed");
                throw new JsonResponseException("paging.notify.article.failed");
            } else {
                if (!response.isSuccess()) {
                    log.error("paging notify article failed");
                    throw new JsonResponseException("paging.notify.article.failed");
                } else {
                    return response.getResult();
                }
            }
        }
    }

    /**
     * 查看详情页面
     * @param id 公告ID
     * @return 公告详情
     */

    @RequestMapping(value = "/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public NotifyArticle detail(Long id) {
        Response<NotifyArticle> notifyArticleResponse = notifyArticleReadService.findById(id);
        if (!notifyArticleResponse.isSuccess()) {
            log.error("find notify article by id failed,cause {}", notifyArticleResponse.getError());
            throw new JsonResponseException("find.notify.article.failed");
        }
        return notifyArticleResponse.getResult();
    }

    /**
     * 获取用户二维码接口
     *
     * @param response http
     * @param type 类型 1:业务经理生成二维码;2:经销商生成二维码
     */
    @RequestMapping(value = "/get/qrCode/{type}", method = RequestMethod.GET)
    public void getUserQr(HttpServletResponse response, @PathVariable("type") Integer type) {
        final ParanaUser user = UserUtil.getCurrentUser();
        try {
            QrcodeConfig config = new QrcodeConfig()
                    .setLogoBorderSize(1)
                    .setLogoBorderColor("#B0C4DE");
            QrcodeGenerator qrcodeGenerator = new SimpleQrcodeGenerator(config);

            String avatar = getUserAvatar(user.getId());
            if (Strings.isNullOrEmpty(avatar)) {
                // 获取不到,默认用admin的头像
                avatar = getUserAvatar(DefaultId.ADMIN_USER_ID);
            }
            String urlContent = "";
            if (type == 1) {
                urlContent = "/register_by_h5?serviceId="+ user.getId() ;
            }
            if (type == 2) {
                urlContent = "/register_by_h5?shopUserId="+ user.getId() ;
            }

            String xlsFileName = URLEncoder.encode("用户二维码", "UTF-8") + ".png";
            response.setContentType("image/png");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            response.setHeader(headerKey, headerValue);
            if (Strings.isNullOrEmpty(avatar)) {
                // 不用头像
                qrcodeGenerator.generate(mobileDomain+urlContent).toStream(response.getOutputStream());
            } else {
                avatar = "http:" + avatar.trim().replaceAll("http:", "");
                qrcodeGenerator.setRemoteLogo(avatar).generate(mobileDomain+urlContent).toStream(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("get qr code failed, userId:{} ", user.getId());
            throw new JsonResponseException("get.user.qr.fail");
        }
    }


    /**
     * 改写产品用户注册获取手机验证码接口,取消前台验证码判断
     * 原接口:/api/user/register-by-mobile/send-sms
     *
     * @param mobile  用户注册手机号码
     * @param request request
     */
    @RequestMapping(value = "/register-by-mobile/send-sms", method = RequestMethod.POST)
    public void sendSms(@RequestParam String mobile, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String regCode = (String) session.getAttribute("sms-code-reg");
        failIfCannotResend(regCode);
        String code = String.valueOf((int) ((Math.random() * 9.0D + 1.0D) * 100000.0D));
        session.setAttribute("sms-code-reg", code + "@" + System.currentTimeMillis() + "@" + mobile);
        doSendSmsForRegister(mobile, code);
    }


    /**
     * 改写产品用户修改手机号码获取手机验证码接口,取消前台验证码判断
     * 原接口:/api/user/change-mobile/send-sms
     *
     * @param mobile  手机号码
     * @param request request
     */
    @RequestMapping(value = "/change-mobile/send-sms", method = RequestMethod.POST)
    public void sendSmsForChangeMobile(@RequestParam String mobile, HttpServletRequest request) {
        judgeMobile(mobile);
        HttpSession session = request.getSession();
        String chgCode = (String) session.getAttribute("sms-code-chg-mob");
        failIfCannotResend(chgCode);
        String code = String.valueOf((int) ((Math.random() * 9.0D + 1.0D) * 100000.0D));
        session.setAttribute("sms-code-chg-mob", code + "@" + System.currentTimeMillis() + "@" + mobile);
        doSendSmsForChangeMobile(mobile, code);
    }


    /**
     * 改写产品用户重置密码获取手机验证码接口,取消前台验证码判断
     * 原接口:/api/user/reset-password-by-mobile/send-sms
     *
     * @param mobile  手机号码
     * @param request request
     */
    @RequestMapping(value = "/reset-password-by-mobile/send-sms", method = RequestMethod.POST)
    public void sendSmsForResetPassword(@RequestParam String mobile, HttpServletRequest request) {
        judgeMobile(mobile);
        HttpSession session = request.getSession();
        String fgtCode = (String) session.getAttribute("sms-code-reset-pw");
        failIfCannotResend(fgtCode);
        String code = String.valueOf((int) ((Math.random() * 9.0D + 1.0D) * 100000.0D));
        session.setAttribute("sms-code-reset-pw", code + "@" + System.currentTimeMillis() + "@" + mobile);
        doSendSmsForForgetPassword(mobile, code);
    }

    private void failIfCannotResend(String code) {
        if (!Strings.isNullOrEmpty(code)) {
            List parts = Splitters.AT.splitToList(code);
            long sendTime = Long.parseLong((String) parts.get(1));
            if (System.currentTimeMillis() - sendTime < TimeUnit.MINUTES.toMillis(1L)) {
                log.error("could not send sms, sms only can be sent once in one minute");
                throw new JsonResponseException(500, "1分钟内只能获取一次验证码");
            }
        }
    }

    private void judgeMobile(String mobile) {
        if (!mobile.matches("^((13[0-9])|(14[0-9])|(15[0-9])|(17[0-9])|(18[0-9]))\\d{8}$")) {
            throw new JsonResponseException(400, "user.mobile.invalid");
        }
    }

    private void doSendSmsForRegister(String mobile, String code) {
        log.debug("sending code={} to mobile={} for registering ...", code, mobile);
        doSendSms(mobile, "sms.user.register.code", code);
    }

    private void doSendSmsForChangeMobile(String mobile, String code) {
        log.debug("sending code={} to mobile={} for changing mobile ...", code, mobile);
        doSendSms(mobile, "sms.user.change.mobile", code);
    }

    private void doSendSmsForForgetPassword(String mobile, String code) {
        log.debug("sending code={} to mobile={} for changing password by mobile", code, mobile);
        doSendSms(mobile, "sms.user.forget.password", code);
    }

    public void doSendSms(String mobile, String template, String code) {
        try {
            String result = this.msgService.send(mobile, template, MsgContext.of("code", code), null);
            log.info("sendSms result={}, mobile={}, message={}", result, mobile, code);
        } catch (MsgException e) {
            log.error("sms send failed, mobile={}, cause:{}", mobile, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("sms.send.fail");
        }
    }

    /**
     * 获取用户头像地址
     * @return 用户头像信息
     */
    private String getUserAvatar(Long userId) {
        Response<UserProfile> resp = userProfileReadService.findProfileByUserId(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find admin profile user(id={}), cause: {}", userId, resp.getError());
            return null;
        }
        return resp.getResult().getAvatar();
    }

}
