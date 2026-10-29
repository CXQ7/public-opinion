import { reactive } from 'vue'
export const store = reactive({
    login: false,
    isAdmin: false,
    header: {
        'accept': 'application/json',
        access_token: '__CONFIGURE_LOCALLY__',
        token_type: '',
        'Authorization': '__CONFIGURE_LOCALLY__',
    },
    user: {
        id: 1,
        username: '__CONFIGURE_LOCALLY__',
        stu_id: '',
        phone: '',
        email: '',
        image_url: 'https://img0.baidu.com/it/u=962511916,1823173324&fm=253&fmt=auto&app=120&f=JPEG?w=501&h=500' ,
        created_at: null,
        updated_at: null,
    },
    keyword:{
        id: 0,
        keyword: "",
        heat: 0,
        sentiment: 0.0,
        createTime: "2024-10-29 22:48:42",
        updateTime: "2024-10-29 22:48:42",
        representativeEvent:{}
    },
    event:{
        id: 0,
        keywordId: 0,
        newsIds: "",
        heat: 0,
        sentiment: 0.0,
        urls: "",
        eventDatetime: "2024-10-29 22:48:42",
        createTime: "2024-10-29 22:48:42",
        updateTime: "2024-10-29 22:48:42",
    },
    homePageParams:{
        filter:{
            searchTerm: '',
        },
    },
})
