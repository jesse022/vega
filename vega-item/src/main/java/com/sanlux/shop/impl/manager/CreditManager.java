package com.sanlux.shop.impl.manager;

import com.sanlux.shop.impl.dao.CreditAlterResumeDao;
import com.sanlux.shop.impl.dao.CreditRepaymentResumeDao;
import com.sanlux.shop.impl.dao.VegaShopExtraDao;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.model.CreditRepaymentResume;
import com.sanlux.shop.model.VegaShopExtra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created Date : 16/8/16
 * Author : wujianwei
 */
@Component
public class CreditManager {

    private final CreditAlterResumeDao creditAlterResumeDao;
    private final CreditRepaymentResumeDao creditRepaymentResumeDao;
    private final VegaShopExtraDao shopExtraDao;

    @Autowired
    public CreditManager(CreditAlterResumeDao creditAlterResumeDao,
                         CreditRepaymentResumeDao creditRepaymentResumeDao,
                         VegaShopExtraDao shopExtraDao) {
        this.creditAlterResumeDao = creditAlterResumeDao;
        this.creditRepaymentResumeDao = creditRepaymentResumeDao;
        this.shopExtraDao = shopExtraDao;
    }

    /**
     * 添加信用额度履历
     *
     * @param creditAlterResume 履历信息
     * @param shopExtra 店铺信息
     * @return 是否创建成功
     */
    @Transactional
    public Boolean createCreditAlterResume(CreditAlterResume creditAlterResume, VegaShopExtra shopExtra) {
            return creditAlterResumeDao.create(creditAlterResume) && shopExtraDao.update(shopExtra);
    }

    /**
     * 更新履历信息
     *
     * @param creditAlterResume 履历信息
     * @param shopExtra 店铺信息
     * @return 是否更新成功
     */
    @Transactional
    public Boolean updateCreditAlterResume(CreditAlterResume creditAlterResume,VegaShopExtra shopExtra){
        return creditAlterResumeDao.update(creditAlterResume) && shopExtraDao.update(shopExtra);
    }

    /**
     * <p>
     * 创建还款履历
     * 包含更新店铺信息, 原有履历额度信息, 还款履历
     * </p>
     * @param shopExtra 店铺信息
     * @param alterResume 额度履历信息
     * @param operatedByAdmin 额度变更履历(类型为运营操作的还款履历信息)
     * @param repaymentResume 还款履历信息
     * @return 是否更新成功
     */
    public Boolean createRepaymentResume(VegaShopExtra shopExtra,
                                         CreditAlterResume alterResume,
                                         CreditAlterResume operatedByAdmin,
                                         CreditRepaymentResume repaymentResume) {
        Boolean updateShop = shopExtraDao.updateByShopId(shopExtra);
        Boolean updateAlterResume = creditAlterResumeDao.update(alterResume);
        Boolean createPayResume = creditRepaymentResumeDao.create(repaymentResume);

        Boolean createAlterResume = true;
        if (operatedByAdmin != null) {
            createAlterResume = creditAlterResumeDao.create(operatedByAdmin);
        }

        return updateShop && updateAlterResume && createAlterResume && createPayResume;
    }

    /**
     * <p>
     * 创建还款履历
     * 包含更新店铺信息, 原有履历额度信息, 还款履历
     * </p>
     * @param alterResume 额度履历信息
     * @param operatedByAdmin 额度变更履历(类型为运营操作的还款履历信息)
     * @param repaymentResume 还款履历信息
     * @return 是否更新成功
     */
    public Boolean createRepaymentResume(CreditAlterResume alterResume,
                                         CreditAlterResume operatedByAdmin,
                                         CreditRepaymentResume repaymentResume) {
        Boolean updateAlterResume = creditAlterResumeDao.update(alterResume);
        Boolean createPayResume = creditRepaymentResumeDao.create(repaymentResume);

        Boolean createAlterResume = true;
        if (operatedByAdmin != null) {
            createAlterResume = creditAlterResumeDao.create(operatedByAdmin);
        }

        return  updateAlterResume && createAlterResume && createPayResume;
    }
}

