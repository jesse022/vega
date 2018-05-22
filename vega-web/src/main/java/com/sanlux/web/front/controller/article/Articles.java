package com.sanlux.web.front.controller.article;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.sanlux.web.front.core.consts.ArticleType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.article.model.Article;
import io.terminus.parana.article.service.ArticleReadService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Date: 6/23/16
 * Time: 7:10 PM
 * Author: 2016年 <a href="mailto:d@terminus.io">张成栋</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/articles")
public class Articles {
    @RpcConsumer
    private ArticleReadService articleReadService;

    @RequestMapping(value = "/by-type", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<RichTypeArticle>> listTypeArticle(@RequestParam(required = false, defaultValue = "1") Integer status) {
        try {
            // 根据状态获取文章，默认获取已发布文章
            Response<List<Article>> findAll = articleReadService.listBy(
                    Collections.<String, Object>singletonMap("status",
                                                             MoreObjects.firstNonNull(status, 1)));
            if (!findAll.isSuccess()) {
                log.error("fail to find all article, cause:{}", findAll.getError());
                return Response.fail(findAll.getError());
            }

            if (findAll.getResult().isEmpty()) {
                return Response.ok(Collections.<RichTypeArticle>emptyList());
            }

            // 按照类型归组
            ImmutableMultimap<Integer, Article> typeToArticles = FluentIterable.from(findAll.getResult())
                    .index(new Function<Article, Integer>() {
                        @Nullable
                        @Override
                        public Integer apply(@Nullable Article input) {
                            return input.getType();
                        }
                    });

            // 组装数据
            List<RichTypeArticle> richTypeArticles = Lists.newArrayList();
            for (ArticleType type: ArticleType.values()) {
                final Integer t = type.value();
                RichTypeArticle richTypeArticle = new RichTypeArticle();
                richTypeArticle.setType(t);
                richTypeArticle.setDesc(type.toString());

                if (typeToArticles.containsKey(t)) {
                    richTypeArticle.setArticles(new ArrayList<>(typeToArticles.get(t)));
                } else {
                    richTypeArticle.setArticles(Collections.<Article>emptyList());
                }

                richTypeArticles.add(richTypeArticle);
            }

            return Response.ok(richTypeArticles);
        } catch (Exception e) {
            log.error("fail to list articles by type, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("article.find.fail");
        }
    }

    /**
     * 按照分类归组的文章
     */
    @Data
    private class RichTypeArticle implements Serializable {
        private static final long serialVersionUID = 9214819884259515902L;

        Integer type;
        String desc;

        List<Article> articles;
    }
}
