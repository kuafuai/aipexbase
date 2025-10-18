import request from '@/utils/request';

export default {
    // 获取API密钥列表
    list(appId, data) {
        return request({
            url: `/admin/application/config/apikeys/${appId}/page`,
            method: "post",
            data: data
        });
    },
    // 生成新的API密钥
    generate(appId, data) {
        return request({
            url: `/admin/application/config/apikeys/${appId}/save`,
            method: "post",
            data: data
        });
    },
    // 删除API密钥
    delete(appId, keyName) {
        return request({
            url: `/admin/application/config/apikeys/${appId}/${keyName}`,
            method: "delete",
        });
    },
    // 切换密钥状态（启用/禁用）
    toggle(appId, data) {
        return request({
            url: `/admin/application/config/apikeys/${appId}/switch`,
            method: "post",
            data: data
        });
    }
}
