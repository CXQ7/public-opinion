// utils.js

import {ElMessage} from "element-plus";
import {store} from "@/store/store";

const Utils = {
    check_res(res) {
        console.log("res:", res)

        if (res.status === 200 && res.data.code === 1)
            return true;
        ElMessage.error(res.data.msg)
        if(res.data.msg === "无效的token")
            store.login = false;
        return false;
    },
    is_null(v){
        return !v || v === '' || (Array.isArray(v) && v.length === 0);
    },
    formatDateArrayToString(dateArray) {
        // 检查传入的数组是否具有 6 个元素
        if (!dateArray || dateArray.length > 6) {
            throw new Error("日期数组必须小于 6 个元素：年、月、日、小时、分钟");
        }
        if (dateArray.length < 6) {
            for (let i = dateArray.length; i < 6; i++) {
                dateArray.push(0);
            }
        }
        // 解构数组元素
        const [year, month, day, hour, minute, second] = dateArray;

        // 使用 padStart 方法确保月、日、小时和分钟是两位数格式
        const formattedMonth = String(month).padStart(2, "0");
        const formattedDay = String(day).padStart(2, "0");
        const formattedHour = String(hour).padStart(2, "0");
        const formattedMinute = String(minute).padStart(2, "0");

        // 组合成最终的日期时间字符串
        return `${year}-${formattedMonth}-${formattedDay} ${formattedHour}:${formattedMinute}`;
    },
    trimText(text, maxLength) {
        if (text.length > maxLength) {
            return text.slice(0, maxLength) + '...';
        }
        return text;
    },

};

export default Utils;
