<template>
  <div v-loading="loading">
    <el-alert
      v-if="unknownConfigs.length"
      type="info"
      :closable="false"
      class="unknown-alert"
      :title="`另有 ${unknownConfigs.length} 个未分组配置项，已归入「其他」`"
    />

    <el-form label-width="200px">
      <el-card v-for="group in groups" :key="group.key" shadow="never" class="group-card">
        <template #header>
          <span>{{ group.label }}</span>
        </template>
        <template v-if="groupedConfigs[group.key]?.length">
          <el-form-item v-for="item in groupedConfigs[group.key]" :key="item.configKey" :label="item.configKey">
            <el-input v-model="item.editValue" :placeholder="item.remark || item.configKey" clearable>
              <template #suffix v-if="item.remark">
                <el-tooltip :content="item.remark" placement="top">
                  <el-icon><InfoFilled /></el-icon>
                </el-tooltip>
              </template>
            </el-input>
          </el-form-item>
        </template>
        <el-empty v-else description="暂无该分组配置" :image-size="60" />
      </el-card>
    </el-form>

    <div class="save-bar">
      <el-button type="primary" :loading="saving" @click="onSave">保存全部配置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listConfigs, updateConfigs } from '@/api/admin'
import type { SysConfig } from '@/types/api'

interface EditableConfig extends SysConfig {
  editValue: string
}

/** 配置分组（按 key 前缀归类，未匹配归「其他」） */
const groups = [
  { key: 'semester', label: '学期信息', prefixes: ['semester', 'term'] },
  { key: 'week', label: '教学周', prefixes: ['week'] },
  { key: 'period', label: '节次时间', prefixes: ['period', 'section', 'class_time'] },
  { key: 'upload', label: '上传限制', prefixes: ['upload'] },
  { key: 'expire', label: '内容过期', prefixes: ['expire', 'expiry', 'ttl'] },
  { key: 'ocr', label: 'OCR', prefixes: ['ocr'] },
  { key: 'storage', label: '存储', prefixes: ['storage', 'oss', 'minio'] },
  { key: 'wx', label: '微信配置', prefixes: ['wx', 'wechat', 'weixin'] },
  { key: 'other', label: '其他', prefixes: [] },
] as const

const configs = ref<EditableConfig[]>([])
const loading = ref(false)
const saving = ref(false)

function groupOf(key: string): string {
  const lower = key.toLowerCase()
  for (const g of groups) {
    if (g.prefixes.some((p) => lower.startsWith(p))) return g.key
  }
  return 'other'
}

const groupedConfigs = computed<Record<string, EditableConfig[]>>(() => {
  const map: Record<string, EditableConfig[]> = {}
  for (const g of groups) map[g.key] = []
  for (const c of configs.value) {
    map[groupOf(c.configKey)]?.push(c)
  }
  return map
})

const unknownConfigs = computed(() => groupedConfigs.value.other ?? [])

async function load(): Promise<void> {
  loading.value = true
  try {
    const list = (await listConfigs()) ?? []
    configs.value = list.map((c) => ({ ...c, editValue: c.configValue ?? '' }))
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

async function onSave(): Promise<void> {
  saving.value = true
  try {
    await updateConfigs(
      configs.value.map((c) => ({ configKey: c.configKey, configValue: c.editValue })),
    )
    ElMessage.success('配置保存成功')
    await load()
  } catch {
    // 错误提示已统一处理
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.group-card {
  margin-bottom: 16px;
}

.unknown-alert {
  margin-bottom: 16px;
}

.save-bar {
  position: sticky;
  bottom: 0;
  padding: 12px;
  background: #f0f2f5;
  text-align: right;
}
</style>
