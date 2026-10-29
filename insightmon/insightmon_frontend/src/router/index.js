import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        { path: '/', name: 'HomePage', component: () => import('../views/HomePage.vue')},
        { path: '/eventTimeline', name: 'EventTimeline', component: () => import('../views/EventTimeline.vue' )},
        { path: '/detail', name: 'Detail', component: () => import('../views/Detail.vue')},
        { path: '/config', name: 'Config', component: () => import('../views/ConfigList.vue')},
        { path: '/user', name: 'User', component: () => import('../views/User.vue')},
        { path: '/newsList', name: 'NewsList', component: () => import('../views/NewsList.vue')},
    ]
})

export default router
