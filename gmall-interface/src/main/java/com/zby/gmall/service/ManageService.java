package com.zby.gmall.service;

import com.zby.gmall.bean.*;

import java.util.List;

public interface ManageService {
    List<BaseCatalog1> getCatalog1();

    List<BaseCatalog2> getCatalog2(String catalog1Id);

    List<BaseCatalog3> getCatalog3(String catalog2Id);

    List<BaseAttrInfo> getAttrList(String catalog3Id);

    //添加商品平台属性
    boolean saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //根据平台属性id查询属性值集合
    List<BaseAttrValue> getAttrValueList(String attrId);

    //根据attrId获取BaseAttrInfo
    BaseAttrInfo getAttrInfo(String attrId);

    //根据三级分类Id查询商品Spu信息
    List<SpuInfo> getSpuList(String catalog3Id);

    List<SpuInfo> getSpuList(SpuInfo spuInfo);

    //查询销售属性列表
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存SPU
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 通过SpuImage属性查找spuImage集合
     *
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImgList(SpuImage spuImage);

    /**
     * 根据spuId查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    //保存sku
    void saveSkuInfo(SkuInfo skuInfo);

    //根据skuId查询SkuInfo数据
    SkuInfo getSkuInfo(String skuId);

    //根据skuId查询图片列表
    List<SkuImage> getSkuImageBySkuId(String skuId);

    //根据skuInfo查询出对应选中的销售属性
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    //根据spuId查询SkuSaleAttrValue集合
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 根据valueId查询BaseAttrInfo的集合
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
