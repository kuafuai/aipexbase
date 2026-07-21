package com.kuafuai.usage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.usage.entity.UsageHourly;
import com.kuafuai.usage.vo.EndpointCountVO;
import com.kuafuai.usage.vo.TimeseriesPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UsageHourlyMapper extends BaseMapper<UsageHourly> {

    /**
     * 批量追加, 纯 INSERT. 每次 flush 都是新行, 多实例天然并发安全.
     */
    void batchInsert(@Param("list") List<UsageHourly> list);

    /**
     * 时间范围内某 appId 的总调用数.
     */
    Long sumCallCount(@Param("appId") String appId,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /**
     * 时间范围内某 appId 的错误调用数 (status_bucket in 4xx / 5xx).
     */
    Long sumErrCount(@Param("appId") String appId,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

    /**
     * 时间范围内耗时累加(毫秒), 用于计算平均耗时.
     */
    Long sumLatencyMs(@Param("appId") String appId,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /**
     * 按天分桶的时间序列, 供趋势图使用.
     */
    List<TimeseriesPointVO> timeseriesByDay(@Param("appId") String appId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /**
     * 按小时分桶的时间序列, 24h 内用.
     */
    List<TimeseriesPointVO> timeseriesByHour(@Param("appId") String appId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    /**
     * Top N 接口分组, 按调用量倒序.
     */
    List<EndpointCountVO> topEndpoints(@Param("appId") String appId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("limit") int limit);
}
