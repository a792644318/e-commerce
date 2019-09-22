package com.leyou.item.service;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecService {

    @Autowired
    private SpecGroupService groupService;

    @Autowired
    private SpecParamService paramService;

    public List<SpecGroup> querySpecsByCid(Long cid) {
        //查询规格组
        List<SpecGroup> groups = groupService.queryGroupsByCid(cid);
        //便利规格组，添加规格参数名
        groups.forEach(g -> {
            g.setParams(paramService.querySpecParam(null, g.getId(), null));
        });
        return groups;
    }
}
