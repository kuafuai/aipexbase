package com.kuafuai.dynamic.mapper;

import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Maps;
import com.kuafuai.dynamic.condition.OrderByBuilder;
import com.kuafuai.dynamic.context.TableContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class DynamicMapperTest {
    @Resource
    private DynamicMapper dynamicMapper;

    @Test
    void insertBatch() {
        final ArrayList<Map<String, Object>> conditions = new ArrayList<>();
        final HashMap<String, Object> e = Maps.newHashMap();
        e.put("name","测试");
        e.put("content","coding");
        conditions.add(e);
        conditions.add(e);
        System.out.println(dynamicMapper.insertBatch("5567","system_config", conditions));;
    }

    @Test
    void test_number(){
        System.out.println(!NumberUtil.isNumber("2.0"));
    }

    @Test
    void build_order_by(){
        Map<String, Object> map = Maps.newHashMap();
        Map<String,Object> orderby = new HashMap<>();
        orderby.put("id","desc");
        orderby.put("ok","asc");



        map.put("order_by",orderby);

        TableContext tableContext = new TableContext("a","",map, Collections.EMPTY_LIST);

        String build = new OrderByBuilder(tableContext).build();
        System.out.printf(build);
    }

    @Test
    void build_order_by_single(){
        Map<String, Object> map = Maps.newHashMap();
        String a = "asd";



        map.put("order_by",a);

        TableContext tableContext = new TableContext("a","",map, Collections.EMPTY_LIST);

        String build = new OrderByBuilder(tableContext).build();
        System.out.printf(build);
    }
}