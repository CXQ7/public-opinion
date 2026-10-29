<script setup>
import "@/assets/iconfont/iconfont.css"

const props = defineProps({
  heat: {
    type: Number,
    default: 0
  },
  //放大率，调整不同场景下的heat
  enlargeRatio: {
    type: Number,
    default: 1
  },
});

const getColorByHeat = () => {
  let heat = props.heat * props.enlargeRatio;
  if (heat > 100000) return 'rgb(143, 70, 154)'; // 深紫色
  if (heat > 50000) return 'rgb(151, 90, 220)'; // 紫色
  if (heat > 35000) return 'rgb(245, 79, 46)'; // 红
  if (heat > 20000) return 'rgb(255, 127, 127)'; // 浅红
  if (heat > 12000) return 'rgb(255, 145, 12)'; // 橙色
  if (heat > 8000) return 'rgb(255, 193, 15)'; // 黄色
  if (heat > 4000) return 'rgb(135, 206, 250)'; // 天蓝
  return 'rgb(19, 148, 246)'; // 蓝
};



</script>

<template>
  <span class="iconfont icon-huomiao" :style="{ color: getColorByHeat() }">&nbsp;&nbsp;{{Math.floor(props.heat)}}</span>
</template>

<style scoped>

</style>
