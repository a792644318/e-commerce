package com.leyou.pojo;

import com.leyou.item.pojo.Sku;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Document(indexName = "goods", type = "docs",shards = 1,replicas = 0)
public class Goods{

    @Id
    private Long id;
    @Field(type = FieldType.Text,analyzer = "ik_smart")
    private String all;
    private Long cid1;
    private Long cid2;
    private Long cid3;
    private Long brandId;
    @Field(type = FieldType.Keyword, index = false)
    private String subTitle;
    private Date createTime;
    @Field(type = FieldType.Keyword, index = false)
    private String skus;
    private List<Long> price;
    private Map<String,Object> specs;

}