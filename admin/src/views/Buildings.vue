<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新建楼宇
      </el-button>
      <el-button @click="load">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <el-table :data="buildings" v-loading="loading" stripe>
      <el-table-column label="楼编号" prop="code" width="100" />
      <el-table-column label="楼名" prop="name" min-width="140" />
      <el-table-column label="校区" prop="campus" width="120" />
      <el-table-column label="楼层数" prop="floorsCount" width="90" />
      <el-table-column label="教室数" prop="classroomCount" width="90" />
      <el-table-column label="位置说明" prop="locationDesc" min-width="160" show-overflow-tooltip />
      <el-table-column label="描述" prop="description" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无楼宇" />
      </template>
    </el-table>
  </el-card>

  <!-- 新建/编辑对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="editing ? '编辑楼宇' : '新建楼宇'"
    width="640px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="楼编号" prop="code">
            <el-input v-model="form.code" placeholder="如 J1" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="楼名" prop="name">
            <el-input v-model="form.name" placeholder="如 教学楼一" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="校区" prop="campus">
            <el-input v-model="form.campus" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="楼层数" prop="floorsCount">
            <el-input-number v-model="form.floorsCount" :min="1" :max="60" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="位置说明">
        <el-input v-model="form.locationDesc" placeholder="文字位置说明" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>

      <el-divider content-position="left">楼层维护</el-divider>
      <el-table :data="form.floors" size="small">
        <el-table-column label="楼层号" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.floorNo" :min="-5" :max="60" controls-position="right" style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="楼层描述">
          <template #default="{ row }">
            <el-input v-model="row.description" placeholder="如：多媒体教室集中层" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeFloor($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button class="add-floor" size="small" @click="addFloor">
        <el-icon><Plus /></el-icon>添加楼层
      </el-button>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createBuilding, deleteBuilding, listBuildings, updateBuilding } from '@/api/admin'
import type { Building, Floor } from '@/types/api'

const buildings = ref<Building[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<Building | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<{
  code: string
  name: string
  campus: string
  locationDesc: string
  description: string
  floorsCount: number
  floors: Floor[]
}>({
  code: '',
  name: '',
  campus: '',
  locationDesc: '',
  description: '',
  floorsCount: 1,
  floors: [],
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入楼编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入楼名', trigger: 'blur' }],
  floorsCount: [{ required: true, message: '请输入楼层数', trigger: 'blur' }],
}

function addFloor(): void {
  form.floors.push({ floorNo: form.floors.length + 1, description: '' })
}

function removeFloor(index: number): void {
  form.floors.splice(index, 1)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    buildings.value = (await listBuildings()) ?? []
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editing.value = null
  Object.assign(form, { code: '', name: '', campus: '', locationDesc: '', description: '', floorsCount: 1, floors: [] })
  dialogVisible.value = true
}

function openEdit(row: Building): void {
  editing.value = row
  Object.assign(form, {
    code: row.code,
    name: row.name,
    campus: row.campus ?? '',
    locationDesc: row.locationDesc ?? '',
    description: row.description ?? '',
    floorsCount: row.floorsCount,
    floors: (row.floors ?? []).map((f) => ({ floorNo: f.floorNo, description: f.description ?? '' })),
  })
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: Partial<Building> = {
      code: form.code,
      name: form.name,
      campus: form.campus,
      locationDesc: form.locationDesc,
      description: form.description,
      floorsCount: form.floorsCount,
      floors: form.floors,
    }
    if (editing.value) {
      await updateBuilding(editing.value.id, payload)
      ElMessage.success('楼宇已更新')
    } else {
      await createBuilding(payload)
      ElMessage.success('楼宇已创建')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示已统一处理
  } finally {
    saving.value = false
  }
}

async function onDelete(row: Building): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除楼宇「${row.name}」吗？删除后不可恢复展示。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteBuilding(row.id)
    ElMessage.success('已删除')
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
  margin-bottom: 12px;
}

.add-floor {
  margin-top: 8px;
}
</style>
