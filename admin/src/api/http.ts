import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

export const TOKEN_KEY = 'campus_admin_token'
export const ROLE_KEY = 'campus_admin_role'
export const REAL_NAME_KEY = 'campus_admin_real_name'

/** 清除本地登录态（401 或退出登录时调用） */
export function clearAuthLocal(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(REAL_NAME_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response): any => {
    // 二进制下载（导出 xlsx 等）直接返回原始数据
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data as ApiResult
    // code=0 成功；非 0 失败
    if (res.code !== 0) {
      // 未登录或 token 失效，跳回登录页
      if (res.code === 401) {
        clearAuthLocal()
        if (!location.pathname.startsWith('/login')) {
          location.href = '/login'
        }
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearAuthLocal()
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
      ElMessage.error('登录已失效，请重新登录')
    } else {
      ElMessage.error(error?.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

/** 解包后的请求方法：成功时直接返回 data */
export async function request<T>(config: Parameters<typeof http.request>[0]): Promise<T> {
  // 响应拦截器已解包 AxiosResponse，这里运行时拿到的是 ApiResult
  const res = (await http.request(config)) as unknown as ApiResult<T>
  return res.data
}

export default http
