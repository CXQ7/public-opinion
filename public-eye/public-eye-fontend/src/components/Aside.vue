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
        <el-sub-menu index="1" popper-class="dark-mode">
          <template #title>
            <div class="logo"></div>
            <span>天眼</span>
          </template>
          <el-menu-item-group>
            <template #title><span>Group One</span></template>
            <el-menu-item index="1-1">item one</el-menu-item>
            <el-menu-item index="1-2">item two</el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="Group Two">
            <el-menu-item index="1-3">item three</el-menu-item>
          </el-menu-item-group>
          <el-sub-menu index="1-4">
            <template #title><span>item four</span></template>
            <el-menu-item index="1-4-1">item one</el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
        <el-menu-item index="/">
          <el-icon><icon-menu /></el-icon>
          <template #title>主页</template>
        </el-menu-item>
        <el-menu-item index="/config">
          <el-icon><document /></el-icon>
          <template #title>我的配置</template>
        </el-menu-item>
        <el-sub-menu index="5" popper-class="dark-mode">
          <template #title>
            <el-icon>
              <User />
            </el-icon><span>Home</span>
          </template>
          <el-menu-item index="/user">User Home</el-menu-item>
        </el-sub-menu>
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
  height: 2.2vw;
  width: 2.2vw;
  background-image: url("/logo5.png");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}
</style>
