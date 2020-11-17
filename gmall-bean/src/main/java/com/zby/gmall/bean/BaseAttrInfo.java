package com.zby.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@Data
public class BaseAttrInfo implements Serializable ,  Comparable{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)//获取主键自增  mysql:IDENTITY   oracle:auto
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    @Transient//@Transient表示非数据字段，但是业务逻辑需要使用的字段
    private List<BaseAttrValue> attrValueList;

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
