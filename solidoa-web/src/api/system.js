import request from '@/utils/request'

export const systemApi = {
  // 登录
  login: (data) => request.post('/v1/auth/login', data),
  logout: () => request.post('/v1/auth/logout'),
  refreshToken: (data) => request.post('/v1/auth/refresh', data),

  // 用户管理
  getUserList: (params) => request.get('/v1/system/users', { params }),
  getUserById: (id) => request.get(`/v1/system/users/${id}`),
  getCurrentUser: () => request.get('/v1/system/users/current'),
  createUser: (data) => request.post('/v1/system/users', data),
  updateUser: (id, data) => request.put(`/v1/system/users/${id}`, data),
  deleteUser: (id) => request.delete(`/v1/system/users/${id}`),
  resetPassword: (id) => request.put(`/v1/system/users/${id}/password`),
  changePassword: (data) => request.put('/v1/system/users/current/password', data),

  // 部门管理
  getDeptTree: () => request.get('/v1/system/depts/tree'),
  getDeptById: (id) => request.get(`/v1/system/depts/${id}`),
  createDept: (data) => request.post('/v1/system/depts', data),
  updateDept: (id, data) => request.put(`/v1/system/depts/${id}`, data),
  deleteDept: (id) => request.delete(`/v1/system/depts/${id}`),

  // 角色管理
  getRoleList: (params) => request.get('/v1/system/roles', { params }),
  getRoleById: (id) => request.get(`/v1/system/roles/${id}`),
  createRole: (data) => request.post('/v1/system/roles', data),
  updateRole: (id, data) => request.put(`/v1/system/roles/${id}`, data),
  deleteRole: (id) => request.delete(`/v1/system/roles/${id}`),
  getRolePermissions: (id) => request.get(`/v1/system/roles/${id}/permissions`),
  assignPermissions: (id, data) => request.post(`/v1/system/roles/${id}/permissions`, data),

  // 通讯录
  getContactList: () => request.get('/v1/system/contacts'),
  getContactById: (id) => request.get(`/v1/system/contacts/${id}`),
  getContactsByDept: (deptId) => request.get(`/v1/system/contacts/dept/${deptId}`),
  searchContacts: (keyword) => request.get('/v1/system/contacts/search', { params: { keyword } }),

  // 消息管理
  markMessageRead: (messageId) => request.put(`/v1/system/messages/${messageId}/read`),
  getUnreadCount: () => request.get('/v1/system/messages/unread/count'),

  // 日程管理
  getScheduleList: () => request.get('/v1/system/schedules'),
  getScheduleById: (id) => request.get(`/v1/system/schedules/${id}`),
  getScheduleAll: () => request.get('/v1/system/schedules/all'),
  createSchedule: (data) => request.post('/v1/system/schedules', data),
  updateSchedule: (id, data) => request.put(`/v1/system/schedules/${id}`, data),
  deleteSchedule: (id) => request.delete(`/v1/system/schedules/${id}`),

  // 消息回复
  replyMessage: (messageId, data) => request.post(`/v1/system/messages/${messageId}/reply`, data),

  // ========== 首页仪表盘 ==========
  getDashboardStats: () => request.get('/v1/dashboard/stats'),
  getDashboardMessages: () => request.get('/v1/dashboard/messages'),
  getFooterStats: () => request.get('/v1/dashboard/footer-stats'),
}