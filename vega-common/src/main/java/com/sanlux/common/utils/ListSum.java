package com.sanlux.common.utils;

import java.util.List;

/**
 * Created by cuiwentao
 * on 16/11/1
 */
public class ListSum {


    public static Integer listSum(List<Integer> lists) {
        Integer sum = 0;
        for (Integer element : lists) {
            sum += element;
        }
        return sum;
    }
}
