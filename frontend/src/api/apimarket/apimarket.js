import request from '@/utils/request';

export default {
    // 获取API市场列表(分页)
    pages(data) {
        return request({
            url: '/admin/api/market/page',
            method: 'post',
            data
        });
    },

    // 获取API详情
    detail(id) {
        return request({
            url: `/admin/api/market/${id}`,
            method: 'get'
        });
    },

    // 添加API
    add(data) {
        return request({
            url: '/admin/api/market/add',
            method: 'post',
            data
        });
    },

    // 更新API
    update(data) {
        return request({
            url: '/admin/api/market/update',
            method: 'post',
            data
        });
    },

    // 删除API
    delete(id) {
        return request({
            url: `/admin/api/market/delete/${id}`,
            method: 'post'
        });
    },

    // 获取所有API列表（不分页）
    list() {
        return request({
            url: '/admin/api/market/list',
            method: 'get'
        });
    }
};
