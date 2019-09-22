package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecApi {

    @GetMapping("spec/params")
    List<SpecParam> querySpecParam(
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "searching", required = false) Boolean searching
    );

    @GetMapping("spec/{cid}")
    List<SpecGroup> querySpecsByCid(@PathVariable("cid") Long cid);
}
