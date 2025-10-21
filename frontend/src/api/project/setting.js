import request from "@/utils/request.js";

export default {
    login() {
        return request({
            url: "/system/setting/login",
            method: "get",
        });
    },
    settings(appId) {
        return request({
            url: "/admin/system/settings/" + appId,
            method: "get",
        });
    },
    saveSetting(appId, data) {
        return request({
            url: "/admin/system/settings/" + appId,
            method: "post",
            data: data
        });
    },
    version() {
        return request({
            url: "/admin/version",
            method: "get",
        });
    }
};