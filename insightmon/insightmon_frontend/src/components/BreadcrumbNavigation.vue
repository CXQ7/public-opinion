<script setup>
import {ref, watchEffect} from 'vue';
import {useRoute} from 'vue-router';
import {constant} from "@/store/constant";
import router from "@/router";
import {ArrowRight} from "@element-plus/icons-vue";

const route = useRoute();
const breadcrumbItems = ref([]);

const update = () => {
  breadcrumbItems.value = [];
  let routeName = route.name;
  breadcrumbItems.value.push({label: routeName, path: `/${routeName}`});
  while (routeName in constant.breadCrumbConfig.navigateTree) {
    routeName = constant.breadCrumbConfig.navigateTree[routeName]
    breadcrumbItems.value.push({label: routeName, path: `/${routeName}`});
  }
  breadcrumbItems.value.push({label: 'Home', path: '/'});
  breadcrumbItems.value = breadcrumbItems.value.reverse()
}

// 使用 watchEffect 来自动追踪路由的变化并更新面包屑
watchEffect(() => {
  // updateBreadcrumbItems();
  update()
});

const navigateTo = (item) => {
  router.push(item.path);
};
</script>

<template>
  <el-breadcrumb :separator-icon="ArrowRight" v-show="constant.breadCrumbConfig.shownPages.has(route.name)">
    <el-breadcrumb-item
        v-for="(item, index) in breadcrumbItems"
        :key="index"
        @click="navigateTo(item)"
        style="cursor: pointer;">
      {{ item.label }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>


<style scoped>
.el-breadcrumb {
  display: flex; /* 确保面包屑横向排列 */
  align-items: center; /* 垂直居中 */
  justify-content: flex-start; /* 确保面包屑内容从左到右展开 */
  width: 100%; /* 确保面包屑占据整个父容器宽度 */
  font-size: 100%;
  text-decoration: none;
}

.el-breadcrumb :hover {
  color: #007bff; /* 悬停时的颜色 */
}
</style>
