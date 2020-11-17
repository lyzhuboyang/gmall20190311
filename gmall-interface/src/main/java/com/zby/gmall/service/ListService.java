package com.zby.gmall.service;


import com.zby.gmall.bean.SkuLsInfo;
import com.zby.gmall.bean.SkuLsParams;
import com.zby.gmall.bean.SkuLsReslut;

public interface ListService {
    /**
     * 商品上架
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 检索接口
     * @param skuLsParams
     * @return
     */
    SkuLsReslut search(SkuLsParams skuLsParams);

    /**
     * 更新热度
     */
    void updateHotScore(String skuId);

}
