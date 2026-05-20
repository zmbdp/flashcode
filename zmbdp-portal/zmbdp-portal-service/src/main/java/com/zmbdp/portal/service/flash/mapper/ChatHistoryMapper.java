package com.zmbdp.portal.service.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.portal.service.flash.domain.dto.ChatHistoryDTO;
import com.zmbdp.portal.service.flash.domain.dto.ChatHistoryListReqDTO;
import com.zmbdp.portal.service.flash.domain.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    List<ChatHistory> selectTopByAppId(@Param("appId") Long appId, @Param("size") int maxMessage);

    Integer selectChatHistoryCount(ChatHistoryListReqDTO appListReqDTO);

    List<ChatHistoryDTO> selectChatHistoryPage(ChatHistoryListReqDTO appListReqDTO);
}