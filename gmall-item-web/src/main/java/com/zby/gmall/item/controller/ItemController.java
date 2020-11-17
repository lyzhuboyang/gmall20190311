package com.zby.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zby.gmall.bean.SkuInfo;
import com.zby.gmall.bean.SkuSaleAttrValue;
import com.zby.gmall.bean.SpuSaleAttr;
import com.zby.gmall.config.LoginRequire;
import com.zby.gmall.service.ListService;
import com.zby.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    //@LoginRequire(autoRedirect = true)//表示访问商品详情的时候，必须要登录！
    public String getItem(@PathVariable String skuId, HttpServletRequest request, Model model){
        //调用服务层
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 存储 spu，sku数据
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //获取销售属性值组成的skuId集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        //132|134 = 37 133|136 = 38 118|122=39
        //拼接规则：skuId与skuId不相等的时候，不拼接！当拼接到集合末尾不拼接
        //map.put("132|134",37)然后将其转换为json字符串即可
        String key = "";
        Map<String,String> map = new HashMap<>();
        for (int i = 0; i <skuSaleAttrValueList.size() ; i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            //第一次拼接：key=132
            //第二次拼接: key=132|
            //第三次拼接：key=132|134
            //第四次拼接: key=""
            //什么时候拼接|
            if(key.length()>0){
                key+="|";
            }
            key+=skuSaleAttrValue.getSaleAttrValueId();
            if((i+1)==skuSaleAttrValueList.size()||!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                //放入map中！
                map.put(key,skuSaleAttrValue.getSkuId());
                //清空key
                key="";
            }
        }
        //将map转换为json字符串
        String valueSkuJson = JSON.toJSONString(map);
        request.setAttribute("valueSkuJson",valueSkuJson);
        request.setAttribute("skuInfo",skuInfo);
        model.addAttribute("saleAttrList",saleAttrList);
        //当访问一次商品详情时，热度加一
        listService.updateHotScore(skuId);
        return "item";
    }

}
