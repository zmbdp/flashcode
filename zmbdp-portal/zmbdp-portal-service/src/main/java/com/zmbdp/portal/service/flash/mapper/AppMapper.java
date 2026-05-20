package com.zmbdp.portal.service.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.portal.service.flash.domain.dto.AppDTO;
import com.zmbdp.portal.service.flash.domain.dto.AppListReqDTO;
import com.zmbdp.portal.service.flash.domain.entity.App;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AppMapper extends BaseMapper<App> {
    Integer selectAppsCount(AppListReqDTO appListReqDTO);

    List<AppDTO> selectPage(AppListReqDTO appListReqDTO);
}