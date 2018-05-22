package com.sanlux.web.front.core.youyuncai.token;

import com.google.common.base.Strings;
import com.sanlux.web.front.core.youyuncai.order.constants.YouyuncaiConstants;
import com.sanlux.web.front.core.youyuncai.token.JcforToken;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserProfileReadService;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 集乘网登录验证
 * Created by lujm on 2018/2/27.
 */
@Slf4j
@Component
public class JcforLoginVerification {

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private UserProfileReadService userProfileReadService;


    /**
     * 根据认证Id和认证密码验证是否认证成功
     *
     * @param clientId     集乘网认证Id
     * @param clientSecret 集乘网认证密码
     * @return 是否成功
     */
    public Boolean isVerificationSuccess (String clientId, String clientSecret) {
        JcforToken jcforToken = new JcforToken();

        if (Arguments.isNull(clientId) || Arguments.isNull(clientSecret)) {
            log.warn("[YOU-YUN-CAI]:fail to login jcfor because clientId or clientSecret is null");
            return Boolean.FALSE;
        }

        if (!Objects.equals(clientId, jcforToken.getClientId())) {
            log.warn("[YOU-YUN-CAI]:fail to login jcfor because clientId is error");
            return Boolean.FALSE;
        }

        if (!Objects.equals(clientSecret, jcforToken.getClientSecret())) {
            log.warn("[YOU-YUN-CAI]:fail to login jcfor because clientSecret is error");
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }


    /**
     * 判断用户是否登录成功
     * @param clientId      集乘网认证Id
     * @param clientSecret  集乘网认证密码
     * @param userId        用户Id
     * @return 是否成功
     */
    public Boolean isLoginSuccess (String clientId, String clientSecret, Long userId) {

        if (isVerificationSuccess(clientId, clientSecret)) {

            Response userResp = userReadService.findById(userId);
            if(!userResp.isSuccess()) {
                log.warn("[YOU-YUN-CAI]:try login user failed, userId={}, error={}", userId, userResp.getError());
                return Boolean.FALSE;
            }

            return isYouyuncaiUser(userId);
        }

        return Boolean.FALSE;
    }

    /**
     * 根据用户Id判断是否友云采同步过来的用户
     * @param userId 用户Id
     * @return 是否
     */
    public Boolean isYouyuncaiUser (Long userId) {
        Response<UserProfile> userProfileResp = userProfileReadService.findProfileByUserId(userId);
        if(!userProfileResp.isSuccess()) {
            log.warn("[YOU-YUN-CAI]:try login user failed, userId={}, error={}", userId, userProfileResp.getError());
            return Boolean.FALSE;
        }

        // 判断是否是友云采同步用户
        if (Arguments.isNull(userProfileResp.getResult())
                || Strings.isNullOrEmpty(userProfileResp.getResult().getExtraJson())
                || !userProfileResp.getResult().getExtraJson().contains(YouyuncaiConstants.USER_CODE)) {
            log.warn("[YOU-YUN-CAI]:try login user failed, userId={}, because this user is not you yun cai user", userId);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }



}
