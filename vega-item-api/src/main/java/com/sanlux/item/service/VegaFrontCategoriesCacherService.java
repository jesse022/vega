package com.sanlux.item.service;

import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;

import java.util.List;

/**
 * 前台类目ID获取前台类目缓存
 * Created by syf on 2017/6/29.
 */
public interface VegaFrontCategoriesCacherService {

    Response<List<FrontCategory>> findAncestorsFromCatch(Long id);

}
