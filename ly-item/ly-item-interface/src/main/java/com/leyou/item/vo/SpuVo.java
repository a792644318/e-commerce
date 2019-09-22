package com.leyou.item.vo;

import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import lombok.Data;

import javax.persistence.Transient;
import java.util.List;

@Data
public class SpuVo extends Spu {


    private String cname;// 商品分类名称
    private String bname;// 品牌名称

    @Transient
    private SpuDetail spuDetail;
    @Transient
    private List<Sku> skus;
    
}