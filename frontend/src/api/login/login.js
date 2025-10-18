import request from '@/utils/request';

export default {

    loginPasswd(data) {
        return request({
            url: "/admin/login",
            method: "post",
            data: data
        });
    },
    register(data) {
        return request({
            url: "/admin/register",
            method: "post",
            data: {
                "email": data.email,
                "password": data.password
            }
        });
    },
    getUserInfo() {
        return request({
            url: "/getUserInfo",
            method: "get"
        });
    },
    logout() {
        return request({
            url: "/logout",
            method: "get"
        });
    }
};