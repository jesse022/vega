package com.sanlux.web.admin.user;


import com.sanlux.user.dto.criteria.NotifyArticleCriteria;
import com.sanlux.user.model.NotifyArticle;
import com.sanlux.user.service.NotifyArticleReadService;
import com.sanlux.user.service.NotifyArticleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Created by liangfujie on 16/11/7
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/notify/articles")
public class AdminNotifyArticles {

    @RpcConsumer
    private NotifyArticleWriteService notifyArticleWriteService;

    @RpcConsumer
    private NotifyArticleReadService notifyArticleReadService;

    /**
     * 创建公告
     *
     * @param notifyArticle 公告信息
     * @return 是否创建成功
     */
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Boolean create(@RequestBody NotifyArticle notifyArticle) {
        notifyArticle.setStatus(1);//默认正常发布
        Response<Long> response = notifyArticleWriteService.create(notifyArticle);
        if (!response.isSuccess()) {
            log.error("create notify article failed,cause{}", response.getError());
            throw new JsonResponseException("create.notify.article.failed");
        }
        if (Arguments.isNull(response.getResult())) {
            log.error("create notify article failed,cause{}", response.getError());
            throw new JsonResponseException("create.notify.article.failed");
        }
        return Boolean.TRUE;
    }

    /**
     * 编辑公告
     *
     * @param notifyArticle 编辑的公告信息
     * @return 是否编辑成功
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Boolean update(@RequestBody NotifyArticle notifyArticle) {

        if (Arguments.isNull(notifyArticle)) {
            log.error("notify article is null");
            throw new JsonResponseException("notify.article.null");
        }
        Response<Boolean> response = notifyArticleWriteService.update(notifyArticle);

        if (!response.isSuccess()) {
            log.error("update notify article failed , cause {}", response.getError());
            throw new JsonResponseException("notify.article.update.failed");
        }
        return response.getResult();
    }

    /**
     * 获取通知公告分页信息
     *
     * @return 通知公告分页信息
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET)
    public Paging<NotifyArticle> pagingNotifyArticles(NotifyArticleCriteria criteria) {

        List<String> roles = UserUtil.getCurrentUser().getRoles();
        if (roles.size() == 0) {
            log.error("user not login");
            throw new JsonResponseException("user.not.login");
        } else {
            Response<Paging<NotifyArticle>> response = null;
            criteria.setStatus(1);
            if (roles.contains("ADMIN") || roles.contains("OPERATOR")) {
                response = notifyArticleReadService.paging(criteria);//运营查看全部通知公告
            }
            if (Arguments.isNull(response)) {
                log.error("paging notify article failed");
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
     * 删除通知公告
     *
     * @param id 通知公告ID
     * @return 是否删除成功
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Boolean delete(Long id) {
        Response<NotifyArticle> notifyArticleResponse = notifyArticleReadService.findById(id);
        if (!notifyArticleResponse.isSuccess()) {
            log.error("find notify article by id failed,cause {}", notifyArticleResponse.getError());
            throw new JsonResponseException("find.notify.article.failed");
        }
        NotifyArticle notifyArticle = notifyArticleResponse.getResult();
        notifyArticle.setStatus(-1);
        Response<Boolean> response = notifyArticleWriteService.update(notifyArticle);
        if (!response.isSuccess()) {
            log.error("delete notify article failed,cause {}", response.getError());
            throw new JsonResponseException("delete.notify.article.failed");
        }
        return response.getResult();
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


}
