package com.kuafuai.pay.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.pay.db.domain.GeneralOrders;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;


@Mapper
@Component("commonGeneralOrdersMapper")
/**
* @author www.macpe.cn
* @description 针对表【general_orders】的数据库操作Mapper
* @createDate 2025-05-09 17:20:09
* @Entity generator.domain.GeneralOrders
*/
public interface GeneralOrdersMapper extends BaseMapper<GeneralOrders> {

}




