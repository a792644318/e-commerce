package com.leyou.service;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.leyou.client.BrandClient;
import com.leyou.client.GoodsClient;
import com.leyou.client.SpecClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.item.vo.SpuVo;
import com.leyou.pojo.Goods;
import com.leyou.pojo.SearchRequest;
import com.leyou.pojo.SearchResult;
import com.leyou.repository.GoodsRepository;
import com.leyou.client.CategoryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.spatial3d.geom.GeoOutsideDistance;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.joda.time.chrono.IslamicChronology;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    /**
     * 远程调用和封装(被索引库导入的数据)
     * @param spu
     * @return
     */
    public Goods buildGoods(SpuVo spu) {
        //查询分类名称
        List<Category> categories = categoryClient.selectByIds(Arrays.asList(
                spu.getCid1(),spu.getCid2(),spu.getCid3()));
        if(CollectionUtils.isEmpty(categories)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> name = categories.stream().map(Category::getName).collect(Collectors.toList());
        //查询品牌名称
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if(brand == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //设置搜索条件
        String all= spu.getTitle() + StringUtils.join(name," ") + brand.getName();
        //查询sku
        List<Sku> skus = goodsClient.querySkuBySpuId(spu.getId());
        if(CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //遍历sku
        List<Long> prices = new ArrayList<>();
        List<Map<String,Object>> skuList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String,Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("price",sku.getPrice());
            map.put("image",StringUtils.isBlank(sku.getImages())? "":
                    StringUtils.substringBefore(sku.getImages(),","));
            map.put("title",sku.getTitle());
            skuList.add(map);
        });
        //查询规格参数名
        List<SpecParam> params = specClient.querySpecParam(spu.getCid3(),null,true);
        if(CollectionUtils.isEmpty(params)){
            throw new LyException(ExceptionEnum.PARAM_NOT_FOUND);
        }
        //查询商品详情
        SpuDetail detail = goodsClient.querySpuDetailBySpuId(spu.getId());
        if(detail == null){
            throw new LyException(ExceptionEnum.DETAIL_QUERY_ERROR);
        }
        //获取商品规格参数值
        Map<Long,String> gv = JsonUtils.nativeRead(detail.getGenericSpec(), new TypeReference<Map<Long, String>>() {});
        Map<Long,List<String>> sv = JsonUtils.nativeRead(detail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {});
        //配对规格参数名和规格参数值
        Map<String,Object> specs = new HashMap<>();
        for(SpecParam param : params){
            String key = param.getName();
            Object value = "";
            if(param.getGeneric()){
                value = gv.get(param.getId());
                if(param.getNumeric()){
                    value = chooseSegment(gv.get(param.getId()),param);
                }
            }else{
                value = sv.get(param.getId());
            }
            specs.put(key,value);
        }
        //封装
        Goods goods = new Goods();
        goods.setAll(all);
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(new Date());
        goods.setId(spu.getId());
        goods.setPrice(prices);
        goods.setSkus(JsonUtils.serialize(skuList));
        goods.setSpecs(specs);
        goods.setSubTitle(spu.getSubTitle());
        return goods;
    }

    /**
     * 向索引库导入增加或修改单个good
     * @param id
     */
    public void createIndex(Long id){
        Spu spu = goodsClient.querySpuBySpuId(id);
        SpuVo spuVo = new SpuVo();
        BeanUtils.copyProperties(spu,spuVo);
        Goods goods = buildGoods(spuVo);
        repository.save(goods);
    }

    /**
     * 删除索引库的单个good
     * @param id
     */
    public void deleteIndex(Long id) {
        repository.deleteById(id);
    }

    //将值转换成一段segment的方法
    private String chooseSegment(String value,SpecParam p){
        Double val = NumberUtils.toDouble(value);
        String result = "其他";
        for(String segment : p.getSegments().split(",")){
            String[] segs = segment.split("-");
            Double begin = NumberUtils.toDouble(segs[0]);
            Double end = Double.MAX_VALUE;
            if(segs.length==2){
                end = NumberUtils.toDouble(segs[1]);
            }
            if(val >= begin && val <=end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
            }
        }
        return result;
    }

    public SearchResult queryGoods(SearchRequest request) {
        //1构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //1.1 key和filter查询条件
        QueryBuilder basicQuery = buildBasicQueryWithFilter(request);
        queryBuilder.withQuery(basicQuery);
        //1.2 过滤返回结果字段
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        //1.3 分页
        Integer page = request.getPage();
        Integer size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page-1,size));
        //1.4 聚合
        String categoryAggName = "category";
        String brandAggName = "brand";
        //1.4.1 对分类聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        //1.4.2 对品牌聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        //2 查询
        AggregatedPage pageResult = template.queryForPage(queryBuilder.build(),Goods.class);
        //3 解析查询结果
        int totalPages = pageResult.getTotalPages();
        long totalElements = pageResult.getTotalElements();
        List<Goods> goods = pageResult.getContent();
        Aggregations aggs = pageResult.getAggregations();
        List<Category> categoryList = parseCategoryAgg(aggs.get(categoryAggName));
        List<Brand> brandList = parseBrandAgg(aggs.get(brandAggName));
        // 4 获取规格参数聚合结果
        List<Map<String,Object>> specs = new ArrayList<>();
        if(categoryList != null && categoryList.size() == 1){
            specs = getSpecs(categoryList.get(0).getId(),basicQuery);
        }
        //5 返回结果
        return new SearchResult(totalElements,totalPages,goods,categoryList,brandList,specs);

    }

    /**
     * 条件以及过滤查询
     * @param request
     * @return
     */
    private QueryBuilder buildBasicQueryWithFilter(SearchRequest request) {
        //查询条件载体
        BoolQueryBuilder basicQuery = QueryBuilders.boolQuery();
        //条件查询
        basicQuery.must(QueryBuilders.matchQuery("all",request.getKey()));
        //过滤查询
        Map<String,String> filter = request.getFilter();
        for(Map.Entry<String,String> entry : filter.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            //排除商品分类和品牌
            if(!key.equals("cid3") && !key.equals("brandId")){
                key = "specs" + key + ".keyword";
            }
            basicQuery.filter(QueryBuilders.termQuery("specs" + key + ".keyword",value));
        }
        return basicQuery;
    }

    /**
     * 规格参数的聚合
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getSpecs(Long cid, QueryBuilder basicQuery) {
        try {
            //构建查询条件
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            //条件查询
            queryBuilder.withQuery(basicQuery);
            //查询规格参数
            List<SpecParam> params = specClient.querySpecParam(cid,null,true);
            if(CollectionUtils.isEmpty(params)){
                throw new LyException(ExceptionEnum.DETAIL_QUERY_ERROR);
            }
            for(SpecParam param : params){
                //添加聚合
                String aggName = param.getName();
                String field = "specs" + param.getName() + "" + ".keyword";
                queryBuilder.addAggregation(AggregationBuilders.terms(aggName).field(field));
            }
            //查询结果
            AggregatedPage result = template.queryForPage(queryBuilder.build(),Goods.class);
            //解析结果
            Aggregations aggs = result.getAggregations();
            List<Map<String,Object>> specs = new ArrayList<>();
            for(SpecParam param : params){
                Map<String,Object> spec = new HashMap<>();
                String name = param.getName();
                StringTerms terms = aggs.get(name);
                List<String> options = terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());
                spec.put("k",name);
                spec.put("options",options);
                specs.add(spec);
            }
            return specs;
        } catch (Exception e) {
            log.error("规格参数聚合失败", e);
            return null;
        }
    }

    private List<Brand> parseBrandAgg(LongTerms terms) {
        try{
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Brand> brands = brandClient.queryBrandByIds(ids);
            if(CollectionUtils.isEmpty(brands)){
                throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
            }
            return brands;
        }catch (Exception e){
            log.error("品牌聚合失败",e);
            return null;
        }
    }

    private List<Category> parseCategoryAgg(LongTerms terms) {
        try{
            List<Long> ids = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            List<Category> categoryList = categoryClient.selectByIds(ids);
            if(CollectionUtils.isEmpty(categoryList)){
                throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
            }
            return categoryList;
        }catch (Exception e){
            log.error("商品分类聚合失败",e);
            return null;
        }
    }
}
