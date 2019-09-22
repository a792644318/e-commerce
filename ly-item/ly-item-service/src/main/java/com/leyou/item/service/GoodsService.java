package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import com.leyou.item.vo.SpuVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.nio.file.SecureDirectoryStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<SpuVo> querySpuByPage(String key, Integer page, Integer rows, Boolean saleable) {
        //分页
        PageHelper.startPage(page, rows);
        //创建查询条件
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //搜索条件
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title", "%"+key+"%");
        }
        if(saleable != null) {
            criteria.orEqualTo("saleable", saleable);
        }
        example.setOrderByClause("last_update_time DESC");
        Page<Spu> pageInfo = (Page<Spu>) spuMapper.selectByExample(example);
        List<SpuVo> list = pageInfo.getResult().stream().map(spu ->{
            //创建Spuvo
            SpuVo spuVo = new SpuVo();
            //拷贝spu属性注入spuvo
            BeanUtils.copyProperties(spu, spuVo);
            //根据cid查询三级分类
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(),spu.getCid2(),spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spuVo.setCname(StringUtils.join(names,"/"));
            spuVo.setBname(brandService.queryById(spu.getBrandId()).getName());
            return spuVo;
        }).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return new PageResult<>(pageInfo.getTotal(), list);

    }

    /**
     * 新增商品
     * @param spu
     */
    @Transactional
    public void saveGoods(SpuVo spu) {
        //新增spu
        spu.setId(null);
        spu.setValid(true);
        spu.setSaleable(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());

        int count = spuMapper.insert(spu);
        if(count != 1) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        spuDetailMapper.insert(spuDetail);
        saveSkuAndStock(spu.getSkus(), spu.getId());
        //发送消息队列
        sendMessage(spu.getId(),"insert");
    }

    /**
     * 新增sku
     * @param skus
     * @param spuId
     */
    private void saveSkuAndStock(List<Sku> skus, Long spuId) {
        List<Stock> stocks = new ArrayList<>();
        for(Sku sku : skus){
            sku.setSpuId(spuId);
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());

            int count = skuMapper.insert(sku);
            if(count != 1) {
                throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
            }
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            /*stockMapper.insert(stock);*/
            stocks.add(stock);
        }
        stockMapper.insertList(stocks);

    }

    /**
     * 新增detail
     * @param spuId
     * @return
     */
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if(spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return spuDetail;
    }

    /**
     * 通过spuId查询sku
     * @param spuId
     * @return
     */
    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        addStock(skuList);
        return skuList;
    }

    /**
     * 通过skuId查询sku
     * @param id
     * @return
     */
    public Sku querySkuById(Long id) {
        Sku sku = skuMapper.selectByPrimaryKey(id);
        if(sku == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        Stock stock = stockMapper.selectByPrimaryKey(sku.getId());
        if(stock == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        sku.setStock(stock.getStock());
        return sku;
    }

    /**
     * 通过ids查询sku
     * @param ids
     * @return
     */
    public List<Sku> querySkuByIds(List<Long> ids) {
        List<Sku> skuList = skuMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        addStock(skuList);
        return skuList;
    }

    /**
     * 向sku注入stock属性
     * @param skuList
     */
    private void addStock(List<Sku> skuList) {
        List<Long> ids = skuList.stream().map(Sku :: getId).collect(Collectors.toList());
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(stockList)){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        Map<Long, Integer> map = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s -> s.setStock(map.get(s.getId())));
    }

    /**
     * 更新商品
     * @param spu
     */
    @Transactional
    public void updateGoods(SpuVo spu) {
        if(spu.getId() == null){
            throw new LyException(ExceptionEnum.GOODS_ID_NOT_BE_NULL);
        }
        //查询以前的sku
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        //如果以前存在，则删除
        if(!CollectionUtils.isEmpty(skuList)){
            //删除sku
            skuMapper.delete(sku);
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //新增sku和库存
        saveSkuAndStock(spu.getSkus(), spu.getId());
        //更新spu
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);
        spu.setSaleable(null);
        spu.setValid(null);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if(count != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //更新spudetail
        count = spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if(count != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //发送消息队列
        sendMessage(spu.getId(),"update");
    }

    public SpuVo querySpuBySpuId(Long id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        SpuVo spuVo = new SpuVo();
        BeanUtils.copyProperties(spu,spuVo);
        if(spuVo == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        List<Sku> skus = querySkuBySpuId(id);
        SpuDetail detail = querySpuDetailBySpuId(id);
        spuVo.setSkus(skus);
        spuVo.setSpuDetail(detail);
        return spuVo;
    }

    /**
     * 发送队列消息
     * @param id
     * @param type
     */
    private void sendMessage(Long id,String type) {
        try {
            amqpTemplate.convertAndSend("item." + type, id);
        } catch (AmqpException e) {
            log.error("{}商品消息发送异常，商品id:{}",type,id,e);
        }
    }

    @Transactional
    public void decreaseStock(List<CartDTO> carts) {
        for(CartDTO cartDTO : carts){
            //减库存
           int count =  stockMapper.decreaseStock(cartDTO.getSkuId(),cartDTO.getNum());
           if(count != 1){
               throw new LyException(ExceptionEnum.DECREASE_STOCK_ERROR);
           }
        }
    }
}
