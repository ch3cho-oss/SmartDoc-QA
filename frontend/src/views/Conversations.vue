<template>
  <div class="page-convs">
    <div class="page-header">
      <h2>对话历史</h2>
      <div class="header-right">
        <button v-if="selectedIds.length" class="btn-batch-delete" @click="batchDelete">删除选中 ({{ selectedIds.length }})</button>
        <el-input v-model="search" placeholder="搜索对话..." class="doc-search" clearable />
      </div>
    </div>

    <div v-if="filtered.length===0" class="empty-page">
      <svg width="64" height="64" viewBox="0 0 64 64"><circle cx="32" cy="32" r="24" stroke="#d1d5db" stroke-width="2"/><path d="M24 32a4 4 0 0 1 8 0M28 28l8 8M28.5 24v3M35.5 37v3" stroke="#d1d5db" stroke-width="1.5" stroke-linecap="round"/></svg>
      <p>暂无对话记录</p>
    </div>

    <div v-else class="conv-cards">
      <div v-for="conv in filtered" :key="conv.id" class="conv-card" @click="openChat(conv)">
        <div class="conv-card-top">
          <label class="conv-check" @click.stop><input type="checkbox" :checked="selectedIds.includes(conv.id)" @change="toggleSelect(conv.id)" /></label>
          <div v-if="renamingId === conv.id" class="conv-card-title" @click.stop>
            <input class="rename-input" v-model="renameVal" @keydown.enter="finishRename(conv)" @keydown.escape="cancelRename" @blur="finishRename(conv)" ref="renameInput" />
          </div>
          <div v-else class="conv-card-title" @dblclick.stop="startRename(conv)" :title="'双击重命名'">{{ conv.title }}</div>
          <span class="conv-card-date">{{ formatDate(conv.createTime) }}</span>
        </div>
        <div class="conv-card-doc" v-if="conv.documentId">
          <svg width="12" height="12" viewBox="0 0 12 12"><rect x="2" y="1" width="8" height="10" rx="1.5" stroke="currentColor" stroke-width="1"/></svg>
          {{ getDocName(conv.documentId) || '未知文档' }}
        </div>
        <div class="conv-card-meta">
          <svg width="14" height="14" viewBox="0 0 14 14"><path d="M2 10V2.5A.5.5 0 0 1 2.5 2H10" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><rect x="4" y="4" width="8" height="8" rx="1.5" stroke="currentColor" stroke-width="1.2"/></svg>
          点击继续对话
          <svg width="10" height="10" viewBox="0 0 10 10"><path d="M3 1 7 5 3 9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const conversations = ref([])
const documents = ref([])
const search = ref('')
const selectedIds = ref([])
const renamingId = ref(null)
const renameVal = ref('')
const renameInput = ref(null)

onMounted(async () => {
  await loadConvs()
  await loadDocs()
})

async function loadConvs() { try { const r = await api.getConversations(); conversations.value = r.data } catch {} }
async function loadDocs() { try { const r = await api.getDocuments(); documents.value = r.data } catch {} }

function getDocName(docId) {
  return documents.value.find(d => d.id === docId)?.fileName || null
}

const filtered = computed(() => {
  if (!search.value) return conversations.value
  const q = search.value.toLowerCase()
  return conversations.value.filter(c => c.title?.toLowerCase().includes(q) || getDocName(c.documentId)?.toLowerCase().includes(q))
})

function openChat(conv) {
  router.push({ path: `/chat/${conv.documentId}`, query: { convId: conv.id } })
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

async function batchDelete() {
  try { await ElMessageBox.confirm(`删除选中的 ${selectedIds.value.length} 个对话？`, '确认批量删除', { type:'warning', confirmButtonText:'删除', cancelButtonText:'取消' }) } catch { return }
  try {
    await api.batchDeleteConversations(selectedIds.value)
    ElMessage.success('已删除')
    selectedIds.value = []
    await loadConvs()
  } catch (e) { ElMessage.error('删除失败') }
}

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t); const now = new Date()
  const diff = now - d
  if (diff < 86400000 && d.toDateString() === now.toDateString()) return '今天 ' + d.toLocaleTimeString('zh-CN', { hour:'2-digit', minute:'2-digit' })
  if (diff < 172800000 && new Date(now - 86400000).toDateString() === d.toDateString()) return '昨天'
  return d.toLocaleDateString('zh-CN', { month:'short', day:'numeric' })
}
async function startRename(conv) {
  renamingId.value = conv.id
  renameVal.value = conv.title
  await nextTick()
  renameInput.value?.focus?.()
}
async function finishRename(conv) {
  if (!renamingId.value) return
  const v = renameVal.value.trim()
  if (v && v !== conv.title) {
    try { await api.renameConversation(conv.id, v); conv.title = v; ElMessage.success('已重命名') } catch (e) { ElMessage.error('重命名失败') }
  }
  renamingId.value = null
}
function cancelRename() { renamingId.value = null }
</script>

<style scoped>
.page-convs { padding: 24px 32px; height: 100%; overflow-y: auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; }
.header-right { display: flex; align-items: center; gap: 12px; }
.btn-batch-delete { padding: 6px 14px; border: 1px solid #ea4335; background: #fff; color: #ea4335; font-size: 13px; border-radius: 6px; cursor: pointer; transition: all 0.12s; }
.btn-batch-delete:hover { background: #fce8e6; }
.doc-search { width: 240px; }

.empty-page { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 80px 0; color: #bdc1c6; font-size: 15px; }

.conv-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.conv-card { padding: 16px 20px; background: #fff; border: 1px solid #e8eaed; border-radius: 10px; cursor: pointer; transition: all 0.15s; position: relative; }
.conv-card:hover { border-color: #2563eb; }
.conv-card-top { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.conv-check { display: flex; align-items: center; flex-shrink: 0; }
.conv-check input { width: 15px; height: 15px; accent-color: #2563eb; cursor: pointer; }
.conv-card-title { font-size: 15px; font-weight: 600; color: #1a1a2e; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rename-input { width: 100%; font-size: 15px; font-weight: 600; padding: 2px 6px; border: 1px solid #2563eb; border-radius: 4px; outline: none; color: #1a1a2e; background: #fff; }
.conv-card-date { font-size: 13px; color: #bdc1c6; white-space: nowrap; }
.conv-card-doc { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #80868b; margin-bottom: 8px; }
.conv-card-meta { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #bdc1c6; }
</style>
