package com.zby.gmall.list.list;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.*;
import com.zby.gmall.service.ListService;
import com.zby.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Controller
@CrossOrigin
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String getSearch(SkuLsParams skuLsParams, HttpServletRequest request){
        skuLsParams.setPageSize(2);
        SkuLsReslut skuLsReslut = listService.search(skuLsParams);
        List<SkuLsInfo> skuLsInfoList = skuLsReslut.getSkuLsInfoList();


        //分开走
//        List<BaseAttrInfo> baseAttrInfoList = null;
//        List<String> attrValueIdList = skuLsReslut.getAttrValueIdList();
//        if(skuLsParams.getCatalog3Id()!=null){
//            baseAttrInfoList = manageService.getAttrList(skuLsParams.getCatalog3Id());
//        }else{
//            baseAttrInfoList = manageService.getAttrList(attrValueIdList);
//        }
        
        //都通过平台属性值Id检索
        //获取平台属性，平台属性值数据
        List<String> attrValueIdList = skuLsReslut.getAttrValueIdList();
        //根据平台属性值Id查询平台属性，平台属性值13 14 80 81 82 83
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        //编写一个方法记录当前的查询条件
        String urlParam = makeUrlParam(skuLsParams);

        //声明一个面包屑集合
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();

        //点击平台属性值，和url中valueId对比，若相等，从baseAttrInfoList移除平台属性对象
        //集合在遍历的过程中要删除对应的数据 必须使用迭代器遍历
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            //获取平台属性对象
            BaseAttrInfo baseAttrInfo = iterator.next();
            //获取平台属性值集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            //获取集合中平台属性值Id
            for (BaseAttrValue attrValue : attrValueList) {
                //获取url上的valueId在skuLsParams中
                if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        if(valueId.equals(attrValue.getId())){
                            //删除平台属性对象
                            iterator.remove();
                            //构成面包屑 baseAttrValue的名称就是面包屑
                            BaseAttrValue baseAttrValue = new BaseAttrValue();
                            baseAttrValue.setValueName(baseAttrInfo.getAttrName()+":"+attrValue.getValueName());

                            //重新制作urlParam参数
                            // http://list.gmall.com/list.html?keyword=小米&valueId=83&valueId=82
                            // http://list.gmall.com/list.html?keyword=小米&valueId=83

                            String newUrlParam =  makeUrlParam(skuLsParams,valueId);
                            // 保存点击面包屑之后的url参数
                            baseAttrValue.setUrlParam(newUrlParam);
                            //将面包屑添加到集合
                            baseAttrValueList.add(baseAttrValue);
                        }
                    }
                }
            }
        }

        //保存数据
        request.setAttribute("urlParam",urlParam);
        request.setAttribute("skuLsInfoList",skuLsInfoList);
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);
        //保存面包屑
        request.setAttribute("baseAttrValueList",baseAttrValueList);
        //保存检索关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());

        //分页
        request.setAttribute("totalPage",skuLsReslut.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        return "list";
    }

    //编写一个方法记录当前的查询条件
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
        //  href="list.html?keyword=?&valueId=?"
        //判断keyWord
        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }

        //判断三级分类Id
        if(skuLsParams.getCatalog3Id()!=null&&skuLsParams.getCatalog3Id().length()>0){
            if(urlParam.length()>0){
               urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }

        //判断平台属性值Id
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {
                if(excludeValueIds!=null&&excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(valueId.equals(excludeValueId)){
                        continue;
                    }
                }
                if(urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return urlParam;
    }

}
