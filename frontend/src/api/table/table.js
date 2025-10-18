import service from "@/utils/request";

const BASE_API = "/api/data/invoke";

export default {
    add(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=add`,
            method: "post",
            data: param
        });
    },
    delete(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=delete`,
            method: "post",
            data: param
        });
    },
    update(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=update`,
            method: "post",
            data: param
        });
    },

    get(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=get`,
            method: "post",
            data: param
        });
    },

    page(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=page`,
            method: "post",
            data: param
        });
    },
    list(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=list`,
            method: "post",
            data: param
        });
    },
    deleteBatch(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=deleteBatch`,
            method: "post",
            data: param
        });
    },
}