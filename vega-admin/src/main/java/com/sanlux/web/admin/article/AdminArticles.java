package com.sanlux.web.admin.article;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.web.front.core.consts.ArticleType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.article.enums.ArticleStatus;
import io.terminus.parana.article.model.Article;
import io.terminus.parana.article.service.ArticleReadService;
import io.terminus.parana.article.service.ArticleWriteService;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.DateUtil;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * 运营后台文章操作
 * <p>
 * Author  : panxin
 * Date    : 5:14 PM 3/21/16
 */
@Slf4j
@RestController
@RequestMapping("/api/article")
public class AdminArticles {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @RpcConsumer
    private ArticleWriteService articleWriteService;

    @RpcConsumer
    private ArticleReadService articleReadService;

    /**
     * 新建文章
     *
     * @param article 文章信息
     * @return 新建文章ID
     */
    @RequestMapping(value = "", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createArticle(@RequestBody Article article) {
        ParanaUser user = UserUtil.getCurrentUser();

        article.setOwnerId(user.getId());
        article.setStatus(ArticleStatus.UNPUBLISHED.value());
        article.setOwnerName(user.getName());

        Response<Long> resp = articleWriteService.create(article);
        if (!resp.isSuccess()) {
            log.error("failed to create article = {}, cause : {}", article, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 更新文章
     *
     * @param article
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateArticle(@RequestBody Article article) {
        Response<Boolean> resp = articleWriteService.update(article);
        if (!resp.isSuccess()) {
            log.error("failed to update article({}), id = {}, cause : {}", article, article.getId(), resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 修改文章状态
     *
     * @param id     文章ID
     * @param status 文章状态
     * @return 是否更新成功
     */
    @RequestMapping(value = "/status/{id}/{status}", method = RequestMethod.PUT)
    public Boolean updateStatus(@PathVariable Long id,
                                @PathVariable Integer status) {
        checkArgument(notNull(id), "article.id.empty");
        checkArgument(notNull(status), "article.status.empty");
        Response<Boolean> resp = articleWriteService.setStatus(id, status);
        if (!resp.isSuccess()) {
            log.error("failed to set article status = {}, by id = {}, cause : {}",
                    status, id, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return Boolean.TRUE;
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean deleteArticleById(@RequestParam(value = "id") Long id) {
        checkArgument(notNull(id), "article.id.empty");
        Response<Boolean> resp = articleWriteService.delete(id);
        if (!resp.isSuccess()) {
            log.error("failed to delete article by id = {}, cause : ", id, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Article> paging(@RequestParam(value = "title", required = false) String title,
                                  @RequestParam(value = "type", required = false) Integer type,
                                  @RequestParam(value = "status", required = false) Integer status,
                                  @RequestParam(value = "beginAt", required = false) String beginAt,
                                  @RequestParam(value = "endAt", required = false) String endAt,
                                  @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Map<String, Object> criteria = new HashMap<>();
        if (StringUtils.hasText(title)) {
            criteria.put("title", title);
        }
        if (StringUtils.hasText(beginAt)) {
            criteria.put("beginAt", new DateTime(DateUtil.withTimeAtStartOfDay(toDate(beginAt))).toString(TIME_FORMATTER));
        }
        if (StringUtils.hasText(endAt)) {
            criteria.put("endAt", new DateTime(DateUtil.withTimeAtEndOfDay(toDate(endAt))).toString(TIME_FORMATTER));
        }
        if (type != null) {
            criteria.put("type", type);
        }
        if (status != null) {
            criteria.put("status", status);
        }

        Response<Paging<Article>> response = articleReadService.paging(pageNo, pageSize, criteria);
        if (!response.isSuccess()) {
            log.error("fail to paging article with criteria:{},and pageNo={},pageSize={},cause:{}",
                    criteria, pageNo, pageSize, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Article findById(@PathVariable("id") Long id) {
        Response<Article> findResp = articleReadService.findById(id);
        if (!findResp.isSuccess()) {
            log.error("fail to find article by id={},cause:{}", id, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    /**
     * 获取文章分类
     *
     * @return 分类列表[{id: value, name: value}, ...]
     */
    @RequestMapping(value = "/types", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, String>> getArticleTypes() {
        List<Map<String, String>> typeList = Lists.newArrayList();
        for (ArticleType value : ArticleType.values()) {
            Map<String, String> typeMap = Maps.newHashMap();
            typeMap.put("id", value.value() + "");
            typeMap.put("name", value.toString());
            typeList.add(typeMap);
        }
        return typeList;
    }

    private Date toDate(String date){
        return DateTime.parse(date,DATE_FORMATTER).toDate();
    }
}
