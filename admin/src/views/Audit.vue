<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane v-for="t in visibleTabs" :key="t.value" :label="t.label" :name="t.value" />
    </el-tabs>

    <!-- 状态筛选（PENDING 为待审，其余为历史处理记录） -->
    <div class="toolbar">
      <el-radio-group v-model="status" @change="load">
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
        <el-radio-button value="OFFLINE">已下架</el-radio-button>
      </el-radio-group>
      <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="ID" width="80">
        <template #default="{ row }">{{ row.targetId ?? row.id }}</template>
      </el-table-column>
      <el-table-column label="提交人" width="120">
        <template #default="{ row }">{{ row.submitterName ?? (row.submitterId ? `用户#${row.submitterId}` : '-') }}</template>
      </el-table-column>
      <el-table-column label="内容摘要" min-width="280">
        <template #default="{ row }">
          <div class="summary">
            <!-- 图片类可预览大图 -->
            <el-image
              v-if="treatAsImage(row)"
              :src="row.url ?? row.ossKey"
              :preview-src-list="[row.url ?? row.ossKey]"
              preview-teleported
              fit="cover"
              class="thumb"
            />
            <span>{{ summaryOf(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(statusOf(row))" size="small">{{ STATUS_LABELS[statusOf(row)] ?? statusOf(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ row.createdAt ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="历史处理" min-width="180">
        <template #default="{ row }">
          <template v-if="row.auditRemark || row.auditTime">
            {{ row.auditRemark || '-' }}
            <span class="audit-time">{{ row.auditTime ?? '' }}</span>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="openHandle(row, 'APPROVE')">通过</el-button>
          <el-button link type="warning" @click="openHandle(row, 'REJECT')">驳回</el-button>
          <el-button v-if="treatAsImage(row) || row.targetType === 'LOST_ITEM' || row.targetType === 'ROOM_STATUS'" link type="info" @click="openHandle(row, 'OFFLINE')">下架</el-button>
          <el-button link type="danger" @click="openHandle(row, 'DELETE')">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="status === 'PENDING' ? '暂无待审内容' : '暂无记录'" />
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
  <el-dialog v-model="handleVisible" :title="`审核处理 - ${ACTION_LABELS[action]}`" width="480px">
    <el-form label-width="80px">
      <el-form-item label="处理动作">
        <el-tag>{{ ACTION_LABELS[action] }}</el-tag>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="remark" type="textarea" :rows="3" placeholder="处理备注（将通知提交者）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleVisible = false">取消</el-button>
      <el-button type="primary" :loading="handling" @click="onHandle">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { handleAudit, listAudits } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import type { AuditItem, AuditStatus, AuditTargetType } from '@/types/api'

const auth = useAuthStore()

const TABS: Array<{ value: AuditTargetType; label: string }> = [
  { value: 'CLASSROOM_IMAGE', label: '教室照片' },
  { value: 'EQUIPMENT_REPORT', label: '资料修正' },
  { value: 'LOST_ITEM', label: '失物信息' },
  { value: 'ROOM_STATUS', label: '教室状态' },
  { value: 'REPORT', label: '举报' },
]

const STATUS_LABELS: Record<AuditStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
}

const ACTION_LABELS: Record<string, string> = {
  APPROVE: '通过',
  REJECT: '驳回',
  OFFLINE: '下架',
  DELETE: '删除',
}

// MAINTAINER 仅可见教室资料类（教室照片/资料修正/教室状态），其余角色全部可见
const visibleTabs = computed(() => {
  if (auth.role === 'MAINTAINER') {
    return TABS.filter((t) => ['CLASSROOM_IMAGE', 'EQUIPMENT_REPORT', 'ROOM_STATUS'].includes(t.value))
  }
  return TABS
})

const activeTab = ref<AuditTargetType>('CLASSROOM_IMAGE')
const status = ref<AuditStatus | 'PENDING' | ''>('PENDING')
const rows = ref<AuditItem[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const handleVisible = ref(false)
const handling = ref(false)
const remark = ref('')
const action = ref<'APPROVE' | 'REJECT' | 'OFFLINE' | 'DELETE'>('APPROVE')
const current = ref<AuditItem | null>(null)

function onTabChange(): void {
  page.value = 1
  status.value = 'PENDING'
  load()
}

function statusOf(row: AuditItem): AuditStatus {
  return (row.auditStatus ?? row.status ?? 'PENDING') as AuditStatus
}

function statusTagType(s: AuditStatus): 'warning' | 'success' | 'danger' | 'info' {
  if (s === 'PENDING') return 'warning'
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

function treatAsImage(row: AuditItem): boolean {
  return row.targetType === 'CLASSROOM_IMAGE' && !!(row.url ?? row.ossKey)
}

function summaryOf(row: AuditItem): string {
  return (
    row.description ??
    row.remark ??
    row.content ??
    row.title ??
    row.reasonDesc ??
    (row.fieldName ? `${row.fieldName} → ${row.newValue ?? ''}` : '-') ??
    '-'
  )
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listAudits({
      targetType: activeTab.value,
      status: status.value === '' ? undefined : status.value,
      page: page.value,
      size,
    })
    rows.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

function openHandle(row: AuditItem, a: 'APPROVE' | 'REJECT' | 'OFFLINE' | 'DELETE'): void {
  current.value = row
  action.value = a
  remark.value = ''
  handleVisible.value = true
}

async function onHandle(): Promise<void> {
  const row = current.value
  if (!row) return
  handling.value = true
  try {
    await handleAudit({
      targetType: row.targetType,
      targetId: row.targetId ?? row.id,
      action: action.value,
      remark: remark.value || undefined,
    })
    ElMessage.success(`已${ACTION_LABELS[action.value]}`)
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

.summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  flex-shrink: 0;
}

.audit-time {
  color: #909399;
  font-size: 12px;
  margin-left: 6px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
