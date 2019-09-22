package com.leyou.item.web;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.SpecGroupService;
import com.leyou.item.service.SpecParamService;
import com.leyou.item.service.SpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("spec")
public class SpecController {

    @Autowired
    private SpecGroupService groupService;

    @Autowired
    private SpecParamService paramService;

    @Autowired
    private SpecService specService;

    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecGroupByCid(@PathVariable("cid") Long cid){
        return ResponseEntity.ok(groupService.queryGroupsByCid(cid));
    }

    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> querySpecParam(
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "searching", required = false) Boolean searching
            ){
        return ResponseEntity.ok(paramService.querySpecParam(cid, gid, searching));
    }

    @GetMapping("{cid}")
    public ResponseEntity<List<SpecGroup>> querySpecsByCid(@PathVariable("cid") Long cid){
        return ResponseEntity.ok(specService.querySpecsByCid(cid));

    }


}
