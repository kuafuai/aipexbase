import request from '@/utils/request';

import {saveAs} from 'file-saver'
import {ElLoading, ElMessage} from "element-plus";
import errorCode from "@/utils/errorCode.js";
import {blobValidate} from "@/utils/ruoyi.js";

export default {
    tables(data) {
        return request({
            url: "/admin/database/tables",
            method: "post",
            data: data
        });
    },
    columns(data) {
        return request({
            url: "/admin/database/columns",
            method: "post",
            data: data
        });
    },
    createTable(data) {
        return request({
            url: "/admin/database/create/table",
            method: "post",
            data: data
        });
    },
    updateTable(data) {
        return request({
            url: "/admin/database/update/table",
            method: "post",
            data: data
        });
    },
    deleteTable(data) {
        return request({
            url: "/admin/database/delete/table",
            method: "post",
            data: data
        });
    },
    getTableInfo(data) {
        return request({
            url: "/admin/database/get/table",
            method: "post",
            data: data
        });
    },
    selectTableData(data) {
        return request({
            url: "/admin/database/select",
            method: "post",
            data: data
        });
    },
    datas(data) {
        return request({
            url: "/admin/database/data",
            method: "post",
            data: data
        });
    },
    addData(data) {
        return request({
            url: "/admin/database/data/add",
            method: "post",
            data: data
        });
    },
    updateData(data) {
        return request({
            url: "/admin/database/data/update",
            method: "post",
            data: data
        });
    },
    deleteData(data) {
        return request({
            url: "/admin/database/data/delete",
            method: "post",
            data: data
        });
    },
    export(params, filename, tableName, appId) {
        let downloadLoadingInstance = ElLoading.service({
            text: "正在下载数据，请稍候",
            background: "rgba(0, 0, 0, 0.7)",
        });
        return request({
            url: "/admin/" + tableName + "/export/" + appId,
            method: 'post',
            responseType: 'blob',
            data: params
        }).then(async (data) => {
            const isBlob = blobValidate(data);
            if (isBlob) {
                const blob = new Blob([data])
                saveAs(blob, filename)
            } else {
                const resText = await data.text();
                const rspObj = JSON.parse(resText);
                const errMsg = errorCode[rspObj.code] || rspObj.message || errorCode['default']
                ElMessage.error(errMsg);
            }
            downloadLoadingInstance.close();
        }).catch((r) => {
            console.error(r)
            ElMessage.error('下载文件出现错误，请联系管理员！')
            downloadLoadingInstance.close();
        })
    },
    importData(tableName, file, appId) {
        const formData = new FormData();
        formData.append('file', file);

        const url = appId
            ? `/admin/${tableName}/import/${appId}`
            : `/${tableName}/import`;

        return request({
            url,
            method: "post",
            data: formData,
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
    },

    downloadImportTemplate(tableName, appId) {
        const url = appId
            ? `/admin/${tableName}/downloadTemplate/${appId}`
            : `/${tableName}/downloadTemplate`;

        return request({
            url,
            method: "get",
            responseType: 'blob'
        });
    }

}