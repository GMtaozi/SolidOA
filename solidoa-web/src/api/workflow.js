import request from '@/utils/request'

export const workflowApi = {
  // ========== 请假 ==========
  getLeaveList: (params) => request.get('/v1/workflow/leave', { params }),
  getLeaveById: (id) => request.get(`/v1/workflow/leave/${id}`),
  createLeave: (data) => request.post('/v1/workflow/leave', data),
  approveLeave: (id, data) => request.post(`/v1/workflow/leave/${id}/approve`, data),
  cancelLeave: (id) => request.put(`/v1/workflow/leave/${id}/cancel`),

  // ========== 用印申请 ==========
  getStampList: (params) => request.get('/v1/workflow/stamp', { params }),
  getStampById: (id) => request.get(`/v1/workflow/stamp/${id}`),
  createStamp: (data) => request.post('/v1/workflow/stamp', data),
  approveStamp: (id, data) => request.post(`/v1/workflow/stamp/${id}/approve`, data),
  cancelStamp: (id) => request.put(`/v1/workflow/stamp/${id}/cancel`),
  recordStamp: (id, data) => request.post(`/v1/workflow/stamp/${id}/record`, data),
  getStampStatistics: (params) => request.get('/v1/workflow/stamp/statistics', { params }),
  getStampTypes: () => request.get('/v1/workflow/stamp/types'),

  // ========== 报销（已迁移至 hrApi）==========

  // ========== 采购申请 ==========
  getPurchaseList: (params) => request.get('/v1/workflow/purchase', { params }),
  getPurchaseById: (id) => request.get(`/v1/workflow/purchase/${id}`),
  createPurchase: (data) => request.post('/v1/workflow/purchase', data),
  approvePurchase: (id, data) => request.post(`/v1/workflow/purchase/${id}/approve`, data),
  cancelPurchase: (id) => request.put(`/v1/workflow/purchase/${id}/cancel`),
  updateProgress: (id, data) => request.put(`/v1/workflow/purchase/${id}/progress`, data),
  getPurchaseStatistics: (params) => request.get('/v1/workflow/purchase/statistics', { params }),
  getPurchaseTypes: () => request.get('/v1/workflow/purchase/types'),

  // ========== 审批任务 ==========
  getMyTasks: () => request.get('/v1/workflow/tasks/my'),
  getMyProcessed: () => request.get('/v1/workflow/tasks/processed'),
  getPendingCount: () => request.get('/v1/workflow/tasks/pending/count'),

  // ========== 审批流程 ==========
  getLeaveFlow: (id) => request.get(`/v1/workflow/leave/${id}/flow`),
  getStampFlow: (id) => request.get(`/v1/workflow/stamp/${id}/flow`),
  getPurchaseFlow: (id) => request.get(`/v1/workflow/purchase/${id}/flow`),
  // HR模块的流程（报销、加班等通过 hr-api 调用）

  // ========== 转交审批 ==========
  transfer: (id, data) => request.post(`/v1/workflow/tasks/${id}/transfer`, data),

  // ========== 审批流程配置 ==========
  getFlowConfigList: (businessType) => request.get('/v1/workflow/flow/config', { params: { businessType } }),
  createFlowConfig: (data) => request.post('/v1/workflow/flow/config', data),
  updateFlowConfig: (id, data) => request.put(`/v1/workflow/flow/config/${id}`, data),
  deleteFlowConfig: (id) => request.delete(`/v1/workflow/flow/config/${id}`),
  setDefaultFlow: (id, businessType) => request.put(`/v1/workflow/flow/config/${id}/set-default`, null, { params: { businessType } }),

  // ========== 抄送 ==========
  getMyCcList: (params) => request.get('/v1/workflow/cc/my', { params }),
  getMyCcUnreadCount: () => request.get('/v1/workflow/cc/my/unread-count'),
  markCcAsRead: (id) => request.put(`/v1/workflow/cc/${id}/read`),

  // ========== 加班/出差/补卡（已迁移至 hrApi）==========

  // ========== 审批记录 ==========
  getApprovalRecordList: (params) => request.get('/v1/workflow/record/all', { params }),
  getApprovalRecordById: (id) => request.get(`/v1/workflow/record/${id}`),
  getMyApplyList: (params) => request.get('/v1/workflow/record/my-apply', { params }),
  getMyApprovedList: (params) => request.get('/v1/workflow/record/my-approved', { params }),
  getApprovalRecordFlow: (id) => request.get(`/v1/workflow/record/${id}/flow`),
  exportApprovalRecord: (params) => request.get('/v1/workflow/record/export', { params, responseType: 'blob' }),

  // ========== 用印记录 ==========
  getStampRecords: (params) => request.get('/v1/workflow/stamp/records', { params }),

  // ========== 审批流统一入口（V2.0 State Machine 8 端点，适配器模式保留旧 API） ==========
  // 流程图（核心：含 nodes + edges）
  getFlowGraph: (type, id) => request.get(`/v1/workflow/approval/${type}/${id}/flow-graph`),
  // 撤回
  withdraw: (type, id, data) => request.post(`/v1/workflow/approval/${type}/${id}/withdraw`, data),
  // 转交
  transferApproval: (type, id, data) => request.post(`/v1/workflow/approval/${type}/${id}/transfer`, data),
  // 加签
  addSignApproval: (type, id, data) => request.post(`/v1/workflow/approval/${type}/${id}/add-sign`, data),
  // 同意/拒绝（统一入口，V2.0 推荐使用）
  universalApprove: (type, id, data) => request.post(`/v1/workflow/approval/${type}/${id}/approve`, data),
  universalReject: (type, id, data) => request.post(`/v1/workflow/approval/${type}/${id}/reject`, data),
  // 状态机调试信息
  getStateMachineInfo: () => request.get('/v1/workflow/approval/state-machine/info')
}