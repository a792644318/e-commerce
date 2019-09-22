package com.leyou.repository;


import com.leyou.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
    List<Goods> findByPriceBetween(double price1, double price2);
}
