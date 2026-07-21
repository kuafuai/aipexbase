package com.kuafuai.usage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.usage.entity.FileUploadLog;
import com.kuafuai.usage.mapper.FileUploadLogMapper;
import com.kuafuai.usage.vo.StorageBreakdownVO;
import com.kuafuai.usage.vo.StorageSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageQueryService {

    private final FileUploadLogMapper fileUploadLogMapper;

    /**
     * kind -> content_type WHERE 片段. 白名单静态字符串, 不接受用户输入拼接, SQL 注入无风险.
     * 跟 FileUploadLogMapper.xml 里 breakdown 的 CASE 逻辑保持一致.
     */
    private static final Map<String, String> KIND_WHERE = new HashMap<>();

    static {
        KIND_WHERE.put("image", "content_type LIKE 'image/%'");
        KIND_WHERE.put("video", "content_type LIKE 'video/%'");
        KIND_WHERE.put("audio", "content_type LIKE 'audio/%'");
        KIND_WHERE.put("doc",
                "(content_type = 'application/pdf' " +
                        "OR content_type LIKE 'application/msword%' " +
                        "OR content_type LIKE 'application/vnd.%' " +
                        "OR content_type LIKE 'text/%')");
        KIND_WHERE.put("other",
                "(content_type IS NULL " +
                        "OR (content_type NOT LIKE 'image/%' " +
                        "AND content_type NOT LIKE 'video/%' " +
                        "AND content_type NOT LIKE 'audio/%' " +
                        "AND content_type <> 'application/pdf' " +
                        "AND content_type NOT LIKE 'application/msword%' " +
                        "AND content_type NOT LIKE 'application/vnd.%' " +
                        "AND content_type NOT LIKE 'text/%'))");
    }

    public StorageSummaryVO summary(String appId) {
        StorageSummaryVO vo = new StorageSummaryVO();
        long totalFiles = nz(fileUploadLogMapper.countByApp(appId));
        long totalBytes = nz(fileUploadLogMapper.sumBytesByApp(appId));
        vo.setTotalFiles(totalFiles);
        vo.setTotalMb(round4(totalBytes / 1024d / 1024d));
        LocalDateTime now = LocalDateTime.now();
        vo.setUploadedThisWeek(nz(fileUploadLogMapper.countByAppSince(appId, now.minusDays(7))));
        vo.setUploadedThisMonth(nz(fileUploadLogMapper.countByAppSince(appId, now.minusDays(30))));
        return vo;
    }

    public List<StorageBreakdownVO> breakdown(String appId) {
        return fileUploadLogMapper.breakdown(appId);
    }

    public IPage<FileUploadLog> pageFiles(String appId, String kind, String keyword,
                                          long current, long pageSize) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        Page<FileUploadLog> page = new Page<>(current <= 0 ? 1 : current, pageSize);

        LambdaQueryWrapper<FileUploadLog> qw = new LambdaQueryWrapper<FileUploadLog>()
                .eq(FileUploadLog::getAppId, appId);

        // kind: 白名单 + 静态 SQL 片段, 不涉及用户输入拼接
        String kindWhere = kind == null ? null : KIND_WHERE.get(kind.toLowerCase());
        if (kindWhere != null) {
            qw.apply(kindWhere);
        }

        if (StringUtils.isNotEmpty(keyword)) {
            qw.and(w -> w.like(FileUploadLog::getOriginalName, keyword)
                    .or()
                    .like(FileUploadLog::getFileUrl, keyword));
        }

        qw.orderByDesc(FileUploadLog::getUploadedAt).orderByDesc(FileUploadLog::getId);

        return fileUploadLogMapper.selectPage(page, qw);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static double round4(double v) {
        return Math.round(v * 10000d) / 10000d;
    }
}
