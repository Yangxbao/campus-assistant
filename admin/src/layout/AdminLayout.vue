<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><School /></el-icon>
        <span>校园助手管理后台</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.68)"
        active-text-color="#ffffff"
        class="menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-tag :type="roleTagType" size="small" effect="plain">{{ roleLabel }}</el-tag>
          <span class="real-name">{{ auth.realName || '管理员' }}</span>
          <el-button link type="danger" @click="onLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { AdminRole } from '@/types/api'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

interface MenuItem {
  path: string
  title: string
  icon: string
  roles: AdminRole[]
}

const allMenus: MenuItem[] = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine', roles: ['SUPER_ADMIN', 'AUDITOR', 'MAINTAINER'] },
  { path: '/buildings', title: '楼宇管理', icon: 'OfficeBuilding', roles: ['SUPER_ADMIN', 'MAINTAINER'] },
  { path: '/classrooms', title: '教室管理', icon: 'School', roles: ['SUPER_ADMIN', 'MAINTAINER'] },
  { path: '/audit', title: '内容审核', icon: 'DocumentChecked', roles: ['SUPER_ADMIN', 'AUDITOR', 'MAINTAINER'] },
  { path: '/reports', title: '举报处理', icon: 'Warning', roles: ['SUPER_ADMIN', 'AUDITOR'] },
  { path: '/users', title: '用户管理', icon: 'User', roles: ['SUPER_ADMIN', 'AUDITOR'] },
  { path: '/configs', title: '系统配置', icon: 'Setting', roles: ['SUPER_ADMIN'] },
]

const menus = computed(() =>
  allMenus.filter((m) => auth.role && m.roles.includes(auth.role)),
)

const currentTitle = computed(() => (route.meta as { title?: string }).title ?? '')

const roleLabel = computed(() => {
  const labels: Record<string, string> = {
    SUPER_ADMIN: '超级管理员',
    AUDITOR: '审核员',
    MAINTAINER: '维护员',
  }
  return labels[auth.role] ?? '未知角色'
})

const roleTagType = computed(() => {
  if (auth.role === 'SUPER_ADMIN') return 'danger'
  if (auth.role === 'AUDITOR') return 'primary'
  return 'success'
})

async function onLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
    auth.logout()
    router.replace('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.menu {
  border-right: none;
  flex: 1;
}

.header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.real-name {
  color: #606266;
  font-size: 14px;
}

.main {
  background: #f0f2f5;
  padding: 16px;
  overflow: auto;
}
</style>
