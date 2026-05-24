import api from './index'

export const systemApi = {
  // 登录
  login: (data) => api.post('/v1/auth/login', data),
  logout: () => api.post('/v1/auth/logout'),
  refreshToken: (data) => api.post('/v1/auth/refresh', data),

  // 用户管理
  getUserList: (params) => api.get('/v1/system/users', { params }),
  getUserById: (id) => api.get(`/v1/system/users/${id}`),
  createUser: (data) => api.post('/v1/system/users', data),
  updateUser: (id, data) => api.put(`/v1/system/users/${id}`, data),
  deleteUser: (id) => api.delete(`/v1/system/users/${id}`),

  // 部门管理
  getDeptList: () => api.get('/v1/system/departments'),

  // 角色管理
  getRoleList: () => api.get('/v1/system/roles')
}