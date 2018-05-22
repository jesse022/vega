package com.sanlux.user.manager;


import com.sanlux.user.dto.scope.ShopUserDto;
import com.sanlux.user.impl.dao.ShopUserDao;
import com.sanlux.user.impl.dao.ShopUserExtrasDao;
import com.sanlux.user.impl.dao.UserRankResumeDao;
import com.sanlux.user.model.Rank;
import com.sanlux.user.dto.UserRank;
import com.sanlux.user.model.ShopUser;
import com.sanlux.user.model.ShopUserExtras;
import com.sanlux.user.model.UserRankResume;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Created by liangfujie on 16/8/9
 */
@Slf4j
@Component
public class ShopUserManager {

    private final UserRankResumeDao userRankResumeDao;
    private final UserDao userDao;
    private final ShopUserDao shopUserDao;
    private final ShopUserExtrasDao shopUserExtrasDao;


    @Autowired
    public ShopUserManager(UserRankResumeDao userRankResumeDao, UserDao userDao,
                           ShopUserDao shopUserDao, ShopUserExtrasDao shopUserExtrasDao) {
        this.userRankResumeDao = userRankResumeDao;
        this.userDao = userDao;
        this.shopUserDao = shopUserDao;
        this.shopUserExtrasDao = shopUserExtrasDao;
    }


    @Transactional
    public Boolean updateUserRank(UserRank userRank, Rank rank, Long operateId, String operateName) throws Exception {


        UserRankResume userRankResume = new UserRankResume();
        userRankResume.setRankName(rank.getName());
        userRankResume.setGrowthValue(rank.getGrowthValueStart());
        userRankResume.setUserId(userRank.getUserId());
        userRankResume.setUserName(userRank.getUserName());
        userRankResume.setOperateId(operateId);
        userRankResume.setOperateName(operateName);
        userRankResume.setRankId(rank.getId());
        Boolean tag = userRankResumeDao.create(userRankResume);
        if (tag) {

            User user = userDao.findById(userRank.getUserId());
            User update = new User();
            userRank.setRankId(rank.getId());
            userRank.setGrowthValue(userRankResume.getGrowthValue());
            userRank.setRankName(userRankResume.getRankName());
            String userRankJson = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(userRank);
            update.setId(user.getId());
            update.setExtraJson(userRankJson);
            return userDao.update(update);

        }

        return tag;


    }


    @Transactional
    public Boolean updateUserRank(UserRank userRank) throws Exception {

        UserRankResume userRankResume = new UserRankResume();
        userRankResume.setRankName(userRank.getRankName());
        userRankResume.setGrowthValue(userRank.getGrowthValue());
        userRankResume.setUserId(userRank.getUserId());
        userRankResume.setUserName(userRank.getUserName());
        userRankResume.setOperateId(userRank.getUserId());
        userRankResume.setOperateName(userRank.getUserName());
        userRankResume.setRankId(userRank.getRankId());
        userRankResumeDao.create(userRankResume);
        User user = new User();
        user.setId(userRank.getUserId());
        String userRankJson = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(userRank);
        user.setExtraJson(userRankJson);
        userDao.update(user);
        return Boolean.TRUE;


    }

    @Transactional
    public  Boolean addShopUser(ShopUser shopUser, ShopUserExtras shopUserExtras) {
        Boolean tag = shopUserDao.create(shopUser);
        if (tag) {
            ShopUserExtras isExist = shopUserExtrasDao.findByUserId(shopUserExtras.getUserId());
            if (!Arguments.isNull(isExist) && !Arguments.isNull(isExist.getId())) {
                // 考虑到用户信息收集问题,会员表信息删除后,会员扩展表信息不会删除
                shopUserExtras.setId(isExist.getId());
                shopUserExtrasDao.update(shopUserExtras);
            } else {
                shopUserExtrasDao.create(shopUserExtras);
            }
        }
        return tag;
    }

    @Transactional
    public  Boolean updateShopUser(ShopUser shopUser, ShopUserExtras shopUserExtras, ShopUserDto shopUserDto) {
        Boolean tag = shopUserDao.update(shopUser);
        if (tag) {
            ShopUserExtras isExist = shopUserExtrasDao.findByMobile(shopUserExtras.getMobile());
            if (!Arguments.isNull(isExist) && !Arguments.isNull(isExist.getId())) {
                shopUserExtras.setId(isExist.getId());

                shopUserExtrasDao.update(shopUserExtras);
            } else {
                // 如果扩展表没有数据,直接新增
                User user = userDao.findByMobile(shopUserExtras.getMobile());
                shopUserExtras.setUserId(user.getId());
                shopUserExtras.setUserName(user.getName());
                shopUserExtras.setMobile(shopUserDto.getMobile());
                shopUserExtras.setUserType(user.getType());
                shopUserExtras.setUserStatus(user.getStatus());
                shopUserExtras.setShopId(shopUserDto.getShopId());
                shopUserExtras.setShopName(shopUserDto.getShopName());

                shopUserExtrasDao.create(shopUserExtras);
            }
        }
        return tag;
    }

    @Transactional
    public  Boolean refreshShopUserByUserId(Long userId, User user) {
        Boolean tag = shopUserDao.refreshShopUserByUserId(userId, user.getMobile(), user.getName());
        if (tag) {
            ShopUserExtras isExist = shopUserExtrasDao.findByUserId(userId);
            if (!Arguments.isNull(isExist)) {
                // 存在时更新
                shopUserExtrasDao.refreshShopUserByUserId(userId, user.getMobile(),
                        user.getName(), user.getType(), user.getStatus());
            }
        }
        return tag;
    }
}
