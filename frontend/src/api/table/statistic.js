import service from "@/utils/request";

const BASE_API = "/api/statistics/invoke";

export default {
    count(params={}) {
        const {table_name, param={}}= params;
        return service({
            url: BASE_API + `?table=${table_name}&method=count`,
            method: "post",
            data: param
        });
    },
}