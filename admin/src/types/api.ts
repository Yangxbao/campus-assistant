/** 全局统一响应包 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** 统一分页结构 */
export interface Page<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export type AdminRole = 'SUPER_ADMIN' | 'AUDITOR' | 'MAINTAINER'

export interface LoginResult {
  token: string
  role: AdminRole
  realName: string
}

/** 楼层 */
export interface Floor {
  id?: number
  buildingId?: number
  floorNo: number
  description?: string
}

export interface Building {
  id: number
  code: string
  name: string
  floorsCount: number
  description?: string
  campus?: string
  locationDesc?: string
  status?: string
  floors?: Floor[]
  classroomCount?: number
}

export type ClassroomType = 'LINEAR' | 'SMALL' | 'LAB' | 'MULTIMEDIA' | 'OTHER'

export interface Classroom {
  id?: number
  buildingId: number
  floorId?: number
  roomNo: string
  name?: string
  type: ClassroomType
  seatCount?: number | null
  hasAc?: boolean
  socketDesc?: string
  socketCount?: number | null
  hasProjector?: boolean
  hasPc?: boolean
  hasMic?: boolean
  hasWhiteboard?: boolean
  hasWifi?: boolean
  otherDevices?: string
  envScoreQuiet?: number | null
  envDesc?: string
  deskDesc?: string
  lightDesc?: string
  ventilationDesc?: string
  cleanDesc?: string
  status?: string
  buildingName?: string
  floorNo?: number
  updatedContentAt?: string
}

export interface ImportResultRow {
  row: number
  success: boolean
  message?: string
  roomNo?: string
  buildingCode?: string
}

export interface ImportResult {
  total: number
  successCount: number
  failCount: number
  details?: ImportResultRow[]
}

/** 审核对象类型 */
export type AuditTargetType =
  | 'CLASSROOM_IMAGE'
  | 'EQUIPMENT_REPORT'
  | 'LOST_ITEM'
  | 'ROOM_STATUS'
  | 'REPORT'

export type AuditStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'OFFLINE'

/** 教室照片分类（与后端 t_classroom_image.category 一致） */
export type ClassroomImageCategory =
  | 'OVERALL'
  | 'FRONT'
  | 'BACK'
  | 'SOCKET'
  | 'AC'
  | 'PROJECTOR'
  | 'PC'
  | 'WHITEBOARD'
  | 'OTHER'

/** 管理端教室照片（全部审核状态） */
export interface AdminClassroomImage {
  id: number
  ossKey: string
  url: string
  category: ClassroomImageCategory | string
  description?: string
  auditStatus: AuditStatus
  createdAt?: string
}


/** 审核条目（后端字段宽松匹配，仅取展示所需） */
export interface AuditItem {
  id: number
  targetType: AuditTargetType
  targetId: number
  auditStatus?: AuditStatus
  status?: AuditStatus
  submitterName?: string
  submitterId?: number
  ossKey?: string
  url?: string
  description?: string
  remark?: string
  content?: string
  title?: string
  reasonDesc?: string
  fieldName?: string
  newValue?: string
  auditRemark?: string
  auditTime?: string
  createdAt?: string
  [key: string]: unknown
}

export interface AuditRecord {
  id: number
  targetType: string
  targetId: number
  auditorName?: string
  auditorId?: number
  action: 'APPROVE' | 'REJECT' | 'OFFLINE' | 'DELETE'
  remark?: string
  createdAt: string
}

export type ReportStatus = 'PENDING' | 'HANDLED' | 'REJECTED'

export interface ReportItem {
  id: number
  reporterId?: number
  reporterName?: string
  targetType: string
  targetId: number
  reasonType: string
  reasonDesc?: string
  status: ReportStatus
  handlerName?: string
  handleResult?: string
  createdAt?: string
}

export type UserStatus = 'ACTIVE' | 'BANNED'

export interface UserItem {
  id: number
  openid?: string
  nickname?: string
  avatarUrl?: string
  phone?: string
  role?: string
  status: UserStatus
  contributeCount?: number
  lastLoginTime?: string
  createdAt?: string
}

export interface UserDetail extends UserItem {
  violations?: Array<Record<string, unknown>>
  reports?: Array<Record<string, unknown>>
  contributions?: Array<Record<string, unknown>>
  [key: string]: unknown
}

export interface SysConfig {
  id?: number
  configKey: string
  configValue: string
  remark?: string
  updatedAt?: string
}

export interface StatsOverview {
  userCount?: number
  buildingCount?: number
  classroomCount?: number
  photoCount?: number
  lostItemCount?: number
  recoveredCount?: number
}

export interface StatsDailyItem {
  date: string
  classroomCount?: number
  lostItemCount?: number
  auditCount?: number
}

export interface Contributor {
  userId?: number
  id?: number
  nickname?: string
  contributeCount?: number
}
