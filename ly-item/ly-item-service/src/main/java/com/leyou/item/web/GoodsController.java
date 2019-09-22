package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.CartDTO;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.pojo.Stock;
import com.leyou.item.service.GoodsService;
import com.leyou.item.vo.SpuVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @GetMapping("/spu/page")
    public ResponseEntity<PageResult<SpuVo>> querySpuByPage(
            @RequestParam(value = "key", required =false) String key,
            @RequestParam(value = "page", defaultValue="1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "saleable", required =false) Boolean saleable
    ){
        return ResponseEntity.ok(goodsService.querySpuByPage(key, page, rows,saleable));
    }

    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuVo spu){
        goodsService.saveGoods(spu);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("goods")
    public ResponseEntity<Void> updateGoods(@RequestBody SpuVo spu){
        goodsService.updateGoods(spu);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/spu/detail/{id}")
    public ResponseEntity<SpuDetail> querySpuDetailBySpuId(@PathVariable("id") Long spuId){
        return ResponseEntity.ok(goodsService.querySpuDetailBySpuId(spuId));
    }

    @GetMapping("/sku/list")
    public ResponseEntity<List<Sku>> querySkuBySpuId(@RequestParam("id") Long spuId){
        return ResponseEntity.ok(goodsService.querySkuBySpuId(spuId));
    }

    @GetMapping("sku/list/{ids}")
    public ResponseEntity<List<Sku>> querySkuByIds(@PathVariable("ids")List<Long> ids){
        return ResponseEntity.ok(goodsService.querySkuByIds(ids));
    }

    @GetMapping("sku/{id}")
    public ResponseEntity<Sku> querySkuById(@PathVariable("id") Long id){
        return ResponseEntity.ok(goodsService.querySkuById(id));
    }

    @GetMapping("spu/{id}")
    public ResponseEntity<SpuVo> queryBySpuId(@PathVariable("id") Long id){
        return ResponseEntity.ok(goodsService.querySpuBySpuId(id));
    }

    @GetMapping("stock/decrease")
    public ResponseEntity<Void> decreaseStock(@RequestBody List<CartDTO> carts){
        goodsService.decreaseStock(carts);
        return ResponseEntity.ok().build();
    }
}
