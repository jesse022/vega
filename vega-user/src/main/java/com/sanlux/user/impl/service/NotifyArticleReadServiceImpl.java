package com.sanlux.user.impl.service;

import com.google.common.base.Throwables;
import com.sanlux.user.dto.criteria.NotifyArticleCriteria;
import com.sanlux.user.impl.dao.NotifyArticleDao;
import com.sanlux.user.model.NotifyArticle;
import com.sanlux.user.service.NotifyArticleReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Code generated by terminus code gen
 * Desc: 公告通知表读服务实现类
 * Date: 2016-11-07
 */
@Slf4j
@Service
@RpcProvider
public class NotifyArticleReadServiceImpl implements NotifyArticleReadService {

    private final NotifyArticleDao notifyArticleDao;

    @Autowired
    public NotifyArticleReadServiceImpl(NotifyArticleDao notifyArticleDao) {
        this.notifyArticleDao = notifyArticleDao;
    }

    @Override
    public Response<NotifyArticle> findById(Long id) {
        try {
            return Response.ok(notifyArticleDao.findById(id));
        } catch (Exception e) {
            log.error("find notifyArticle by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("notify.article.find.fail");
        }
    }

    @Override
    public Response<Paging<NotifyArticle>> paging(NotifyArticleCriteria criteria) {
        try {
            return Response.ok(notifyArticleDao.paging(criteria.toMap()));
        } catch (Exception e) {
            log.error("paging notifyArticle  failed, criteria{}, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("notify.article.paging.fail");
        }
    }


}
