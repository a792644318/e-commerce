package com.leyou.web;

import com.leyou.client.GoodsClient;
import com.leyou.client.SpecClient;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.vo.SpuVo;
import com.leyou.client.CategoryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class controller {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    @GetMapping("category")
    public ResponseEntity<List<Category>> getCategory(@RequestParam("ids")List<Long> ids){
        List<Category> categoryList = categoryClient.selectByIds(ids);
        return ResponseEntity.ok(categoryList);
    }

    @GetMapping("/spu/page")
    public ResponseEntity<PageResult<SpuVo>> querySpuByPage(
            @RequestParam(value = "key", required =false) String key,
            @RequestParam(value = "page", defaultValue="1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "saleable", required =false) Boolean saleable
    ){
        return ResponseEntity.ok(goodsClient.querySpuByPage(key, page, rows,saleable));
    }

    @GetMapping("spec/params")
    public ResponseEntity<List<SpecParam>> querySpecParam(
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "searching", required = false) Boolean searching
    ){
        return ResponseEntity.ok(specClient.querySpecParam(cid, gid, searching));

    }
}
