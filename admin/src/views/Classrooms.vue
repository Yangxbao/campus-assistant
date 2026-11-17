<template>
  <el-card shadow="never">
    <!-- 筛选区 -->
    <div class="toolbar">
      <el-select v-model="query.buildingId" placeholder="楼宇" clearable style="width: 160px" @change="onBuildingChange">
        <el-option v-for="b in buildings" :key="b.id" :label="`${b.code} ${b.name}`" :value="b.id" />
      </el-select>
      <el-select v-model="query.floorId" placeholder="楼层" clearable style="width: 140px">
        <el-option v-for="f in floorOptions" :key="f.id" :label="`第 ${f.floorNo} 层`" :value="f.id" />
      </el-select>
      <el-input v-model="query.roomNo" placeholder="教室编号" clearable style="width: 140px" @keyup.enter="load" />
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 140px">
        <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="load">
        <el-icon><Search /></el-icon>查询
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer" />
      <el-button @click="openImport"><el-icon><Upload /></el-icon>批量导入</el-button>
      <el-button :loading="exporting" @click="onExport"><el-icon><Download /></el-icon>批量导出</el-button>
      <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新建教室</el-button>
    </div>

    <!-- 列表 -->
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="教室" min-width="120" fixed="left">
        <template #default="{ row }">{{ row.roomNo }}{{ row.name ? ` (${row.name})` : '' }}</template>
      </el-table-column>
      <el-table-column label="楼宇" min-width="120">
        <template #default="{ row }">{{ row.buildingName ?? row.buildingId }}</template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ TYPE_LABELS[row.type as ClassroomType] ?? row.type }}</template>
      </el-table-column>
      <el-table-column label="座位" prop="seatCount" width="70" />
      <el-table-column label="设备" min-width="220">
        <template #default="{ row }">
          <el-tag v-if="row.hasAc" size="small" class="dev-tag">空调</el-tag>
          <el-tag v-if="row.hasProjector" size="small" class="dev-tag" type="success">投影</el-tag>
          <el-tag v-if="row.hasPc" size="small" class="dev-tag" type="warning">电脑</el-tag>
          <el-tag v-if="row.hasMic" size="small" class="dev-tag" type="danger">话筒</el-tag>
          <el-tag v-if="row.hasWhiteboard" size="small" class="dev-tag" type="info">白板</el-tag>
          <el-tag v-if="row.hasWifi" size="small" class="dev-tag">WiFi</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="插座" prop="socketCount" width="70" />
      <el-table-column label="安静评分" prop="envScoreQuiet" width="90" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openPhotos(row)">照片</el-button>
          <el-button link type="success" @click="openQrcode(row)">二维码</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无教室数据" />
      </template>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      class="pager"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :page-sizes="[10, 20, 50]"
      @change="load"
    />
  </el-card>

  <!-- 新建/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑教室' : '新建教室'" width="760px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-divider content-position="left">基础信息</el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="楼宇" prop="buildingId">
            <el-select v-model="form.buildingId" style="width: 100%" @change="form.floorId = undefined">
              <el-option v-for="b in buildings" :key="b.id" :label="`${b.code} ${b.name}`" :value="b.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="楼层" prop="floorId">
            <el-select v-model="form.floorId" style="width: 100%" placeholder="选择楼层">
              <el-option v-for="f in formFloors" :key="f.id" :label="`第 ${f.floorNo} 层`" :value="f.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="教室编号" prop="roomNo">
            <el-input v-model="form.roomNo" placeholder="如 301" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="教室名称">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="座位数">
            <el-input-number v-model="form.seatCount" :min="1" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">设备与插座</el-divider>
      <el-row :gutter="12">
        <el-col :span="24">
          <el-form-item label="设备开关">
            <el-checkbox v-model="form.hasAc">空调</el-checkbox>
            <el-checkbox v-model="form.hasProjector">投影</el-checkbox>
            <el-checkbox v-model="form.hasPc">电脑</el-checkbox>
            <el-checkbox v-model="form.hasMic">话筒</el-checkbox>
            <el-checkbox v-model="form.hasWhiteboard">白板</el-checkbox>
            <el-checkbox v-model="form.hasWifi">WiFi</el-checkbox>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="插座数量">
            <el-input-number v-model="form.socketCount" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="插座描述">
            <el-input v-model="form.socketDesc" placeholder="文字描述插座位置" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="其他设备">
            <el-input v-model="form.otherDevices" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">环境描述</el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="安静评分">
            <el-rate v-model="form.envScoreQuiet" :max="5" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="环境描述">
            <el-input v-model="form.envDesc" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="桌椅描述">
            <el-input v-model="form.deskDesc" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="采光描述">
            <el-input v-model="form.lightDesc" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="通风描述">
            <el-input v-model="form.ventilationDesc" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="卫生描述">
            <el-input v-model="form.cleanDesc" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 批量导入对话框 -->
  <el-dialog v-model="importVisible" title="批量导入教室" width="720px" destroy-on-close>
    <el-upload
      drag
      accept=".xlsx,.csv"
      :auto-upload="false"
      :limit="1"
      :on-change="onFileChange"
      :on-remove="() => (importFile = null)"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">仅支持 .xlsx / .csv，表头模板见 docs/教室批量导入模板.csv</div>
      </template>
    </el-upload>
    <div class="import-actions">
      <el-button type="primary" :loading="importing" :disabled="!importFile" @click="onImport">开始导入</el-button>
    </div>
    <template v-if="importResult">
      <el-alert
        :type="importResult.failCount > 0 ? 'warning' : 'success'"
        :title="`共 ${importResult.total ?? '-'} 行，成功 ${importResult.successCount ?? '-'} 行，失败 ${importResult.failCount ?? '-'} 行`"
        show-icon
        :closable="false"
        class="import-alert"
      />
      <el-table v-if="(importResult.details ?? []).length" :data="importResult.details" size="small" max-height="280">
        <el-table-column label="行号" prop="row" width="70" />
        <el-table-column label="楼编号" prop="buildingCode" width="90" />
        <el-table-column label="教室编号" prop="roomNo" width="100" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="message" min-width="180" show-overflow-tooltip />
      </el-table>
    </template>
  </el-dialog>

  <!-- 二维码对话框 -->
  <el-dialog v-model="qrcodeVisible" :title="`教室二维码 - ${qrcodeRoom}`" width="420px" destroy-on-close>
    <div v-loading="qrcodeLoading" class="qrcode-box">
      <img v-if="qrcodeImg" :src="qrcodeImg" alt="教室二维码" class="qrcode-img" />
      <el-empty v-else-if="!qrcodeLoading" description="二维码获取失败" />
    </div>
    <template #footer>
      <el-button type="primary" :disabled="!qrcodeImg" @click="downloadQrcode">下载二维码</el-button>
    </template>
  </el-dialog>

  <!-- 照片管理对话框 -->
  <el-dialog v-model="photosVisible" :title="`教室照片 - ${photosRoom}`" width="860px" destroy-on-close>
    <div v-loading="photosLoading" class="photo-manager">
      <!-- 上传区 -->
      <div class="photo-upload-row">
        <el-select v-model="photoCategory" style="width: 150px" placeholder="照片分类">
          <el-option v-for="(label, key) in IMAGE_CATEGORY_LABELS" :key="key" :label="label" :value="key" />
        </el-select>
        <el-input
          v-model="photoDesc"
          placeholder="照片描述（可选，如：讲台侧面插座）"
          maxlength="100"
          style="flex: 1"
        />
        <el-upload
          :show-file-list="false"
          accept="image/jpeg,image/png,image/webp"
          :before-upload="beforePhotoUpload"
          :http-request="(opt: any) => onPhotoUpload(opt.file)"
        >
          <el-button type="primary" :loading="photoUploading">
            <el-icon><Upload /></el-icon>选择图片上传
          </el-button>
        </el-upload>
      </div>
      <div class="photo-tip">仅支持 jpg / png / webp，不超过 8MB；管理员上传的照片直接发布，无需审核。</div>

      <!-- 照片网格 -->
      <el-empty v-if="!photos.length && !photosLoading" description="暂无照片，上传第一张吧" />
      <div v-else class="photo-grid">
        <div v-for="img in photos" :key="img.id" class="photo-card">
          <el-image
            :src="img.url"
            :preview-src-list="photos.map((p) => p.url)"
            :initial-index="photos.findIndex((p) => p.id === img.id)"
            fit="cover"
            class="photo-img"
            preview-teleported
          />
          <div class="photo-meta">
            <el-tag size="small" type="info">{{ IMAGE_CATEGORY_LABELS[img.category] ?? img.category }}</el-tag>
            <el-tag size="small" :type="img.auditStatus === 'APPROVED' ? 'success' : 'warning'">
              {{ img.auditStatus === 'APPROVED' ? '已发布' : '待审核' }}
            </el-tag>
            <el-button link type="danger" size="small" class="photo-del" @click="onPhotoDelete(img)">删除</el-button>
          </div>
          <div v-if="img.description" class="photo-desc">{{ img.description }}</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import {
  createClassroom,
  deleteClassroom,
  deleteClassroomImage,
  exportClassrooms,
  getClassroomQrcode,
  importClassrooms,
  listBuildings,
  listClassroomImages,
  listClassrooms,
  updateClassroom,
  uploadClassroomImage,
} from '@/api/admin'
import type { AdminClassroomImage, Building, Classroom, ClassroomImageCategory, ClassroomType, ImportResult } from '@/types/api'

const TYPE_LABELS: Record<ClassroomType, string> = {
  LINEAR: '大教室',
  SMALL: '小教室',
  LAB: '实验室',
  MULTIMEDIA: '多媒体',
  OTHER: '其他',
}

const buildings = ref<Building[]>([])
const rows = ref<Classroom[]>([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const query = reactive({
  buildingId: undefined as number | undefined,
  floorId: undefined as number | undefined,
  roomNo: '',
  type: '' as ClassroomType | '',
  page: 1,
  size: 10,
})

const floorOptions = computed(() => {
  const b = buildings.value.find((x) => x.id === query.buildingId)
  return b?.floors ?? []
})

const formFloors = computed(() => {
  const b = buildings.value.find((x) => x.id === form.buildingId)
  return b?.floors ?? []
})

const form = reactive<Partial<Classroom>>({})
const rules: FormRules = {
  buildingId: [{ required: true, message: '请选择楼宇', trigger: 'change' }],
  roomNo: [{ required: true, message: '请输入教室编号', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}

function resetQuery(): void {
  query.buildingId = undefined
  query.floorId = undefined
  query.roomNo = ''
  query.type = ''
  query.page = 1
  load()
}

function onBuildingChange(): void {
  query.floorId = undefined
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await listClassrooms({ ...query, roomNo: query.roomNo || undefined })
    rows.value = page.list ?? []
    total.value = page.total ?? 0
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

function fillForm(row?: Classroom): void {
  Object.assign(form, {
    id: row?.id,
    buildingId: row?.buildingId,
    floorId: row?.floorId,
    roomNo: row?.roomNo ?? '',
    name: row?.name ?? '',
    type: row?.type ?? 'SMALL',
    seatCount: row?.seatCount ?? undefined,
    hasAc: row?.hasAc ?? false,
    socketDesc: row?.socketDesc ?? '',
    socketCount: row?.socketCount ?? undefined,
    hasProjector: row?.hasProjector ?? false,
    hasPc: row?.hasPc ?? false,
    hasMic: row?.hasMic ?? false,
    hasWhiteboard: row?.hasWhiteboard ?? false,
    hasWifi: row?.hasWifi ?? false,
    otherDevices: row?.otherDevices ?? '',
    envScoreQuiet: row?.envScoreQuiet ?? 0,
    envDesc: row?.envDesc ?? '',
    deskDesc: row?.deskDesc ?? '',
    lightDesc: row?.lightDesc ?? '',
    ventilationDesc: row?.ventilationDesc ?? '',
    cleanDesc: row?.cleanDesc ?? '',
  })
}

function openCreate(): void {
  editingId.value = null
  fillForm()
  dialogVisible.value = true
}

function openEdit(row: Classroom): void {
  editingId.value = row.id ?? null
  fillForm(row)
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateClassroom(editingId.value, form)
      ElMessage.success('教室已更新')
    } else {
      await createClassroom(form)
      ElMessage.success('教室已创建')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示已统一处理
  } finally {
    saving.value = false
  }
}

async function onDelete(row: Classroom): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除教室「${row.roomNo}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteClassroom(row.id!)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 错误提示已统一处理
  }
}

// ==================== 批量导入 ====================

const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<ImportResult | null>(null)

function openImport(): void {
  importFile.value = null
  importResult.value = null
  importVisible.value = true
}

function onFileChange(file: UploadFile): void {
  importFile.value = (file.raw as File) ?? null
}

async function onImport(): Promise<void> {
  if (!importFile.value) return
  importing.value = true
  try {
    importResult.value = await importClassrooms(importFile.value)
    ElMessage.success('导入完成')
    await load()
  } catch {
    // 错误提示已统一处理
  } finally {
    importing.value = false
  }
}

// ==================== 批量导出 ====================

async function onExport(): Promise<void> {
  exporting.value = true
  try {
    const blob = await exportClassrooms()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'classrooms.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // 错误提示已统一处理
  } finally {
    exporting.value = false
  }
}

// ==================== 二维码 ====================

const qrcodeVisible = ref(false)
const qrcodeLoading = ref(false)
const qrcodeImg = ref('')
const qrcodeRoom = ref('')

async function openQrcode(row: Classroom): Promise<void> {
  qrcodeVisible.value = true
  qrcodeLoading.value = true
  qrcodeImg.value = ''
  qrcodeRoom.value = row.roomNo
  try {
    const base64 = await getClassroomQrcode(row.id!)
    qrcodeImg.value = base64.startsWith('data:') ? base64 : `data:image/png;base64,${base64}`
  } catch {
    // 错误提示已统一处理
  } finally {
    qrcodeLoading.value = false
  }
}

function downloadQrcode(): void {
  const a = document.createElement('a')
  a.href = qrcodeImg.value
  a.download = `qrcode-${qrcodeRoom.value}.png`
  a.click()
}

// ==================== 教室照片管理 ====================

const IMAGE_CATEGORY_LABELS: Record<string, string> = {
  OVERALL: '整体',
  FRONT: '前排',
  BACK: '后排',
  SOCKET: '插座',
  AC: '空调',
  PROJECTOR: '投影',
  PC: '电脑',
  WHITEBOARD: '白板',
  OTHER: '其他',
}

const photosVisible = ref(false)
const photosLoading = ref(false)
const photosRoom = ref('')
const photosId = ref<number | null>(null)
const photos = ref<AdminClassroomImage[]>([])
const photoCategory = ref<ClassroomImageCategory>('OVERALL')
const photoDesc = ref('')
const photoUploading = ref(false)

function openPhotos(row: Classroom): void {
  photosId.value = row.id ?? null
  photosRoom.value = row.roomNo
  photos.value = []
  photosVisible.value = true
  if (photosId.value) loadPhotos()
}

async function loadPhotos(): Promise<void> {
  if (!photosId.value) return
  photosLoading.value = true
  try {
    photos.value = (await listClassroomImages(photosId.value)) ?? []
  } catch {
    // 错误提示已统一处理
  } finally {
    photosLoading.value = false
  }
}

/** 上传前校验：类型与大小 */
function beforePhotoUpload(file: File): boolean {
  const okTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!okTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg / png / webp 格式')
    return false
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.error('文件超过 8MB 限制')
    return false
  }
  return true
}

async function onPhotoUpload(file: File): Promise<void> {
  if (!photosId.value) return
  if (!beforePhotoUpload(file)) return
  photoUploading.value = true
  try {
    await uploadClassroomImage(photosId.value, file, photoCategory.value, photoDesc.value || undefined)
    ElMessage.success('上传成功，已直接发布')
    photoDesc.value = ''
    await loadPhotos()
  } catch {
    // 错误提示已统一处理
  } finally {
    photoUploading.value = false
  }
}

async function onPhotoDelete(img: AdminClassroomImage): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    '删除后小程序端将不再展示该照片，确定删除吗？',
    '删除照片',
    { type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteClassroomImage(img.id)
    ElMessage.success('已删除')
    await loadPhotos()
  } catch {
    // 错误提示已统一处理
  }
}

onMounted(async () => {
  buildings.value = (await listBuildings().catch(() => [])) ?? []
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.spacer {
  flex: 1;
}

.dev-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.import-actions {
  margin-top: 12px;
}

.import-alert {
  margin: 12px 0;
}

.qrcode-box {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrcode-img {
  width: 240px;
  height: 240px;
}

.photo-manager {
  min-height: 200px;
}

.photo-upload-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.photo-tip {
  margin: 8px 0 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.photo-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
  background: var(--el-fill-color-blank);
}

.photo-img {
  width: 100%;
  height: 140px;
  display: block;
}

.photo-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px 2px;
}

.photo-del {
  margin-left: auto;
}

.photo-desc {
  padding: 2px 8px 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
