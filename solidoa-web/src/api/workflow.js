import api from './index'

export const workflowApi = {
  // 请假
  getLeaveList: (params) => api.get('/v1/workflow/leave', { params }),
  getLeaveById: (id) => api.get(`/v1/workflow/leave/${id}`),
  createLeave: (data) => api.post('/v1/workflow/leave', data),
  approveLeave: (id, data) => api.post(`/v1/workflow/leave/${id}/approve`, data),
  cancelLeave: (id) => api.put(`/v1/workflow/leave/${id}/cancel`),

  // 我的待办
  getMyTasks: () => api.get('/v1/workflow/tasks/my'),

  // 已处理
  getMyProcessed: () => api.get('/v1/workflow/tasks/processed')
}