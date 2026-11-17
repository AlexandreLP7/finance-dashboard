<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  title: string
  option: echarts.EChartsOption
}>()

const el = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

onMounted(() => {
  if (el.value) {
    chart = echarts.init(el.value)
    chart.setOption(props.option)
  }
})

watch(
  () => props.option,
  (option) => {
    chart?.setOption(option, true)
  },
  { deep: true }
)

onBeforeUnmount(() => {
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="chart-card">
    <h3>{{ title }}</h3>
    <div
      ref="el"
      class="chart"
    />
  </div>
</template>

<style scoped>
.chart-card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 0.5rem;
  padding: 1rem;
}
.chart {
  width: 100%;
  height: 320px;
}
</style>
