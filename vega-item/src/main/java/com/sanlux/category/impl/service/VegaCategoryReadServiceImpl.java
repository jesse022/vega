package com.sanlux.category.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Interner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.category.impl.dao.BackCategoryExtDao;
import com.sanlux.category.service.VegaCategoryReadService;
import com.sanlux.youyuncai.model.VegaItemSync;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.category.impl.dao.BackCategoryDao;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.model.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/8/19
 */
@Slf4j
@Service
@RpcProvider
public class VegaCategoryReadServiceImpl implements VegaCategoryReadService {


    private final BackCategoryDao backCategoryDao;

    private final BackCategoryExtDao backCategoryExtDao;

    private final ItemDao itemDao;

    @Autowired
    public VegaCategoryReadServiceImpl(BackCategoryDao backCategoryDao, ItemDao itemDao,
                                       BackCategoryExtDao backCategoryExtDao) {
        this.backCategoryDao = backCategoryDao;
        this.itemDao = itemDao;
        this.backCategoryExtDao = backCategoryExtDao;
    }


    @Override
    public Response<Long> findBackCategoryId(Long itemId) {
        try {
            Item item = itemDao.findById(itemId);
            if(Arguments.isNull(item)) {
                log.error("item find fail,itemId:{}",itemId);
                return Response.fail("item.find.fail");
            }

            BackCategory backCategory = backCategoryDao.findById(item.getCategoryId());
            while (backCategory.getLevel() != 1){
                backCategory = backCategoryDao.findById(backCategory.getPid());
            }
            return Response.ok(backCategory.getId());
        }catch (Exception e) {
            log.error("find back category id  by itemId: {} failed, cause:{}",
                     itemId, Throwables.getStackTraceAsString(e));
            return Response.fail("back.category.find.fail");
        }
    }

    @Override
    public Response<List<BackCategory>> findAncestorsByBackCategoryId (Long categoryId) {
        try {
            BackCategory category = backCategoryDao.findById(categoryId);
            if (category == null) {
                log.warn("back category(id={}) isn\'t existed.", categoryId);
                return Response.fail("category.not.exist");
            }
            List<BackCategory> tree = Lists.newArrayList();
            tree.add(category);

            while (category.getLevel() != 1) {
                category = backCategoryDao.findById(category.getPid());
                tree.add(category);
            }

            Collections.reverse(tree);
            return Response.ok(tree);
        }catch (Exception e) {
            log.error("find ancestors back category by categoryId: {} failed, cause:{}",
                    categoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("ancestors.back.category.find.fail");
        }
    }


    @Override
    public Response<BackCategory> findLeafByBackCategoryPath(List<String> path) {
        try {
            Long categoryId = 0L;
            BackCategory backCategory = new BackCategory();
            for (String childPath : path) {
                backCategory = backCategoryDao.findChildrenByName(categoryId, childPath);
                if (backCategory.getHasChildren()) {
                    categoryId = backCategory.getId();
                }
            }
            return Response.ok(backCategory);
        }catch (Exception e) {
            log.error("get back category by path:{} fail, cause:{}",
                    path, Throwables.getStackTraceAsString(e));
            return Response.fail("find.back.category.by.path.fail");
        }
    }

    @Override
    public Response<Paging<BackCategory>> pagingByStatus(Integer pageNo, Integer pageSize, Integer status) {
        try {
            Map<String, Object> criteria = Maps.newHashMap();
            if (Arguments.notNull(status)) {
                criteria.put("status", status);
            }
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            return Response.ok(backCategoryExtDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), criteria));
        } catch (Exception e) {
            log.error("failed to page back category by status = {}, cause : {}",
                    status, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.back.category.failed");
        }
    }

    @Override
    public Response<Paging<BackCategory>> pagingByNotSync(Integer pageNo, Integer pageSize, Integer status, Integer channel) {
        try {
            Map<String, Object> criteria = Maps.newHashMap();
            criteria.put("channel", Arguments.isNull(channel) ? VegaItemSync.Channel.YOU_YUN_CAI.value() : channel);
            criteria.put("type", VegaItemSync.Type.CATEGORY.value());
            if (Arguments.notNull(status)) {
                criteria.put("status", status);
            }
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            return Response.ok(backCategoryExtDao.pagingByNotSync(pageInfo.getOffset(), pageInfo.getLimit(), criteria));
        } catch (Exception e) {
            log.error("failed to page back category by status = {}, cause : {}",
                    status, Throwables.getStackTraceAsString(e));
            return Response.fail("paging.back.category.failed");
        }
    }

    @Override
    public Response<BackCategory> findBackCategoryByOutId(String outId) {
        try {
            BackCategory category = backCategoryDao.findByOuterId(outId);
            return Response.ok(category);
        }catch (Exception e) {
            log.error("find back category by outId: {} failed, cause:{}",
                    outId, Throwables.getStackTraceAsString(e));
            return Response.fail("back.category.find.fail");
        }
    }

}
