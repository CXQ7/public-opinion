<script setup>
import { ref } from 'vue'
import {
  Document,
  Menu as IconMenu,
  Location,
  Setting,
} from '@element-plus/icons-vue'

const settings = defineProps({
  collapse: Boolean,
  width: String
})

</script>

<template>
<!--  <el-radio-group v-model="isCollapse" style="margin-bottom: 20px">-->
<!--    <el-radio-button :label="false">expand</el-radio-button>-->
<!--    <el-radio-button :label="true">collapse</el-radio-button>-->
<!--  </el-radio-group>-->
  <el-aside id="aside" :width="width">
    <el-scrollbar>
      <el-menu
          default-active="2"
          class="el-menu-vertical-demo dark-mode"
          :collapse="settings.collapse"
          active-text-color="#ffd04b"
          background-color="#333a43"
          text-color="#fff"
          router="true"
      >
        <el-menu-item index="/">
          <div class="logo"></div>
          <template #title>&nbsp;天眼</template>
        </el-menu-item>
        <el-menu-item index="/config">
          <el-icon><document /></el-icon>
          <template #title>我的配置</template>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
        <el-menu-item index="4">
          <el-icon><setting /></el-icon>
          <template #title>设置</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<style scoped>
.el-aside {
  height: 100vh;
  overflow: hidden;
  box-shadow: var(--el-box-shadow-lighter);
}

.el-scrollbar {
  height: 100vh;
  overflow: hidden;
  background-color: #333a43;
}

.el-menu {
  border-right: none;
}

.logo{
  height: 1.8vw;
  width: 1.8vw;
  background-image: url("/logo5.png");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}
</style>
