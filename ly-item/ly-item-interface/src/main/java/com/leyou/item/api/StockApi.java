package com.leyou.item.api;

import com.leyou.item.pojo.CartDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface StockApi {

    @GetMapping("stock/decrease")
    Void decreaseStock(@RequestBody List<CartDTO> carts);
}
