import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000,
  withCredentials: true
})

api.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    ElMessage.error('网络错误，请稍后重试')
    return Promise.reject(error)
  }
)

export default {
  // 用户
  login(data) { return api.post('/user/login', data) },
  register(data) { return api.post('/user/register', data) },
  logout() { return api.post('/user/logout') },
  getCurrentUser() { return api.get('/user/current') },
  getUserInfo() { return api.get('/user/info') },
  changePassword(oldPassword, newPassword) { return api.put('/user/change-password', { oldPassword, newPassword }) },

  // 文档
  uploadDocument(formData, onProgress) {
    return api.post('/document/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  },
  getDocuments() { return api.get('/document/list') },
  deleteDocument(id) { return api.delete(`/document/${id}`) },
  reprocessDocument(id) { return api.post(`/document/${id}/reprocess`) },
  renameDocument(id, fileName) { return api.put(`/document/${id}/rename`, { fileName }) },
  getDocument(id) { return api.get(`/document/${id}`) },

  // 对话
  createConversation(data) { return api.post('/conversation/create', data) },
  getConversations(documentId) {
    return api.get('/conversation/list', { params: { documentId } })
  },
  getMessages(conversationId) {
    return api.get(`/conversation/${conversationId}/messages`)
  },
  deleteConversation(id) { return api.delete(`/conversation/${id}`) },
  renameConversation(id, title) { return api.put(`/conversation/${id}/rename`, { title }) },

  // 问答
  ask(data) { return api.post('/rag/ask', data) },
  // 流式问答 — 返回 fetch Response 供 ReadableStream 读取
  askStream(data) {
    return fetch('/api/rag/ask/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(data)
    })
  },

  // 统计
  getStats() { return api.get('/stats') },

  // 追问建议
  getSuggestions(convId, provider) { return api.get(`/rag/suggestions/${convId}`, { params: { provider } }) },

  // 清空对话消息
  clearMessages(convId) { return api.delete(`/rag/conversation/${convId}/messages`) },

  // 批量删除对话
  batchDeleteConversations(ids) { return api.post('/conversation/batch-delete', { ids }) },

  // 设置检查
  checkSettings() { return api.get('/settings/check') }
}
