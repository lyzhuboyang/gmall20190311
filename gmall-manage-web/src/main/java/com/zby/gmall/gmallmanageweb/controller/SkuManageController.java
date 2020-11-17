package com.zby.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.SkuInfo;
import com.zby.gmall.bean.SkuLsInfo;
import com.zby.gmall.commonutil.data.R;
import com.zby.gmall.service.ListService;
import com.zby.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin
@RequestMapping("/gmall/skumanage")
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @PostMapping("saveSku")
    public R saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
        return R.ok();
    }

    @GetMapping("onSale/{skuId}")
    public R onSale(@PathVariable String skuId){
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        BeanUtils.copyProperties(skuInfo,skuLsInfo);
        listService.saveSkuLsInfo(skuLsInfo);
        return R.ok();
    }




}
