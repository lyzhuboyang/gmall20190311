package com.zby.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsReslut implements Serializable {

    List<SkuLsInfo> skuLsInfoList;

    long total;

    long totalPages;

    //平台属性值Id
    List<String> attrValueIdList;

}
