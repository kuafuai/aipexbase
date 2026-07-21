package com.kuafuai.usage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.usage.entity.FileUploadLog;
import com.kuafuai.usage.vo.StorageBreakdownVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileUploadLogMapper extends BaseMapper<FileUploadLog> {

    @Select("SELECT COUNT(*) FROM file_upload_log WHERE app_id = #{appId}")
    Long countByApp(@Param("appId") String appId);

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM file_upload_log WHERE app_id = #{appId}")
    Long sumBytesByApp(@Param("appId") String appId);

    @Select("SELECT COUNT(*) FROM file_upload_log " +
            "WHERE app_id = #{appId} AND uploaded_at >= #{from}")
    Long countByAppSince(@Param("appId") String appId, @Param("from") LocalDateTime from);

    /**
     * 按 content_type 前缀分组统计. kind = image/video/audio/doc/other.
     * 需要 CASE 表达式聚合, 走 XML.
     */
    List<StorageBreakdownVO> breakdown(@Param("appId") String appId);
}
