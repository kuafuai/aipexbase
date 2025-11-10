import request from '@/utils/request';

export default {
    // 获取第三方服务列表（分页）
    list(appId, params) {
        return request({
            url: `/admin/application/config/dynamicapi/${appId}/page`,
            method: "post",
            data: params
        });
    },
    // 添加第三方服务
    add(appId, data) {
        return request({
            url: `/admin/application/config/dynamicapi/${appId}/save`,
            method: "post",
            data: data
        });
    },
    addMarket(appId, data){
        return request({
            url: `/admin/application/config/dynamicapi/${appId}/saveApiMarket`,
            method: "post",
            data: data
        });
    },
    // 删除第三方服务
    delete(appId, keyName) {
        return request({
            url: `/admin/application/config/dynamicapi/${appId}/delete/${keyName}`,
            method: "delete",
        });
    }
}
