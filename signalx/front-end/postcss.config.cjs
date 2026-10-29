module.exports = {
    plugins: {
        'postcss-pxtorem': {
            rootValue: 37.5, // 适配375px宽度的设计稿
            propList: ['*'], // 所有CSS属性都转rem
            selectorBlackList: ['van-'] // 排除第三方UI库（如Vant）
        }
    }
}
