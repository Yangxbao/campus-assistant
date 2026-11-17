<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="load">
        <el-radio-button value="PENDING">待处理</el-radio-button>
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="HANDLED">已处理</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
      </el-radio-group>
      <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="举报人" width="130">
        <template #default="{ row }">{{ row.reporterName ?? (row.reporterId ? `用户#${row.reporterId}` : '-') }}</template>
      </el-table-column>
      <el-table-column label="举报对象" min-width="160">
        <template #default="{ row }">
          {{ TARGET_TYPE_LABELS[row.targetType] ?? row.targetType }} #{{ row.targetId }}
        </template>
      </el-table-column>
      <el-table-column label="举报原因" width="110">
        <template #default="{ row }">{{ REASON_LABELS[row.reasonType] ?? row.reasonType }}</template>
      </el-table-column>
      <el-table-column label="详细描述" prop="reasonDesc" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status as ReportStatus)" size="small">{{ STATUS_LABELS[row.status as ReportStatus] ?? row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理结果" prop="handleResult" min-width="180" show-overflow-tooltip />
      <el-table-column label="举报时间" prop="createdAt" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            link
            type="primary"
            @click="openHandle(row)"
          >处理</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无举报记录" />
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      @current-change="load"
    />
  </el-card>

  <!-- 处理对话框 -->
  <el-dialog v-model="handleVisible" title="举报处理" width="480px">
    <el-form label-width="90px">
      <el-form-item label="处理结果">
        <el-radio-group v-model="handleForm.status">
          <el-radio value="HANDLED">受理（违规成立）</el-radio>
          <el-radio value="REJECTED">驳回（举报无效）</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="处理说明">
        <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="处理说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleVisible = false">取消</el-button>
      <el-button type="primary" :loading="handling" @click="onHandle">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { handleReport, listReports } from '@/api/admin'
import type { ReportItem, ReportStatus } from '@/types/api'

const TARGET_TYPE_LABELS: Record<string, string> = {
  CLASSROOM_IMAGE: '教室照片',
  LOST_ITEM: '失物信息',
  REVIEW: '教室评价',
  ROOM_STATUS: '教室状态',
  USER: '用户',
}

const REASON_LABELS: Record<string, string> = {
  PORN: '低俗色情',
  AD: '广告刷屏',
  PRIVACY: '隐私泄露',
  FALSE: '虚假信息',
  OTHER: '其他',
}

const STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: '待处理',
  HANDLED: '已处理',
  REJECTED: '已驳回',
}

const status = ref<ReportStatus | 'PENDING' | ''>('PENDING')
const rows = ref<ReportItem[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const handleVisible = ref(false)
const handling = ref(false)
const current = ref<ReportItem | null>(null)
const handleForm = reactive<{ status: 'HANDLED' | 'REJECTED'; handleResult: string }>({
  status: 'HANDLED',
  handleResult: '',
})

function statusTagType(s: ReportStatus): 'warning' | 'success' | 'info' {
  if (s === 'PENDING') return 'warning'
  if (s === 'HANDLED') return 'success'
  return 'info'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listReports(status.value === '' ? undefined : status.value)
    rows.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

function openHandle(row: ReportItem): void {
  current.value = row
  handleForm.status = 'HANDLED'
  handleForm.handleResult = ''
  handleVisible.value = true
}

async function onHandle(): Promise<void> {
  if (!current.value) return
  handling.value = true
  try {
    await handleReport(current.value.id, {
      status: handleForm.status,
      handleResult: handleForm.handleResult || undefined,
    })
    ElMessage.success('举报已处理')
    handleVisible.value = false
    await load()
  } catch {
    // 错误提示已统一处理
  } finally {
    handling.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
