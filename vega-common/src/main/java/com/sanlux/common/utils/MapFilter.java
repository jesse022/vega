package com.sanlux.common.utils;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/18/16
 * Time: 5:44 PM
 */
public class MapFilter {


    /**
     * 过滤map中key为null或empty的元素
     * @param criteria map
     * @return 过滤后得map
     */
    public static Map<String, Object> filterNullOrEmpty(Map<String, Object> criteria) {
        return Maps.filterEntries(criteria, new Predicate<Map.Entry<String, Object>>() {
            @Override
            public boolean apply(Map.Entry<String, Object> entry) {
                Object v = entry.getValue();
                if (v instanceof String) {
                    return !Strings.isNullOrEmpty((String) v);
                }
                return v != null;
            }
        });
    }
}
