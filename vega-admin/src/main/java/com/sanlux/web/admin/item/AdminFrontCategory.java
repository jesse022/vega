package com.sanlux.web.admin.item;


import com.google.common.collect.Maps;
import com.sanlux.category.service.FrontCategoryExtReadService;
import com.sanlux.category.service.FrontCategoryExtWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.category.service.FrontCategoryReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Created by lujm on 2017/2/10.
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/frontCategory")
public class AdminFrontCategory {
    @RpcConsumer
    private FrontCategoryExtReadService frontCategoryExtReadService;

    @RpcConsumer
    private FrontCategoryExtWriteService frontCategoryExtWriteService;

    @RpcConsumer
    private FrontCategoryReadService frontCategoryReadService;

    /**类目层级移动操作
     *
     * @param categoryID 当前类目ID
     * @param upperLevelsCategoryID 移动后上级类目ID
     * @param categoryLevel 当前类目级别
     * @param hasChildren 当前类目下是否含有叶子类目
     *  总体要求:
     *                   1.所有类目不能移动到叶子类目下
     *                   2.移动后类目层级最多还是保持三级
     *                   3.移动后第三级必须为叶子类目
     *
     *  业务逻辑:
     *                   1.移动后上级类目ID"都不能为叶子类目
     *                   2.一级类目(非叶子节点)下有三级目录时不能移动；
     *                   3.一级类目(非叶子节点)的父节点不能为二级类目；
     *                   4.二级类目((非叶子节点))的父节点不能为其他二级类目；
     * @return
     */
    @RequestMapping(value = "/update", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> frontCategoryManager(@RequestParam Long categoryID,
                                                  @RequestParam Long upperLevelsCategoryID,
                                                  @RequestParam Integer categoryLevel,
                                                  @RequestParam boolean hasChildren) {
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("id", categoryID);
        criteria.put("level", categoryLevel);
        criteria.put("hasChildren", hasChildren ? "1" : "0");
        checkCategoryByIdAndLevelAndHasChildren(criteria);

        Response<List<FrontCategory>> frontCategoryRS = frontCategoryExtReadService.findCategoryByIdAndHasChildren(upperLevelsCategoryID);
        if (!frontCategoryRS.isSuccess()) {
            log.error("find category info by upperLevelsCategoryID:{} failed, cause:{}", upperLevelsCategoryID, frontCategoryRS.getError());
            throw new JsonResponseException(frontCategoryRS.getError());
        }
        List<FrontCategory> frontCategories = frontCategoryRS.getResult();
        if (CollectionUtils.isEmpty(frontCategories)&&upperLevelsCategoryID!=0) {
            //-2,"上级类目不能为叶子类目或上级类目信息无效"
            log.info("upperLevelsCategory is leaves or invalid by upperLevelsCategoryID:{}", upperLevelsCategoryID);
            throw new JsonResponseException(500,"upperLevelsCategory.is.leaves.or.invalid");
        } else {
            //上级类目为非叶子类目
            Integer NewCategoryLevel = 0;//默认为根目录
            if(upperLevelsCategoryID!=0) {
                NewCategoryLevel = frontCategories.get(0).getLevel();//移动后上级类目级别;
            }
            checkCategoryByOther(categoryID,categoryLevel,NewCategoryLevel,hasChildren);
            //更新类目信息
            Response<Boolean> updateRS=frontCategoryExtWriteService.updatePidAndLevel(upperLevelsCategoryID,NewCategoryLevel+1,categoryID);
            if (!updateRS.isSuccess()) {
                log.error("update category info by id:{}  level:{} pid:{} failed, cause:{}", categoryID,NewCategoryLevel+1,upperLevelsCategoryID, updateRS.getError());
                throw new JsonResponseException(updateRS.getError());
            }
        }
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 判断类目信息是否合法
     * @param criteria 类目信息
     */
    private void checkCategoryByIdAndLevelAndHasChildren(Map<String, Object> criteria){
        Response<Long> longResponse = frontCategoryExtReadService.checkCategoryByIdAndLevelAndHasChildren(criteria);
        if (!longResponse.isSuccess()) {
            log.error("find category info by criteria:{} failed, cause:{}", criteria, longResponse.getError());
            throw new JsonResponseException(500,"find.category.fail");
        }
        if (longResponse.getResult() < 1) {
            //-1,"传入类目信息不合法"
            log.info("find category info fail by criteria:{}",criteria);
            throw new JsonResponseException(500,"find.category.fail");
        }
    }

    /**
     * 上级类目为非叶子类目信息合法性判断
     * @param categoryID 当前类目ID
     * @param categoryLevel 当前类目级别
     * @param NewCategoryLevel 移动后类目级别
     * @param hasChildren 是否含有叶子类目
     */
    private void checkCategoryByOther(Long categoryID,Integer categoryLevel,Integer NewCategoryLevel,boolean hasChildren){
        Response<List<FrontCategory>> listResponse = frontCategoryReadService.findChildrenByPid(categoryID);
        if (!listResponse.isSuccess()) {
            log.error("find category info by categoryID:{} failed, cause:{}", categoryID, listResponse.getError());
            throw new JsonResponseException(listResponse.getError());
        }
        List<FrontCategory> listFrontCategory = listResponse.getResult();

        if (categoryLevel == 1) {
            if (!CollectionUtils.isEmpty(listFrontCategory)) {
                for (FrontCategory frontCategory : listFrontCategory) {
                    if (frontCategory.getHasChildren()) {
                        //-3, "非叶子节点一级类目下有三级目录时不能移动"
                        log.info("category can not move by categoryID:{} with category has three levels sonCategory", categoryID);
                        throw new JsonResponseException(500,"category.move.fail.with.has.three.levels.sonCategory");
                    }
                }
            }
            if (NewCategoryLevel == 2 && hasChildren) {
                //-4,"非叶子节点一级类目的父节点不能为二级类目"
                log.info("category can not move by categoryID:{} with firstCategory father node is secondLevels", categoryID);
                throw new JsonResponseException(500,"category.move.fail.with.firstCategory.father.node.is.secondLevels");
            }
        }
        if (categoryLevel == 2 && NewCategoryLevel == 2 && hasChildren) {
            //-5,"非叶子节点二级类目的父节点不能为其他二级类目"
            log.info("category can not move by categoryID:{} with secondCategory father node is secondLevels", categoryID);
            throw new JsonResponseException(500,"category.move.fail.with.secondCategory.father.node.is.secondLevels");
        }
    }
}
