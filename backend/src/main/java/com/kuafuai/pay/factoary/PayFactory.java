package com.kuafuai.pay.factoary;

import com.google.common.collect.Maps;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.pay.service.PayService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.Map;

@Component
@Slf4j
public class PayFactory {

    private static final Map<PayServiceKey, WeakReference<PayServiceAndChannelLink>>
            payServiceAndChannelLinkMap = Maps.newConcurrentMap(); // 使用弱引用，避免内存泄漏

    public PayService getPayService(String database, String payChannel) {
        try {
////          针对不同的database 缓存不同的实例
//            final PayServiceKey payServiceKey = new PayServiceKey(payChannel, database);
//            if (payServiceAndChannelLinkMap.containsKey(payServiceKey)) {
//                final WeakReference<PayServiceAndChannelLink> payServiceAndChannelLinkWeakReference =
//                        payServiceAndChannelLinkMap.get(new PayServiceKey(payChannel, database));
//                if (payServiceAndChannelLinkWeakReference != null && payServiceAndChannelLinkWeakReference.get() != null) {
//                    return payServiceAndChannelLinkWeakReference.get().getPayService();
//                } else {
//                    payServiceAndChannelLinkMap.remove(payServiceKey);
//                }
//            }


            final PayService bean = SpringUtils.getBean(payChannel);
//            final PayServiceAndChannelLink payServiceAndChannelLink = new PayServiceAndChannelLink(payChannel, bean, database);
//            payServiceAndChannelLinkMap.put(new PayServiceKey(payChannel, database),
//                    new WeakReference<>(payServiceAndChannelLink));
            return bean;
        } catch (Exception e) {
            log.error("getPayService", e);
            throw new BusinessException(payChannel + " not found");
        }

    }


    @Data
    @EqualsAndHashCode
    public static class PayServiceAndChannelLink {
        private String payChannel;
        private PayService payService;
        private String database;

        public PayServiceAndChannelLink(String payChannel, PayService payService, String database) {
            this.payChannel = payChannel;
            this.payService = payService;
            this.database = database;
        }
    }

    @Data
    @EqualsAndHashCode
    public static class PayServiceKey {
        private String payChannel;
        private String database;

        public PayServiceKey(String payChannel, String database) {
            this.payChannel = payChannel;
            this.database = database;
        }
    }

}
