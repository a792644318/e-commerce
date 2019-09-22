package com.leyou.item.api;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.vo.SpuVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsApi {

    @GetMapping("/spu/page")
    PageResult<SpuVo> querySpuByPage(
            @RequestParam(value = "key", required =false) String key,
            @RequestParam(value = "page", defaultValue="1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "saleable", required =false) Boolean saleable
    );


    @GetMapping("/spu/detail/{id}")
    SpuDetail querySpuDetailBySpuId(@PathVariable("id") Long spuId);


    @GetMapping("/sku/list")
    List<Sku> querySkuBySpuId(@RequestParam("id") Long spuId);

    @GetMapping("spu/{id}")
    SpuVo querySpuBySpuId(@PathVariable("id") Long id);

    @GetMapping("sku/list/{ids}")
    List<Sku> querySkuByIds(@PathVariable("ids")List<Long> ids);

    @GetMapping("sku/{id}")
    Sku querySkuById(@PathVariable("id") Long id);


}
