<template>
  <div v-loading="loading" class="dashboard">
    <!-- 概览卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="4" :lg="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="20" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value ?? '-' }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 每日趋势图 -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <span>近 30 天运营趋势</span>
      </template>
      <div ref="chartRef" class="chart" />
    </el-card>

    <!-- 贡献排行 -->
    <el-card shadow="never">
      <template #header>
        <span>贡献排行</span>
      </template>
      <el-table :data="contributors" v-loading="contributorLoading" stripe>
        <el-table-column label="排名" width="80" type="index" :index="rankIndex" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">{{ row.nickname ?? `用户#${row.userId ?? row.id}` }}</template>
        </el-table-column>
        <el-table-column label="贡献次数" prop="contributeCount" width="160" sortable />
      </el-table>
      <el-empty v-if="!contributorLoading && contributors.length === 0" description="暂无贡献数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getContributors, getStatsDaily, getStatsOverview } from '@/api/admin'
import type { Contributor, StatsOverview } from '@/types/api'

const loading = ref(false)
const contributorLoading = ref(false)
const overview = reactive<StatsOverview>({})
const contributors = ref<Contributor[]>([])

const cards = computed(() => [
  { label: '用户数', value: overview.userCount, icon: 'User', color: '#409eff' },
  { label: '楼宇数', value: overview.buildingCount, icon: 'OfficeBuilding', color: '#67c23a' },
  { label: '教室数', value: overview.classroomCount, icon: 'School', color: '#e6a23c' },
  { label: '照片数', value: overview.photoCount, icon: 'Picture', color: '#f56c6c' },
  { label: '失物数', value: overview.lostItemCount, icon: 'Search', color: '#9a66ff' },
  { label: '找回数', value: overview.recoveredCount, icon: 'CircleCheck', color: '#00c1a2' },
])

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function rankIndex(index: number): number {
  return index + 1
}

async function loadOverview(): Promise<void> {
  const data = await getStatsOverview()
  Object.assign(overview, data ?? {})
}

async function loadDaily(): Promise<void> {
  const list = (await getStatsDaily(30)) ?? []
  chart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增教室资料', '失物发布', '审核量'] },
    grid: { left: 40, right: 24, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: list.map((i) => i.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '新增教室资料', type: 'line', smooth: true, data: list.map((i) => i.classroomCount ?? 0) },
      { name: '失物发布', type: 'line', smooth: true, data: list.map((i) => i.lostItemCount ?? 0) },
      { name: '审核量', type: 'bar', barMaxWidth: 18, data: list.map((i) => i.auditCount ?? 0) },
    ],
  })
}

async function loadContributors(): Promise<void> {
  contributors.value = (await getContributors()) ?? []
}

function onResize(): void {
  chart?.resize()
}

onMounted(async () => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    window.addEventListener('resize', onResize)
  }
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadDaily(), loadContributors()])
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 300px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.chart-card .chart {
  height: 360px;
  width: 100%;
}
</style>
