import request from './request'

/**
 * 上传文件到文件服务
 * @param {File} file - 文件对象
 * @param {string} [businessType] - 业务类型
 * @param {string} [businessId] - 业务ID
 * @returns {Promise<string>} - 返回文件URL
 */
export const uploadFile = async (file, businessType, businessId) => {
  const formData = new FormData()
  formData.append('file', file)
  if (businessType) formData.append('businessType', businessType)
  if (businessId) formData.append('businessId', businessId)
  const res = await request.post('/v1/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return res.data || ''
}

/**
 * 批量上传文件
 * @param {File[]} files - 文件数组
 * @returns {Promise<string[]>} - 返回文件URL数组
 */
export const uploadFiles = async (files) => {
  if (!files || files.length === 0) return []
  const uploadPromises = files.map(file => uploadFile(file))
  return Promise.all(uploadPromises)
}

/**
 * 删除文件
 * @param {string} id - 文件ID
 */
export const deleteFile = async (id) => {
  if (!id) return
  try {
    await request.delete('/v1/file/' + id)
  } catch (error) {
    console.error('删除文件失败', error)
  }
}