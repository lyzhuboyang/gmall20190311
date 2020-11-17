package com.zby.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.BaseSaleAttr;
import com.zby.gmall.bean.SpuImage;
import com.zby.gmall.bean.SpuInfo;
import com.zby.gmall.bean.SpuSaleAttr;
import com.zby.gmall.commonutil.data.R;
import com.zby.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@CrossOrigin
@RequestMapping("/gmall/spumanage")
public class SpuManageController {

    @Reference
    ManageService manageService;

    //查询销售属性列表
    @RequestMapping("getBaseSaleAttrList")
    public R baseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return R.ok().data("baseSaleAttrList", baseSaleAttrList);
    }

    @PostMapping("saveSpuInfo")
    public R saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        //保存数据
        manageService.saveSpuInfo(spuInfo);
        return R.ok();
    }


    @GetMapping("spuImageList/{spuId}")
    public R spuImageList(@PathVariable("spuId") String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> spuImgList = manageService.getSpuImgList(spuImage);
        return R.ok().data("spuImgList", spuImgList);
    }

    @GetMapping("spuSaleAttrList/{spuId}")
    public R spuSaleAttrList(@PathVariable("spuId") String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return R.ok().data("spuSaleAttrList", spuSaleAttrList);
    }


}