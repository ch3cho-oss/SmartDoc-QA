<template>
  <div class="page-docs">
    <div class="page-header">
      <h2>文档管理</h2>
      <div class="header-right">
        <div class="status-tabs">
          <button :class="['tab-btn', { active: statusFilter === 'ALL' }]" @click="statusFilter = 'ALL'">全部</button>
          <button :class="['tab-btn', { active: statusFilter === 'COMPLETED' }]" @click="statusFilter = 'COMPLETED'">已完成</button>
          <button :class="['tab-btn', { active: statusFilter === 'PROCESSING' }]" @click="statusFilter = 'PROCESSING'">处理中</button>
          <button :class="['tab-btn', { active: statusFilter === 'FAILED' }]" @click="statusFilter = 'FAILED'">失败</button>
        </div>
        <el-input v-model="search" placeholder="搜索文档..." class="doc-search" clearable />
      </div>
    </div>

    <!-- 上传进度 -->
    <div v-if="uploading" class="upload-progress">
      <div class="progress-track"><div class="progress-fill" :style="{width: (uploadDone / uploadTotal * 100)+'%'}"></div></div>
      <span class="progress-text">{{ uploadDone }}/{{ uploadTotal }} 完成</span>
    </div>

    <div v-if="filtered.length===0 && !uploading" class="empty-page">
      <svg width="64" height="64" viewBox="0 0 64 64"><rect x="12" y="8" width="40" height="48" rx="6" stroke="#d1d5db" stroke-width="2"/><path d="M24 24h16M24 32h16M24 40h10" stroke="#d1d5db" stroke-width="2" stroke-linecap="round"/></svg>
      <p>{{ documents.length === 0 ? '还没有上传任何文档' : '没有匹配的文档' }}</p>
      <el-upload v-if="documents.length === 0" :show-file-list="false" :before-upload="beforeUpload" :http-request="handleUpload" accept=".pdf,.txt" multiple>
        <button class="btn-upload-empty">上传第一份文档</button>
      </el-upload>
    </div>

    <div v-else class="doc-table">
      <div class="doc-table-row header"><span class="col-name">文件名</span><span class="col-status">状态</span><span class="col-size">大小</span><span class="col-date">上传时间</span><span class="col-actions">操作</span></div>
      <div v-for="doc in filtered" :key="doc.id" class="doc-table-row">
        <span class="col-name">
          <svg v-if="doc.fileName?.endsWith('.pdf')" width="16" height="16"><rect x="2" y="1" width="12" height="14" rx="2" stroke="#dc2626" stroke-width="1.2"/><path d="M5 5h6M5 8h6M5 11h4" stroke="#dc2626" stroke-width="1.2" stroke-linecap="round"/></svg>
          <svg v-else width="16" height="16"><rect x="2" y="1" width="12" height="14" rx="2" stroke="#6b7280" stroke-width="1.2"/><path d="M5 5h6M5 8h6M5 11h4" stroke="#6b7280" stroke-width="1.2" stroke-linecap="round"/></svg>
          {{ doc.fileName }}
        </span>
        <span class="col-status"><span :class="['status-badge',doc.status?.toLowerCase()]">{{ statusText(doc.status) }}</span></span>
        <span class="col-size">{{ formatFileSize(doc.fileSize) }}</span>
        <span class="col-date">{{ doc.uploadTime?.substring(0,10) }}</span>
        <span class="col-actions">
          <button v-if="doc.status==='FAILED'" class="btn-table retry" @click.stop="reprocess(doc)">重试</button>
          <button class="btn-table" @click="$router.push(`/chat/${doc.id}`)" :disabled="doc.status==='PROCESSING'">对话</button>
          <button class="btn-table danger" @click="deleteDoc(doc)">删除</button>
        </span>
      </div>
    </div>

    <div style="margin-top:16px">
      <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="handleUpload" accept=".pdf,.txt" multiple>
        <button class="btn-upload-sm">+ 上传新文档（可多选）</button>
      </el-upload>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const documents = ref([])
const search = ref('')
const statusFilter = ref('ALL')
const uploading = ref(false)
const uploadDone = ref(0)
const uploadTotal = ref(0)

onMounted(() => loadDocs())
async function loadDocs() { try { const r = await api.getDocuments(); documents.value = r.data } catch (e) { ElMessage.error('加载失败') } }

const filtered = computed(() => {
  let list = documents.value
  if (statusFilter.value !== 'ALL') list = list.filter(d => d.status === statusFilter.value)
  if (search.value) { const q = search.value.toLowerCase(); list = list.filter(d => d.fileName?.toLowerCase().includes(q)) }
  return list
})

function statusText(s) { const m = { COMPLETED:'已完成', PROCESSING:'处理中', FAILED:'处理失败' }; return m[s] || s }
function formatFileSize(b) { if (!b) return '-'; if (b < 1024) return b+' B'; if (b < 1048576) return (b/1024).toFixed(1)+' KB'; return (b/1048576).toFixed(1)+' MB' }
function beforeUpload(f) { if (!['pdf','txt'].includes(f.name.split('.').pop().toLowerCase())) { ElMessage.error('仅支持 PDF / TXT'); return false }; return true }
async function handleUpload(options) {
  const files = options.file instanceof FileList ? Array.from(options.file) : [options.file]
  uploading.value = true; uploadDone.value = 0; uploadTotal.value = files.length

  const uploadOne = async (file) => {
    const fd = new FormData(); fd.append('file', file)
    await api.uploadDocument(fd)
    uploadDone.value++
  }

  const results = await Promise.allSettled(files.map(uploadOne))
  const ok = results.filter(r => r.status === 'fulfilled').length
  const fail = results.filter(r => r.status === 'rejected').length

  if (fail === 0) ElMessage.success(`全部 ${ok} 个文件上传成功，正在处理...`)
  else if (ok > 0) ElMessage.warning(`${ok} 个成功，${fail} 个失败`)
  else ElMessage.error('全部上传失败')

  uploading.value = false
  await loadDocs()
  let polls = 0
  const interval = setInterval(async () => { polls++; await loadDocs(); if (!documents.value.some(d => d.status === 'PROCESSING') || polls >= 20) clearInterval(interval) }, 3000)
}
async function deleteDoc(doc) {
  try { await ElMessageBox.confirm(`删除「${doc.fileName}」？`, '确认删除', { type:'warning', confirmButtonText:'删除', cancelButtonText:'取消' }) } catch { return }
  try { await api.deleteDocument(doc.id); ElMessage.success('已删除'); loadDocs() } catch {}
}
async function reprocess(doc) {
  try { await api.reprocessDocument(doc.id); ElMessage.success('已开始重新处理...'); doc.status = 'PROCESSING'; loadDocs() } catch { ElMessage.error('重试失败') }
}
</script>

<style scoped>
.page-docs { padding: 24px 32px; height: 100%; overflow-y: auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; }
.header-right { display: flex; align-items: center; gap: 16px; }
.status-tabs { display: flex; gap: 2px; background: #f1f3f4; border-radius: 8px; padding: 2px; }
.tab-btn { padding: 5px 14px; border: none; background: none; font-size: 13px; color: #5f6368; border-radius: 6px; cursor: pointer; transition: all 0.12s; }
.tab-btn:hover { color: #1a1a2e; }
.tab-btn.active { background: #fff; color: #2563eb; font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,.06); }
.doc-search { width: 220px; }

.upload-progress { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 10px 16px; background: #e8f0fe; border-radius: 8px; }
.progress-track { flex: 1; height: 6px; background: #c4d7f5; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #2563eb; border-radius: 3px; transition: width 0.3s; }
.progress-text { font-size: 13px; font-weight: 600; color: #2563eb; }

.empty-page { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 80px 0; color: #bdc1c6; font-size: 15px; }
.btn-upload-empty { padding: 10px 24px; border: none; background: #2563eb; color: #fff; font-size: 15px; font-weight: 500; border-radius: 8px; cursor: pointer; }

.doc-table { background: #fff; border: 1px solid #e8eaed; border-radius: 12px; overflow: hidden; }
.doc-table-row { display: grid; grid-template-columns: 2fr 100px 80px 110px 120px; align-items: center; padding: 12px 20px; border-bottom: 1px solid #f1f3f4; font-size: 14px; }
.doc-table-row:last-child { border-bottom: none; }
.doc-table-row.header { font-size: 12px; font-weight: 700; text-transform: uppercase; color: #80868b; background: #fafbfc; letter-spacing: 0.5px; }
.doc-table-row:not(.header):hover { background: #f8f9fa; box-shadow: inset 3px 0 0 #2563eb; }
.col-name { display: flex; align-items: center; gap: 8px; color: #1a1a2e; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.col-status, .col-size, .col-date { color: #5f6368; }
.status-badge { font-size: 12px; padding: 2px 8px; border-radius: 10px; font-weight: 500; }
.status-badge.completed { background: #e6f4ea; color: #137333; }
.status-badge.processing { background: #fef7e0; color: #92600e; }
.status-badge.failed { background: #fce8e6; color: #c5221f; }
.btn-table { padding: 5px 12px; border: 1px solid #e8eaed; background: #fff; border-radius: 6px; font-size: 13px; color: #5f6368; cursor: pointer; margin-left: 6px; transition: all var(--transition); }
.btn-table:hover { border-color: #2563eb; color: #2563eb; background: #f8f9fa; }
.btn-table:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-table.danger:hover { border-color: #ea4335; color: #ea4335; }
.btn-table.retry { border-color: #d97706; color: #d97706; }
.btn-table.retry:hover { border-color: #d97706; color: #fff; background: #d97706; }
.btn-upload-sm { padding: 8px 20px; border: 1px dashed #c4c7cc; background: #fff; border-radius: 8px; font-size: 14px; color: #5f6368; cursor: pointer; }
.btn-upload-sm:hover { border-color: #2563eb; color: #2563eb; }
</style>
