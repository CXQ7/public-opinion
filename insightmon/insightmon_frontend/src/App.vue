<script setup>
import Aside from "@/components/Aside.vue";
import Header from "@/components/Header.vue";
import { ref, onMounted, onBeforeUnmount } from "vue";

// 侧边栏设置
const asideSettings = ref({
  isCollapse: true,
  width: 64,
})

// 动态宽高
const pageWidth = ref(window.innerWidth);
const pageHeight = ref(window.innerHeight);

// 处理侧边栏切换
const changeAside = () => {
  asideSettings.value.isCollapse = !asideSettings.value.isCollapse;
  if (asideSettings.value.isCollapse) {
    asideSettings.value.width = 64;
  } else {
    asideSettings.value.width = 149;
  }
}

// 监听窗口大小变化
const updatePageSize = () => {
  pageWidth.value = window.innerWidth;
  pageHeight.value = window.innerHeight;
}

onMounted(() => {
  window.addEventListener('resize', updatePageSize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', updatePageSize);
});
</script>

<template>
  <el-container class="screen-wrapper" style="min-height: 100vh; min-width: 100vw; overflow: hidden;">
    <Aside :collapse="asideSettings.isCollapse" :width="asideSettings.width"></Aside>
    <div :style="{ height: pageHeight + 'px', width: pageWidth - asideSettings.width + 'px', display: 'flex', flexDirection: 'column' }">
      <Header :isCollapse="asideSettings.isCollapse" @changeAside="changeAside" :header-width="pageWidth - asideSettings.width"></Header>
      <div class="body-wrapper">
        <RouterView :style="{ width: pageWidth - asideSettings.width + 'px' }"></RouterView>
      </div>
    </div>
  </el-container>
</template>

<style scoped>
.body-wrapper {
  display: flex;
  height: 92vh;
  transition: width 0.7s ease;
}
</style>
