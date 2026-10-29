// utils.js

import {ElMessage} from "element-plus";
import {store} from "@/store/store";

const Utils = {
    check_res(res) {
        console.log("res:", res)
        if (res.status === 200 && res.data.code === 1)
            return true;
        if(res.data.msg.includes("token")) {
            store.login = false;
            store.loginDialogAppear = true;
        }
        return false;
    },
    isNull(v){
        return !v || v === '' || (Array.isArray(v) && v.length === 0);
    },
    parseDateToDatetime(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day} 00:00:00`;
    },
    trimText(text, maxLength) {
        if (text.length > maxLength) {
            return text.slice(0, maxLength) + '...';
        }
        return text;
    },
    parseHttpUrlToHttps(url){
        if (url.startsWith('http://')) {
            return url.replace('http://', 'https://');
        }
        return url;
    }
};

export default Utils;
