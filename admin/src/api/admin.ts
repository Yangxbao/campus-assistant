/**
 * 管理后台全部接口（对应契约 docs/api-contract.md 第 3 节 /api/admin/**）
 * 路径与字段必须与契约完全一致，禁止在此之外发起请求。
 */
import { request } from './http'
import type {
  AdminClassroomImage,
  AdminRole,
  ApiResult,
  AuditItem,
  AuditStatus,
  AuditTargetType,
  Building,
  Classroom,
  ClassroomImageCategory,
  ClassroomType,
  Contributor,
  ImportResult,
  LoginResult,
  Page,
  ReportItem,
  ReportStatus,
  StatsDailyItem,
  StatsOverview,
  SysConfig,
  UserDetail,
  UserItem,
  UserStatus,
} from '@/types/api'

// ==================== 认证 ====================

/** POST /admin/auth/login */
export function login(data: { username: string; password: string }): Promise<LoginResult> {
  return request<LoginResult>({ url: '/admin/auth/login', method: 'post', data })
}

// ==================== 楼宇管理（含楼层） ====================

/** GET /admin/buildings */
export function listBuildings(): Promise<Building[]> {
  return request<Building[]>({ url: '/admin/buildings', method: 'get' })
}

/** POST /admin/buildings（含 floors） */
export function createBuilding(data: Partial<Building>): Promise<Building> {
  return request<Building>({ url: '/admin/buildings', method: 'post', data })
}

/** PUT /admin/buildings/{id}（含 floors） */
export function updateBuilding(id: number, data: Partial<Building>): Promise<Building> {
  return request<Building>({ url: `/admin/buildings/${id}`, method: 'put', data })
}

/** DELETE /admin/buildings/{id} */
export function deleteBuilding(id: number): Promise<void> {
  return request<void>({ url: `/admin/buildings/${id}`, method: 'delete' })
}

// ==================== 教室管理 ====================

export interface ClassroomQuery {
  buildingId?: number
  floorId?: number
  roomNo?: string
  type?: ClassroomType | ''
  page?: number
  size?: number
}

/** GET /admin/classrooms（分页筛选） */
export function listClassrooms(query: ClassroomQuery): Promise<Page<Classroom>> {
  return request<Page<Classroom>>({ url: '/admin/classrooms', method: 'get', params: query })
}

/** POST /admin/classrooms */
export function createClassroom(data: Partial<Classroom>): Promise<Classroom> {
  return request<Classroom>({ url: '/admin/classrooms', method: 'post', data })
}

/** PUT /admin/classrooms/{id} */
export function updateClassroom(id: number, data: Partial<Classroom>): Promise<Classroom> {
  return request<Classroom>({ url: `/admin/classrooms/${id}`, method: 'put', data })
}

/** DELETE /admin/classrooms/{id} */
export function deleteClassroom(id: number): Promise<void> {
  return request<void>({ url: `/admin/classrooms/${id}`, method: 'delete' })
}

/** POST /admin/classrooms/import（上传 xlsx/csv，返回成功/失败行明细） */
export function importClassrooms(file: File): Promise<ImportResult> {
  const form = new FormData()
  form.append('file', file)
  return request<ImportResult>({
    url: '/admin/classrooms/import',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** GET /admin/classrooms/export（导出 xlsx，二进制） */
export async function exportClassrooms(): Promise<Blob> {
  const res = await request<Blob>({
    url: '/admin/classrooms/export',
    method: 'get',
    responseType: 'blob',
  })
  // 拦截器对 blob 直接返回 response，此处运行时为 Blob 本体
  return res as unknown as Blob
}

/** GET /admin/classrooms/{id}/qrcode（返回 PNG base64） */
export function getClassroomQrcode(id: number): Promise<string> {
  return request<string | { base64?: string; image?: string; qrCode?: string }>({
    url: `/admin/classrooms/${id}/qrcode`,
    method: 'get',
  }).then((data) => {
    if (typeof data === 'string') return data
    return data?.base64 ?? data?.image ?? data?.qrCode ?? ''
  })
}

// ==================== 教室照片管理 ====================

/** GET /admin/classrooms/{id}/images（全部审核状态） */
export function listClassroomImages(id: number): Promise<AdminClassroomImage[]> {
  return request<AdminClassroomImage[]>({ url: `/admin/classrooms/${id}/images`, method: 'get' })
}

/** POST /admin/classrooms/{id}/images（multipart 上传，管理员直接发布无需审核） */
export function uploadClassroomImage(
  id: number,
  file: File,
  category: ClassroomImageCategory = 'OTHER',
  description?: string,
): Promise<AdminClassroomImage> {
  const form = new FormData()
  form.append('file', file)
  form.append('category', category)
  if (description) form.append('description', description)
  return request<AdminClassroomImage>({
    url: `/admin/classrooms/${id}/images`,
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** DELETE /admin/classrooms/images/{imageId}（逻辑删除） */
export function deleteClassroomImage(imageId: number): Promise<void> {
  return request<void>({ url: `/admin/classrooms/images/${imageId}`, method: 'delete' })
}

// ==================== 内容审核 ====================

export interface AuditQuery {
  targetType: AuditTargetType
  status?: AuditStatus | ''
  page?: number
  size?: number
}

/** GET /admin/audits?targetType=&status=&page= */
export function listAudits(query: AuditQuery): Promise<Page<AuditItem>> {
  return request<Page<AuditItem>>({ url: '/admin/audits', method: 'get', params: query })
}

export interface AuditHandlePayload {
  targetType: AuditTargetType
  targetId: number
  action: 'APPROVE' | 'REJECT' | 'OFFLINE' | 'DELETE'
  remark?: string
}

/** PUT /admin/audits/handle */
export function handleAudit(data: AuditHandlePayload): Promise<void> {
  return request<void>({ url: '/admin/audits/handle', method: 'put', data })
}

// ==================== 举报处理 ====================

/** GET /admin/reports?status= */
export function listReports(status?: ReportStatus | ''): Promise<Page<ReportItem>> {
  return request<Page<ReportItem>>({
    url: '/admin/reports',
    method: 'get',
    params: { status: status || undefined },
  })
}

/** PUT /admin/reports/{id}/handle */
export function handleReport(
  id: number,
  data: { status: 'HANDLED' | 'REJECTED'; handleResult?: string },
): Promise<void> {
  return request<void>({ url: `/admin/reports/${id}/handle`, method: 'put', data })
}

// ==================== 用户管理 ====================

export interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: UserStatus | ''
}

/** GET /admin/users?page=&keyword=&status= */
export function listUsers(query: UserQuery): Promise<Page<UserItem>> {
  return request<Page<UserItem>>({ url: '/admin/users', method: 'get', params: query })
}

/** GET /admin/users/{id}（详情 + 违规记录） */
export function getUserDetail(id: number): Promise<UserDetail> {
  return request<UserDetail>({ url: `/admin/users/${id}`, method: 'get' })
}

/** PUT /admin/users/{id}/status（封禁/解封） */
export function updateUserStatus(id: number, status: UserStatus): Promise<void> {
  return request<void>({ url: `/admin/users/${id}/status`, method: 'put', data: { status } })
}

// ==================== 统计 ====================

/** GET /admin/stats/overview */
export function getStatsOverview(): Promise<StatsOverview> {
  return request<StatsOverview>({ url: '/admin/stats/overview', method: 'get' })
}

/** GET /admin/stats/daily?days=30 */
export function getStatsDaily(days = 30): Promise<StatsDailyItem[]> {
  return request<StatsDailyItem[]>({
    url: '/admin/stats/daily',
    method: 'get',
    params: { days },
  })
}

/** GET /admin/stats/contributors（贡献排行） */
export function getContributors(): Promise<Contributor[]> {
  return request<Contributor[]>({ url: '/admin/stats/contributors', method: 'get' })
}

// ==================== 系统配置 ====================

/** GET /admin/configs */
export function listConfigs(): Promise<SysConfig[]> {
  return request<SysConfig[]>({ url: '/admin/configs', method: 'get' })
}

/** PUT /admin/configs */
export function updateConfigs(
  items: Array<{ configKey: string; configValue: string }>,
): Promise<void> {
  return request<void>({ url: '/admin/configs', method: 'put', data: { configs: items } })
}

// ==================== 角色常量 ====================

export const ROLES: AdminRole[] = ['SUPER_ADMIN', 'AUDITOR', 'MAINTAINER']

/** 供 401 等场景读取响应包使用 */
export type { ApiResult }
