package com.solidoa.system.service.impl;

import com.solidoa.system.entity.Dict;
import com.solidoa.system.mapper.DictMapper;
import com.solidoa.system.service.DictService;
import com.solidoa.system.vo.DictVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DictServiceImpl implements DictService {

    private static final String CACHE_PREFIX = "dict:type:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 应用启动时预热字典缓存 (Sprint 4.5 / V2.0 5.2)
     */
    @PostConstruct
    public void warmupCache() {
        try {
            List<String> types = dictMapper.selectDistinctTypes();
            int total = 0;
            for (String type : types) {
                int count = refreshCache(type);
                total += count;
            }
            log.info("[5.2 字典缓存] 启动预热完成: {} 种类型, 共 {} 条", types.size(), total);
        } catch (Exception e) {
            log.warn("[5.2 字典缓存] 启动预热失败（不影响启动）: {}", e.getMessage());
        }
    }

    @Override
    public List<DictVO> getByType(String type) {
        // 1. 查 Redis 缓存
        String key = CACHE_PREFIX + type;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isEmpty()) {
                // 简单格式: id|type|label|value|sort;...
                return parseCacheValue(cached);
            }
        } catch (Exception e) {
            log.warn("[5.2 字典缓存] 读缓存失败，降级查 DB: type={}, err={}", type, e.getMessage());
        }

        // 2. 缓存未命中查 DB
        List<Dict> dicts = dictMapper.selectByType(type);
        List<DictVO> vos = dicts.stream().map(this::convertToVO).collect(Collectors.toList());

        // 3. 回写缓存
        if (!vos.isEmpty()) {
            writeCache(type, vos);
        }
        return vos;
    }

    @Override
    public List<String> getTypes() {
        return dictMapper.selectDistinctTypes();
    }

    /**
     * 手动刷新某个类型的缓存 (供后台管理调用)
     */
    public int refreshCache(String type) {
        List<Dict> dicts = dictMapper.selectByType(type);
        List<DictVO> vos = dicts.stream().map(this::convertToVO).collect(Collectors.toList());
        writeCache(type, vos);
        return vos.size();
    }

    private void writeCache(String type, List<DictVO> vos) {
        try {
            String value = vos.stream()
                .map(v -> String.format("%d|%s|%s|%s|%d",
                    v.getId(), nullSafe(v.getType()), nullSafe(v.getLabel()),
                    nullSafe(v.getValue()), v.getSort() == null ? 0 : v.getSort()))
                .collect(Collectors.joining(";"));
            redisTemplate.opsForValue().set(CACHE_PREFIX + type, value, CACHE_TTL);
        } catch (Exception e) {
            log.warn("[5.2 字典缓存] 写缓存失败: type={}, err={}", type, e.getMessage());
        }
    }

    private List<DictVO> parseCacheValue(String cached) {
        return Arrays.stream(cached.split(";"))
            .filter(s -> !s.isEmpty())
            .map(s -> {
                String[] parts = s.split("\\|", -1);
                DictVO vo = new DictVO();
                vo.setId(Long.valueOf(parts[0]));
                vo.setType(parts[1]);
                vo.setLabel(parts[2]);
                vo.setValue(parts[3]);
                vo.setSort(Integer.valueOf(parts[4]));
                return vo;
            })
            .collect(Collectors.toList());
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.replace("|", "/").replace(";", ",");
    }

    private DictVO convertToVO(Dict dict) {
        DictVO vo = new DictVO();
        vo.setId(dict.getId());
        vo.setType(dict.getType());
        vo.setLabel(dict.getLabel());
        vo.setValue(dict.getValue());
        vo.setSort(dict.getSort());
        return vo;
    }
}