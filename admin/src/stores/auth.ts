import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/admin'
import { REAL_NAME_KEY, ROLE_KEY, TOKEN_KEY, clearAuthLocal } from '@/api/http'
import type { AdminRole } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const role = ref<AdminRole | ''>((localStorage.getItem(ROLE_KEY) as AdminRole) ?? '')
  const realName = ref<string>(localStorage.getItem(REAL_NAME_KEY) ?? '')

  /** 登录并持久化登录态 */
  async function login(username: string, password: string): Promise<void> {
    const res = await loginApi({ username, password })
    token.value = res.token
    role.value = res.role
    realName.value = res.realName
    localStorage.setItem(TOKEN_KEY, res.token)
    localStorage.setItem(ROLE_KEY, res.role)
    localStorage.setItem(REAL_NAME_KEY, res.realName)
  }

  /** 退出登录 */
  function logout(): void {
    token.value = ''
    role.value = ''
    realName.value = ''
    clearAuthLocal()
  }

  /** 是否拥有指定角色权限 */
  function hasRole(roles: AdminRole[]): boolean {
    return !!role.value && roles.includes(role.value)
  }

  return { token, role, realName, login, logout, hasRole }
})
