/**
 * System API — 系统管理 + 协作
 * 迁移到 TS：以最小侵入方式（仅加 .ts 后缀 + import 改用 @/api/types）
 */
import request from '@/utils/request'
import type { PageQuery, User, Department, Role } from './types'

export const systemApi = {
  // 登录
  login: (data: { username: string; password: string }) =>
    request.post('/v1/auth/login', data),
  logout: () => request.post('/v1/auth/logout'),
  refreshToken: (data: { token: string }) => request.post('/v1/auth/refresh', data),

  // 用户管理
  getUserList: (params?: PageQuery) => request.get('/v1/system/users', { params }),
  getUserById: (id: number) => request.get(`/v1/system/users/${id}`),
  getCurrentUser: () => request.get('/v1/system/users/current'),
  createUser: (data: Partial<User>) => request.post('/v1/system/users', data),
  updateUser: (id: number, data: Partial<User>) => request.put(`/v1/system/users/${id}`, data),
  deleteUser: (id: number) => request.delete(`/v1/system/users/${id}`),
  resetPassword: (id: number) => request.put(`/v1/system/users/${id}/password`),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.put('/v1/system/users/current/password', data),

  // 部门管理
  getDeptTree: () => request.get('/v1/system/depts/tree'),
  getDeptById: (id: number) => request.get(`/v1/system/depts/${id}`),
  createDept: (data: Partial<Department>) => request.post('/v1/system/depts', data),
  updateDept: (id: number, data: Partial<Department>) => request.put(`/v1/system/depts/${id}`, data),
  deleteDept: (id: number) => request.delete(`/v1/system/depts/${id}`),

  // 角色管理
  getRoleList: (params?: PageQuery) => request.get('/v1/system/roles', { params }),
  getRoleById: (id: number) => request.get(`/v1/system/roles/${id}`),
  createRole: (data: Partial<Role>) => request.post('/v1/system/roles', data),
  updateRole: (id: number, data: Partial<Role>) => request.put(`/v1/system/roles/${id}`, data),
  deleteRole: (id: number) => request.delete(`/v1/system/roles/${id}`),
  getRolePermissions: (id: number) => request.get(`/v1/system/roles/${id}/permissions`),
  assignPermissions: (id: number, data: { permissionIds: number[] }) =>
    request.post(`/v1/system/roles/${id}/permissions`, data),

  // 通讯录
  getContactList: () => request.get('/v1/system/contacts'),
  getContactById: (id: number) => request.get(`/v1/system/contacts/${id}`),
  getContactsByDept: (deptId: number) => request.get(`/v1/system/contacts/dept/${deptId}`),
  searchContacts: (keyword: string) =>
    request.get('/v1/system/contacts/search', { params: { keyword } }),

  // 消息管理
  markMessageRead: (messageId: number) => request.put(`/v1/system/messages/${messageId}/read`),
  getUnreadCount: () => request.get('/v1/system/messages/unread/count'),

  // 日程管理
  getScheduleList: () => request.get('/v1/system/schedules'),
  getScheduleById: (id: number) => request.get(`/v1/system/schedules/${id}`),
  getScheduleAll: () => request.get('/v1/system/schedules/all'),
  createSchedule: (data: Record<string, unknown>) => request.post('/v1/system/schedules', data),
  updateSchedule: (id: number, data: Record<string, unknown>) =>
    request.put(`/v1/system/schedules/${id}`, data),
  deleteSchedule: (id: number) => request.delete(`/v1/system/schedules/${id}`),

  // 消息回复
  replyMessage: (messageId: number, data: Record<string, unknown>) =>
    request.post(`/v1/system/messages/${messageId}/reply`, data),

  // ========== 首页仪表盘 ==========
  getDashboardStats: () => request.get('/v1/dashboard/stats'),
  getDashboardMessages: () => request.get('/v1/dashboard/messages'),
  getFooterStats: () => request.get('/v1/dashboard/footer-stats'),
}
