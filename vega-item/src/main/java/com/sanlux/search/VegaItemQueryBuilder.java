package com.sanlux.search;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.search.item.impl.DefaultItemQueryBuilder;
import io.terminus.search.api.query.Aggs;
import io.terminus.search.api.query.Sort;
import io.terminus.search.api.query.Term;
import io.terminus.search.api.query.Terms;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Created by cuiwentao
 * on 16/12/8
 */
@Component
public class VegaItemQueryBuilder extends DefaultItemQueryBuilder {
    private static final Splitter DOLLAR_SPLITTER = Splitter.on('$').omitEmptyStrings().trimResults();
    @Value("${item.search.attributes-size: 1000}")
    private Integer attributesSize;
    @Value("${item.search.brand-id-size: 20}")
    private Integer brandIdSize;
    @Value("${item.search.category-id-size: 200}")
    private Integer categoryIdsSize;



    @Override
    public List<Term> buildTerm(Map<String, String> params) {
        List<Term> termList = Lists.newArrayList();

        String bid = params.get("bid");
        if(StringUtils.hasText(bid)) {
            termList.add(new Term("brandId", bid));
        }

        String shopId = params.get("shopId");
        if(StringUtils.hasText(shopId)) {
            termList.add(new Term("shopId", shopId));
        }

        String shopCatId = params.get("shopCatId");
        if(StringUtils.hasText(shopCatId)) {
            termList.add(new Term("shopCategoryIds", shopCatId));
        }

        /**
         * 搜索引擎增加itemCode(商品编码)字段
         * add by lujm on 2016/12/28
         */
        String itemCode = params.get("itemCode");
        if(StringUtils.hasText(itemCode)) {
            termList.add(new Term("itemCode", itemCode));
        }

        return termList;
    }

    /**
     * 改写buildTerms方法,增加statuses查询条件,搜索引擎支持多状态查询
     * add by lujm on 2016/12/29
     */
    @Override
    public List<Terms> buildTerms(Map<String, String> params) {
        List<Terms> termsList = super.buildTerms(params);
        String statuses = params.get("statuses");
        if(StringUtils.hasText(statuses)) {
            termsList.add(new Terms("status", Splitters.COMMA.splitToList(statuses)));
        }
        return termsList;
    }

    /**
    * 改写buildSort方法,增加按商品名称排序条件
    * add by lujm on 2017/01/12
    * 排序参数格式:"0_0_0_0_0_0",新增第4位用于判断商品名称排序条件,第5位用于商品编码排序
    */
    @Override
    public List<Sort> buildSort(Map<String, String> params) {
        List<Sort> sorts = super.buildSort(params);
        String sort = params.get("sort");
        if(!Strings.isNullOrEmpty(sort)) {
            List<String> parts = Splitters.UNDERSCORE.splitToList(sort);
            if(parts.size()>=4) {
                String itemName = Iterables.get(parts, 4, "0");
                if (!Strings.isNullOrEmpty(itemName)&&itemName.equals("1")) {
                    sorts.add(new Sort("name.raw", "asc"));
                } else if (!Strings.isNullOrEmpty(itemName)&&itemName.equals("2")) {
                    sorts.add(new Sort("name.raw", "desc"));
                }
            }
            if(parts.size()>=5) {
                String itemCode = Iterables.get(parts, 5, "0");
                if (!Strings.isNullOrEmpty(itemCode)&&itemCode.equals("1")) {
                    sorts.add(new Sort("itemCode", "asc"));
                } else if (!Strings.isNullOrEmpty(itemCode)&&itemCode.equals("2")) {
                    sorts.add(new Sort("itemCode", "desc"));
                }
            }
        }
        return sorts;
    }

    @Override
    public List<Aggs> buildAggs(Map<String, String> params) {
        String aggs = params.get("aggs");
        if(!StringUtils.hasText(aggs)) {
            return null;
        } else {
            List<String> aggSpecifiers = DOLLAR_SPLITTER.splitToList(makeAggSpecifiers(aggs));
            List<Aggs> result = Lists.newArrayListWithCapacity(aggSpecifiers.size());

            for (String aggSpecifier : aggSpecifiers) {
                List<String> parts = Splitters.COLON.splitToList(aggSpecifier);
                Aggs agg = new Aggs(parts.get(0), parts.get(1), Integer.parseInt(parts.get(2)));
                result.add(agg);
            }
            return result;
        }
    }

    /**
     * 重写搜索引擎获取聚合查询参数方法
     *
     * @param aggs 原参数
     * @return 新参数
     */
    private String makeAggSpecifiers(String aggs) {
        StringBuilder sb = new StringBuilder("attr_aggs:attributes:" + attributesSize);
        if (aggs.contains("brandId")) {
            sb.append("$brand_aggs:brandId:").append(brandIdSize);
        }
        sb.append("$cat_aggs:categoryIds:").append(categoryIdsSize);
        return sb.toString();
    }

}
