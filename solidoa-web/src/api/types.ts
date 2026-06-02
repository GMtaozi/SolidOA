/**
 * SolidOA 通用 API 类型定义
 * 所有 API 模块共享这些类型
 */

/** 统一 Result 响应 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页查询参数 */
export interface PageQuery {
  page?: number
  size?: number
  pageNum?: number
  pageSize?: number
  keyword?: string
  [key: string]: unknown
}

/** 分页响应 */
export interface PageVO<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 用户 */
export interface User {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  deptId?: number
  deptName?: string
  status: 0 | 1
  createTime?: string
}

/** 部门 */
export interface Department {
  id: number
  name: string
  parentId: number
  sort: number
  deleted: 0 | 1
  children?: Department[]
}

/** 角色 */
export interface Role {
  id: number
  name: string
  code: string
  description?: string
  status: 0 | 1
}

/** 通用 ID */
export type Id = number

/** 通用 VO 基类 */
export interface BaseVO {
  id: Id
  createTime?: string
  updateTime?: string
}
