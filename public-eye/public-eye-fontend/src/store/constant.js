import { reactive } from 'vue'
export const constant = reactive({
    host: 'http://localhost:8080',
    content_type: {
        common: 'application/x-www-form-urlencoded',
        body: 'application/json',
    },
    backgroundImg: [
        'https://ns-strategy.cdn.bcebos.com/ns-strategy/upload/fc_big_pic/part-00522-444.jpg',
        'https://ns-strategy.cdn.bcebos.com/ns-strategy/upload/fc_big_pic/part-00717-705.jpg',
        'https://img1.baidu.com/it/u=3460125785,4049136525&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=281',
        'https://img2.baidu.com/it/u=428281056,433586594&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=333',
        'https://img1.baidu.com/it/u=2450758570,726384014&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=313',
        'https://img1.baidu.com/it/u=263174767,3661881518&fm=253&fmt=auto&app=138&f=JPEG?w=400&h=300',
        'https://img2.baidu.com/it/u=2429610435,1392715426&fm=253&fmt=auto&app=138&f=JPEG?w=610&h=285',
        'https://img2.baidu.com/it/u=2580088795,3756632370&fm=253&fmt=auto&app=138&f=JPEG?w=1048&h=500',
        'https://q1.itc.cn/q_70/images03/20240527/803d06241b2b4cd887613c4e07762c5f.jpeg',
        'https://hbimg.huaban.com/cef8d3b98b98361a57846b5db81c3e950d1e7c0027770-AzOqPS_fw658',
    ],
    origin_store: {
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
        }
    },
})
