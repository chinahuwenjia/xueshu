package com.ruoyi.common.utils;

import java.util.HashMap;
import java.util.Map;

public class ParcelStatusMapper {
    private static final Map<Integer, String> statusMap = new HashMap<>();

    static {
        statusMap.put(1, "揽收");
        statusMap.put(101, "已下单");
        statusMap.put(102, "待揽收");
        statusMap.put(103, "已揽收");
        statusMap.put(0, "在途");
        statusMap.put(1001, "到达派件城市");
        statusMap.put(1002, "干线");
        statusMap.put(1003, "转递");
        statusMap.put(5, "派件");
        statusMap.put(501, "投柜或驿站");
        statusMap.put(3, "签收");
        statusMap.put(301, "本人签收");
        statusMap.put(302, "派件异常后签收");
        statusMap.put(303, "代签");
        statusMap.put(304, "投柜或站签收");
        statusMap.put(6, "退回");
        statusMap.put(4, "退签");
        statusMap.put(401, "已销单");
        statusMap.put(14, "拒签");
        statusMap.put(7, "转投");
        statusMap.put(2, "疑难");
        statusMap.put(201, "超时未签收");
        statusMap.put(202, "超时未更新");
        statusMap.put(203, "拒收");
        statusMap.put(204, "派件异常");
        statusMap.put(205, "柜或驿站超时未取");
        statusMap.put(206, "无法联系");
        statusMap.put(207, "超区");
        statusMap.put(208, "滞留");
        statusMap.put(209, "破损");
        statusMap.put(210, "销单");
        statusMap.put(8, "清关");
        statusMap.put(10, "待清关");
        statusMap.put(11, "清关中");
        statusMap.put(12, "已清关");
        statusMap.put(13, "清关异常");
        statusMap.put(14, "拒签");
    }

    public static String getStatusDescription(int code) {
        return statusMap.getOrDefault(code, "未知状态");
    }
}