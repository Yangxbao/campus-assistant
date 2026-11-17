import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { AdminRole } from '@/types/api'

export interface MenuMeta {
  title: string
  icon?: string
  roles: AdminRole[]
}

/**
 * 各页面可见角色（契约：SUPER_ADMIN 全部；AUDITOR 审核/举报/用户/统计；MAINTAINER 楼宇教室/审核中的教室资料类/统计）
 */
export const ADMIN_ROLES: AdminRole[] = ['SUPER_ADMIN', 'AUDITOR', 'MAINTAINER']

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'DataLine', roles: ADMIN_ROLES } satisfies MenuMeta,
      },
      {
        path: 'buildings',
        name: 'Buildings',
        component: () => import('@/views/Buildings.vue'),
        meta: {
          title: '楼宇管理',
          icon: 'OfficeBuilding',
          roles: ['SUPER_ADMIN', 'MAINTAINER'],
        } satisfies MenuMeta,
      },
      {
        path: 'classrooms',
        name: 'Classrooms',
        component: () => import('@/views/Classrooms.vue'),
        meta: {
          title: '教室管理',
          icon: 'School',
          roles: ['SUPER_ADMIN', 'MAINTAINER'],
        } satisfies MenuMeta,
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/Audit.vue'),
        meta: { title: '内容审核', icon: 'DocumentChecked', roles: ADMIN_ROLES } satisfies MenuMeta,
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/Reports.vue'),
        meta: {
          title: '举报处理',
          icon: 'Warning',
          roles: ['SUPER_ADMIN', 'AUDITOR'],
        } satisfies MenuMeta,
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: {
          title: '用户管理',
          icon: 'User',
          roles: ['SUPER_ADMIN', 'AUDITOR'],
        } satisfies MenuMeta,
      },
      {
        path: 'configs',
        name: 'Configs',
        component: () => import('@/views/Configs.vue'),
        meta: {
          title: '系统配置',
          icon: 'Setting',
          roles: ['SUPER_ADMIN'],
        } satisfies MenuMeta,
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  // 未登录访问受保护页面 → 跳登录
  if (!to.meta.public && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录页 → 回首页
  if (to.path === '/login' && auth.token) {
    return { path: '/dashboard' }
  }
  // 角色无权访问 → 提示并回看板
  const meta = to.meta as unknown as Partial<MenuMeta>
  if (!to.meta.public && meta.roles && auth.role && !meta.roles.includes(auth.role)) {
    ElMessage.warning('当前角色无权访问该页面')
    return { path: '/dashboard' }
  }
  return true
})

export default router
