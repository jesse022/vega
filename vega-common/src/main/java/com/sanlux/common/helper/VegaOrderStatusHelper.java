package com.sanlux.common.helper;

import com.google.common.base.Strings;

/**
 * 获取运营后台订单状态中文名称类
 *
 * Created by lujm on 2018/1/4.
 */
public class VegaOrderStatusHelper {
    public static String getOrderStatusName(Integer status){
        String statusName = null;

        switch (status) {
            case 0:
            case 1:
            case 2:
                statusName = "待买家支付";
                break;
            case 3:
                statusName = "买家已付款 待平台审核";
                break;
            case 4:
                statusName = "待供应商审核";
                break;
            case 5:
            case 8:
            case 10:
            case 13:
            case 14:
            case 15:
                statusName = "待发货";
                break;
            case 6:
            case 7:
                statusName = "待一级经销商审核";
                break;
            case 9:
            case 11:
            case 12:
                statusName = "待二级经销商审核";
                break;
            case 16:
                statusName = "商家已发货";
                break;
            case 17:
                statusName = "买家已确认收货";
                break;
            case 18:
            case 20:
                statusName = "待出库";
                break;
            case 19:
            case 21:
                statusName = "待出库完成";
                break;
            case 22:
                statusName = "已入库";
                break;
            case -1:
                statusName = "买家取消订单";
                break;
            case -2:
                statusName = "平台取消订单";
                break;
            case -3:
                statusName = "平台拒绝订单";
                break;
            case -4:
                statusName = "供应商拒绝订单";
                break;
            case -5:
                statusName = "一级经销商取消订单";
                break;
            case -6:
            case -7:
                statusName = "一级经销商拒绝订单";
                break;
            case -8:
            case -9:
            case -11:
                statusName = "二级经销商拒绝订单";
                break;
            case -10:
                statusName = "二级经销商取消订单";
                break;
            case -12:
            case -15:
            case -18:
            case -21:
            case -24:
            case -27:
            case -30:
            case -33:
            case -36:
            case -39:
            case -57:
                statusName = "申请退款";
                break;
            case -13:
            case -16:
            case -19:
            case -22:
            case -25:
            case -28:
            case -31:
            case -34:
            case -37:
            case -40:
            case -58:
                statusName = "同意退款";
                break;
            case -14:
            case -17:
            case -20:
            case -23:
            case -26:
            case -29:
            case -32:
            case -35:
            case -38:
            case -41:
            case -59:
                statusName = "拒绝退款";
                break;
            case -42:
                statusName = "已退款";
                break;
            case -43:
                statusName = "买家申请退货";
                break;
            case -44:
                statusName = "商家同意退货申请 等待买家退货";
                break;
            case -45:
                statusName = "商家拒绝退货申请";
                break;
            case -46:
                statusName = "买家已退货";
                break;
            case -47:
                statusName = "商家确认退货";
                break;
            case -48:
                statusName = "商家拒绝退货";
                break;
            case -49:
            case -51:
            case -52:
                statusName = "超时关闭订单";
                break;
            case -50:
                statusName = "已退款";
                break;
            case -53:
            case -55:
                statusName = "运营同意退款";
                break;
            case -54:
            case -56:
                statusName = "运营拒绝退款";
                break;
            case -99:
                statusName = "买家已删除";
                break;
        }
        return Strings.isNullOrEmpty(statusName) ? "" : statusName;
    }
}
