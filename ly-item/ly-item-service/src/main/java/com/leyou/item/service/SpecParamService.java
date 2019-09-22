package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;

@Service
public class SpecParamService {

    @Autowired
    private SpecParamMapper paramMapper;

    public List<SpecParam> querySpecParam(Long cid, Long gid, Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setCid(cid);
        specParam.setGid(gid);
        specParam.setSearching(searching);
        List<SpecParam> list = paramMapper.select(specParam);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.PARAM_NOT_FOUND);
        }
        return list;
    }
}
