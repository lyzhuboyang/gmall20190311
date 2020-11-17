package com.zby.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.*;
import com.zby.gmall.commonutil.data.R;
import com.zby.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/gmall/manageweb")
@RestController
@CrossOrigin
public class ManageController {

    @Reference
    private ManageService manageService;

    @GetMapping("getCatalog1")
    public R getCatalog1() {
        List<BaseCatalog1> catalog1List = manageService.getCatalog1();
        return R.ok().data("catalog1List", catalog1List);
    }

    @GetMapping("getCatalog2/{catalog1Id}")
    public R getCatalog2(@PathVariable("catalog1Id") String catalog1Id) {
        List<BaseCatalog2> catalog2List = manageService.getCatalog2(catalog1Id);
        return R.ok().data("catalog2List", catalog2List);

    }

    @GetMapping("getCatalog3/{catalog2Id}")
    public R getCatalog3(@PathVariable String catalog2Id) {
        List<BaseCatalog3> catalog3List = manageService.getCatalog3(catalog2Id);
        return R.ok().data("catalog3List", catalog3List);
    }

    @GetMapping("attrInfoList/{catalog3Id}")
    public R attrInfoList(@PathVariable String catalog3Id) {
        List<BaseAttrInfo> attrList = manageService.getAttrList(catalog3Id);
        return R.ok().data("attrList", attrList);
    }

    //@RequestBody 将前台的json数据转换为java对象
    @PostMapping("saveAttrInfo")
    public R saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        boolean flag = manageService.saveAttrInfo(baseAttrInfo);
        if (flag) {
            return R.ok();
        } else {
            return R.error();
        }
    }


    //根据平台属性id查询属性值集合
    //这个设计的优点问题，BaseAttrInfo 和 BaseAttrValue 是一对多的。 如果1都没有数据，查询多是无意义的。
    @GetMapping("getAttrValueList/{attrId}")
    public R getAttrValueList(@PathVariable String attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return R.ok().data("attrValueList", attrValueList);
    }

    //根据三级分类Id查询商品Spu
    //localhost:8082/gmall/manageweb/getSpuList/61
    @GetMapping("getSpuList/{catalog3Id}")
    public R getSpuList(@PathVariable String catalog3Id) {
        List<SpuInfo> spuList = manageService.getSpuList(catalog3Id);
        return R.ok().data("spuList", spuList);
    }

}
