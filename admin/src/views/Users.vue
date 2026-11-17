<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索昵称 / ID"
        clearable
        style="width: 220px"
        @keyup.enter="load"
      />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已封禁" value="BANNED" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="昵称" min-width="140">
        <template #default="{ row }">{{ row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="110">
        <template #default="{ row }">{{ roleLabel(row.role) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '正常' : '已封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="贡献次数" prop="contributeCount" width="100" sortable />
      <el-table-column label="最近登录" prop="lastLoginTime" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="onToggleStatus(row)">封禁</el-button>
          <el-button v-else link type="success" @click="onToggleStatus(row)">解封</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户" />
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

  <!-- 详情抽屉 -->
  <el-drawer v-model="detailVisible" title="用户详情" size="480px">
    <div v-loading="detailLoading">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ detail?.id ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail?.nickname ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleLabel(detail?.role) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail?.status === 'ACTIVE' ? '正常' : '已封禁' }}
        </el-descriptions-item>
        <el-descriptions-item label="贡献次数">{{ detail?.contributeCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="最近登录">{{ detail?.lastLoginTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail?.createdAt ?? '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">违规记录</el-divider>
      <el-table v-if="(detail?.violations ?? []).length" :data="detail?.violations" size="small">
        <el-table-column v-for="col in detailCols(detail?.violations ?? [])" :key="col" :prop="col" :label="col" min-width="120" />
        <template #empty><el-empty description="无违规记录" /></template>
      </el-table>
      <el-empty v-else description="无违规记录" :image-size="60" />

      <el-divider content-position="left">举报记录</el-divider>
      <el-table v-if="(detail?.reports ?? []).length" :data="detail?.reports" size="small">
        <el-table-column v-for="col in detailCols(detail?.reports ?? [])" :key="col" :prop="col" :label="col" min-width="120" />
        <template #empty><el-empty description="无举报记录" /></template>
      </el-table>
      <el-empty v-else description="无举报记录" :image-size="60" />

      <el-divider content-position="left">贡献记录</el-divider>
      <el-table v-if="(detail?.contributions ?? []).length" :data="detail?.contributions" size="small">
        <el-table-column v-for="col in detailCols(detail?.contributions ?? [])" :key="col" :prop="col" :label="col" min-width="120" />
        <template #empty><el-empty description="无贡献记录" /></template>
      </el-table>
      <el-empty v-else description="无贡献记录" :image-size="60" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserDetail, listUsers, updateUserStatus } from '@/api/admin'
import type { UserDetail, UserItem, UserStatus } from '@/types/api'

const rows = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const statusFilter = ref<UserStatus | ''>('')
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<UserDetail | null>(null)

function roleLabel(role?: string): string {
  if (!role) return '学生'
  if (role === 'auditor') return '审核员'
  if (role === 'maintainer') return '维护员'
  return role
}

/** 记录字段结构未知，取前 4 个字段动态渲染列 */
function detailCols(list: Array<Record<string, unknown>>): string[] {
  if (!list.length) return []
  return Object.keys(list[0]).slice(0, 4)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listUsers({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    rows.value = res.list ?? []
    total.value = res.total ?? 0
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

async function openDetail(row: UserItem): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getUserDetail(row.id)
  } catch {
    // 错误提示已统一处理
  } finally {
    detailLoading.value = false
  }
}

async function onToggleStatus(row: UserItem): Promise<void> {
  const banning = row.status === 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      `确定${banning ? '封禁' : '解封'}用户「${row.nickname ?? row.id}」吗？`,
      `${banning ? '封禁' : '解封'}确认`,
      { type: 'warning' },
    )
  } catch {
    return
  }
  const next: UserStatus = banning ? 'BANNED' : 'ACTIVE'
  try {
    await updateUserStatus(row.id, next)
    ElMessage.success(`已${banning ? '封禁' : '解封'}`)
    await load()
  } catch {
    // 错误提示已统一处理
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
