package com.leyou.service;


import com.leyou.client.GoodsClient;
import com.leyou.common.vo.PageResult;

import com.leyou.item.pojo.Sku;
import com.leyou.item.vo.SpuVo;
import com.leyou.pojo.Goods;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchTest {

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Test
    public void testCreateIndex(){
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    @Test
    public void testLoadData(){
        int page = 1;
        int rows = 100;
        int size = 0;
        do{
            //查询spu
            PageResult<SpuVo> result = goodsClient.querySpuByPage(null, page, rows, true);
            List<SpuVo> spuList = result.getItems();
            if(CollectionUtils.isEmpty(spuList)){
                break;
            }
            //利用spu调用buildGoods()方法生成Goods
            List<Goods> goodsList = spuList.stream()
                    .map(searchService::buildGoods).collect(Collectors.toList());
            /*repository.saveAll(goodsList);*/
            for(Goods goods : goodsList){
                System.out.println(goods);
            }
            page++;
            size = spuList.size();
        }while (size == 100);
    }

    @Test
    public void test1(){
        Sku sku = goodsClient.querySkuById(2600248L);
        System.out.println(sku);
    }
}