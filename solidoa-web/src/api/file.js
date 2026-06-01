import request from '@/utils/request'

/**
 * 上传文件
 * @param {File} file - 文件对象
 * @returns {Promise<string>} - 返回文件URL
 */
export const upload = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/v1/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 批量上传文件
 * @param {File[]} files - 文件数组
 * @returns {Promise<string[]>} - 返回文件URL数组
 */
export const uploadMultiple = (files) => {
  if (!files || files.length === 0) return Promise.resolve([])
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  return request.post('/v1/file/upload/batch', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除文件
 * @param {string} url - 文件URL
 */
export const remove = (url) => {
  return request.delete('/v1/file/delete', { params: { url } })
}