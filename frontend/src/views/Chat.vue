<template>
  <div class="chat-full" v-if="doc" @keydown="onKeyDown">
    <div class="chat-top">
      <button class="btn-back" @click="$router.push('/docs')">
        <svg width="16" height="16" viewBox="0 0 16 16"><path d="M10 3 5 8l5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
      <span v-if="renamingChat" class="chat-doc-name">
        <input class="rename-input" v-model="renameVal" @keydown.enter="finishChatRename" @keydown.escape="cancelChatRename" @blur="finishChatRename" ref="renameInput" />
      </span>
      <span v-else class="chat-doc-name" @dblclick="startChatRename" title="双击重命名">{{ doc.fileName }}</span>
      <span :class="['badge', doc.status?.toLowerCase()]">{{ statusText(doc.status) }}</span>
      <span style="flex:1"></span>
      <button v-if="currentConvId && messages.length" class="btn-ghost-sm" @click="clearMessages" title="清空对话">清空</button>
      <button v-if="currentConvId && messages.length" class="btn-ghost-sm" @click="exportChat" title="导出对话">导出</button>
      <button v-if="!currentConvId && doc.status === 'COMPLETED'" class="btn-primary-sm" @click="newConversation">开始对话</button>
    </div>

    <div class="chat-messages" ref="msgArea" @scroll="onScroll">
      <div v-if="!currentConvId && doc.status === 'COMPLETED'" class="chat-empty">
        <svg width="56" height="56" viewBox="0 0 56 56"><circle cx="28" cy="28" r="26" stroke="#e5e7eb" stroke-width="2"/><path d="M20 38V21.333A2.667 2.667 0 0 1 22.667 18.667h14.666A2.667 2.667 0 0 1 40 21.333v13.334A2.667 2.667 0 0 1 37.333 37.333H24l-4 4V38Z" stroke="#d1d5db" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        <p>点击「开始对话」发起新对话</p>
      </div>
      <div v-else-if="doc.status === 'PROCESSING'" class="chat-empty">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon><p>文档处理中，请稍候...</p>
      </div>

      <div v-for="msg in messages" :key="msg.id" :class="['msg-row', msg.role?.toLowerCase()]">
        <div class="msg-avatar">
          <span v-if="msg.role === 'USER'" class="avatar-text">{{ username?.charAt(0)?.toUpperCase() }}</span>
          <svg v-else width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" rx="6" fill="#2563eb"/><path d="M6 8h4v6H6zM8 8l2-3 2 3M12 10h3M12 12.5h3" stroke="#fff" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <div class="msg-body">
          <div class="msg-bubble">
            <div v-if="msg.role === 'ASSISTANT' && msg.content" class="msg-text markdown-body" v-html="renderMd(msg.content)" @click="onCitationClick"></div>
            <div v-else class="msg-text">{{ msg.content }}</div>
            <div v-if="msg.role === 'ASSISTANT' && !msg.content && asking" class="stream-cursor"></div>
            <button class="msg-copy" @click="copyText(msg.content)" title="复制">
              <svg width="14" height="14" viewBox="0 0 14 14"><rect x="4" y="4" width="8" height="8" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M2 10V2.5A.5.5 0 0 1 2.5 2H10" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            </button>
          </div>
          <div class="msg-meta">
            <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
            <button v-if="msg.role === 'ASSISTANT' && msg.id > 0 && !asking" class="msg-retry-btn" @click="regenerate(msg)" title="重新生成">
              <svg width="12" height="12" viewBox="0 0 12 12"><path d="M2 6a4 4 0 0 1 6.83-2.83M10 6A4 4 0 0 1 3.17 8.83M2 2v3h3M10 10V7H7" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
              重新生成
            </button>
          </div>
          <div v-if="msg.role === 'ASSISTANT' && msg.references" class="msg-refs">
            <el-collapse>
              <el-collapse-item>
                <template #title>
                  <div class="refs-title">
                    <svg width="14" height="14" viewBox="0 0 14 14"><path d="M7 1L2 4v6l5 3 5-3V4L7 1z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/><path d="M7 5v3M5 7h4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                    引用原文 ({{ parseRefs(msg.references).length }} 条)
                  </div>
                </template>
                <div v-for="(ref, i) in parseRefs(msg.references)" :key="i" :data-idx="i+1" class="ref-block">
                  <div class="ref-index"><span class="ref-badge">[{{ i+1 }}]</span>{{ ref.substring(0, 80) }}{{ ref.length > 80 ? '...' : '' }}</div>
                  <div class="ref-full">{{ ref }}</div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </div>

      <!-- 追问建议 -->
      <div v-if="suggestions.length && !asking && currentConvId" class="suggestions-row">
        <div class="suggestions-label">💡 继续追问</div>
        <div class="suggestions-list">
          <button v-for="(s, i) in suggestions" :key="i" class="suggestion-chip" @click="askSuggestion(s)">{{ s }}</button>
        </div>
      </div>
    </div>

    <button v-if="showScrollBtn" class="scroll-bottom-btn" @click="scrollToBottom">
      <svg width="20" height="20" viewBox="0 0 20 20"><path d="M10 4v10M6 10l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
    </button>

    <div class="chat-input-bar" v-if="currentConvId">
      <div class="input-wrap">
        <textarea v-model="question" placeholder="输入问题，Enter 发送 (Shift+Enter 换行)" rows="1" @keydown.enter.exact.prevent="askQuestion" @input="autoResize" ref="inputEl" :disabled="asking"></textarea>
        <el-select v-model="llmProvider" size="small" class="model-switch" :disabled="asking">
          <el-option label="DeepSeek" value="deepseek" />
          <el-option label="Kimi" value="kimi" />
        </el-select>
        <button v-if="!asking" class="send-btn" @click="askQuestion" :disabled="!question.trim()">
          <svg width="18" height="18" viewBox="0 0 18 18"><path d="M2 9h12M10 5l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
        <button v-else class="stop-btn" @click="stopGeneration" title="停止生成">
          <svg width="16" height="16" viewBox="0 0 16 16"><rect x="3" y="3" width="10" height="10" rx="2" fill="currentColor"/></svg>
        </button>
      </div>
    </div>

    <!-- 对话侧栏 -->
    <div class="chat-sidebar">
      <div class="chat-side-header">对话记录<button class="btn-icon" @click="newConversation"><svg width="14" height="14" viewBox="0 0 14 14"><path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></button></div>
      <div v-for="c in conversations" :key="c.id" :class="['chat-side-item', {active: currentConvId===c.id}]" @click="switchConv(c.id)">
        <div class="chat-side-item-text" :title="c.title">{{ c.title }}</div>
        <div class="chat-side-actions">
          <button class="side-btn" @click.stop="startRename(c)" title="重命名"><svg width="12" height="12" viewBox="0 0 12 12"><path d="M2 10h2l5-5L7 3 2 8v2ZM9 2l1 1" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg></button>
          <button class="side-btn danger" @click.stop="deleteConv(c)" title="删除"><svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 3l6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import api from '../api'

marked.setOptions({ breaks: true, gfm: true })

function renderMd(text) {
  if (!text) return ''
  try { return marked.parse(text) } catch { return text }
}

function injectCitations() {
  nextTick(() => {
    document.querySelectorAll('.markdown-body').forEach(el => {
      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, {
        acceptNode: node => {
          const parent = node.parentElement
          if (!parent) return NodeFilter.FILTER_ACCEPT
          if (parent.closest('pre, code, .citation')) return NodeFilter.FILTER_REJECT
          return /\[\d+\]/.test(node.textContent) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP
        }
      })
      const toReplace = []
      while (walker.nextNode()) { toReplace.push(walker.currentNode) }
      toReplace.forEach(textNode => {
        const frag = document.createDocumentFragment()
        const parts = textNode.textContent.split(/(\[\d+\])/)
        parts.forEach(part => {
          const m = part.match(/^\[(\d+)\]$/)
          if (m) {
            const span = document.createElement('span')
            span.className = 'citation'
            span.setAttribute('data-ref', m[1])
            span.textContent = part
            frag.appendChild(span)
          } else if (part) {
            frag.appendChild(document.createTextNode(part))
          }
        })
        textNode.parentNode.replaceChild(frag, textNode)
      })
    })
  })
}

const route = useRoute()
const doc = ref(null)
const username = ref(sessionStorage.getItem('username') || '用户')
const conversations = ref([])
const currentConvId = ref(null)
const messages = ref([])
const asking = ref(false)
const question = ref('')
const msgArea = ref(null)
const inputEl = ref(null)
const showScrollBtn = ref(false)
const suggestions = ref([])
let abortController = null
let lastQuestion = ''
const llmProvider = ref('deepseek')

watch(() => messages.value, () => { injectCitations(); loadSuggestions() }, { deep: true })

onMounted(async () => {
  try { const r = await api.getDocument(route.params.docId); doc.value = r.data } catch (e) { ElMessage.error('加载文档失败') }
  await loadConversations()
})

// === 对话管理 ===
async function loadConversations() {
  try {
    const r = await api.getConversations(route.params.docId)
    conversations.value = r.data
    const targetId = route.query.convId ? Number(route.query.convId) : null
    if (targetId && conversations.value.find(c => c.id === targetId)) {
      currentConvId.value = targetId
    } else if (conversations.value.length > 0) {
      currentConvId.value = conversations.value[0].id
    }
    if (currentConvId.value) await loadMsgs()
  } catch (e) {}
}

async function loadMsgs() {
  try { const r = await api.getMessages(currentConvId.value); messages.value = r.data; await nextTick(); scrollToBottom() } catch (e) { ElMessage.error('消息加载失败') }
}

async function newConversation() {
  try { const r = await api.createConversation({ documentId: doc.value.id }); conversations.value.unshift(r.data); currentConvId.value = r.data.id; messages.value = []; suggestions.value = [] } catch (e) { ElMessage.error('创建对话失败') }
}

async function switchConv(id) { stopGeneration(); currentConvId.value = id; suggestions.value = []; await loadMsgs() }

async function deleteConv(conv) {
  try { await ElMessageBox.confirm('删除该对话及所有消息？', '确认', { type:'warning', confirmButtonText:'删除', cancelButtonText:'取消' }) } catch { return }
  try {
    await api.deleteConversation(conv.id)
    conversations.value = conversations.value.filter(c => c.id !== conv.id)
    if (currentConvId.value === conv.id) {
      currentConvId.value = conversations.value.length > 0 ? conversations.value[0].id : null
      if (currentConvId.value) await loadMsgs()
      else messages.value = []
    }
  } catch (e) { ElMessage.error('删除失败') }
}

async function startRename(conv) {
  try {
    const { value } = await ElMessageBox.prompt('修改对话名称', '重命名', { confirmButtonText: '确定', cancelButtonText: '取消', inputValue: conv.title })
    if (value && value.trim()) { await api.renameConversation(conv.id, value.trim()); conv.title = value.trim(); ElMessage.success('已重命名') }
  } catch (e) {}
}

// 聊天页标题重命名
const renamingChat = ref(false)
const renameVal = ref('')
const renameInput = ref(null)
function startChatRename() {
  renamingChat.value = true
  renameVal.value = doc.value?.fileName || ''
  nextTick(() => renameInput.value?.focus())
}
async function finishChatRename() {
  if (!renamingChat.value) return
  renamingChat.value = false
  const v = renameVal.value.trim()
  if (v && v !== doc.value?.fileName) {
    try { await api.renameDocument(doc.value.id, v); doc.value.fileName = v; ElMessage.success('已重命名') } catch { ElMessage.error('重命名失败') }
  }
}
function cancelChatRename() { renamingChat.value = false }

// === 核心问答 ===
async function askQuestion() {
  const q = question.value.trim(); if (!q || !currentConvId.value || asking.value) return
  suggestions.value = []
  lastQuestion = q
  messages.value.push({ id: Date.now(), role: 'USER', content: q, createTime: new Date().toISOString() })
  question.value = ''; if (inputEl.value) inputEl.value.style.height = 'auto'; await nextTick(); scrollToBottom()
  asking.value = true

  const msgId = Date.now() + 1
  messages.value.push({ id: msgId, role: 'ASSISTANT', content: '', references: null, createTime: new Date().toISOString() })
  await nextTick(); scrollToBottom()
  abortController = new AbortController()

  try {
    const response = await fetch('/api/rag/ask/stream', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include',
      body: JSON.stringify({ question: q, documentId: doc.value.id, conversationId: currentConvId.value, provider: llmProvider.value }),
      signal: abortController.signal
    })
    if (!response.ok) { ElMessage.error('请求失败: ' + response.status); asking.value = false; await loadMsgs(); return }

    const reader = response.body.getReader(); const decoder = new TextDecoder()
    let buffer = '', currentEvent = ''

    while (true) {
      const { done, value } = await reader.read(); if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n'); buffer = lines.pop()
      for (const line of lines) {
        if (line.startsWith('event:')) { currentEvent = line.substring(6).trim() }
        else if (line.startsWith('data:')) {
          const data = line.substring(5).trim(); const msg = messages.value.find(m => m.id === msgId); if (!msg) continue
          switch (currentEvent) {
            case 'refs': msg.references = data; break
            case 'error': ElMessage.error(data); break
            case 'token': default: msg.content += data; break
          }
          currentEvent = ''
        }
      }
      await nextTick(); if (!showScrollBtn.value) scrollToBottom()
    }
  } catch (err) {
    if (err.name !== 'AbortError') ElMessage.error('请求失败: ' + (err.message || '未知错误'))
  } finally {
    asking.value = false; abortController = null
    setTimeout(() => loadMsgs(), 500)
  }
}

function stopGeneration() {
  if (abortController) { abortController.abort(); abortController = null }
}

// === 重新生成 ===
async function regenerate(aiMsg) {
  if (asking.value || !lastQuestion) return
  // 移除该 AI 消息
  messages.value = messages.value.filter(m => m.id !== aiMsg.id)
  asking.value = true
  suggestions.value = []

  const msgId = Date.now()
  messages.value.push({ id: msgId, role: 'ASSISTANT', content: '', references: null, createTime: new Date().toISOString() })
  await nextTick(); scrollToBottom()
  abortController = new AbortController()

  try {
    const response = await fetch('/api/rag/ask/stream', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include',
      body: JSON.stringify({ question: lastQuestion, documentId: doc.value.id, conversationId: currentConvId.value, provider: llmProvider.value }),
      signal: abortController.signal
    })
    if (!response.ok) { ElMessage.error('请求失败'); asking.value = false; await loadMsgs(); return }

    const reader = response.body.getReader(); const decoder = new TextDecoder()
    let buffer = '', currentEvent = ''
    while (true) {
      const { done, value } = await reader.read(); if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n'); buffer = lines.pop()
      for (const line of lines) {
        if (line.startsWith('event:')) { currentEvent = line.substring(6).trim() }
        else if (line.startsWith('data:')) {
          const data = line.substring(5).trim(); const msg = messages.value.find(m => m.id === msgId); if (!msg) continue
          switch (currentEvent) {
            case 'refs': msg.references = data; break
            case 'error': ElMessage.error(data); break
            case 'token': default: msg.content += data; break
          }
          currentEvent = ''
        }
      }
      await nextTick(); if (!showScrollBtn.value) scrollToBottom()
    }
  } catch (err) {
    if (err.name !== 'AbortError') ElMessage.error('重新生成失败')
  } finally {
    asking.value = false; abortController = null
    setTimeout(() => loadMsgs(), 500)
  }
}

// === 追问建议 ===
async function loadSuggestions() {
  if (!currentConvId.value || asking.value || messages.value.length < 2) { return }
  const lastMsg = messages.value[messages.value.length - 1]
  if (!lastMsg || lastMsg.role !== 'ASSISTANT' || !lastMsg.content || lastMsg.content.length < 50) { return }
  try {
    const r = await api.getSuggestions(currentConvId.value, llmProvider.value)
    suggestions.value = (r.data || []).slice(0, 3)
  } catch (e) { /* 非关键功能 */ }
}

function askSuggestion(q) {
  question.value = q
  askQuestion()
}

// === 清空对话 ===
async function clearMessages() {
  try { await ElMessageBox.confirm('清空当前对话的所有消息？', '确认', { type:'warning', confirmButtonText:'清空', cancelButtonText:'取消' }) } catch { return }
  try {
    await api.clearMessages(currentConvId.value)
    messages.value = []
    suggestions.value = []
    ElMessage.success('已清空')
  } catch (e) { ElMessage.error('清空失败') }
}

// === 导出对话 ===
function exportChat() {
  if (!messages.value.length) return
  let md = `# ${doc.value?.fileName || '对话记录'}\n\n`
  messages.value.forEach(msg => {
    const role = msg.role === 'USER' ? '**用户**' : '**AI 助手**'
    const time = msg.createTime ? new Date(msg.createTime).toLocaleString() : ''
    md += `### ${role}  \`${time}\`\n\n${msg.content}\n\n---\n\n`
  })
  const blob = new Blob([md], { type: 'text/markdown' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob)
  a.download = `chat_${doc.value?.fileName || 'export'}_${new Date().toLocaleDateString()}.md`
  a.click(); URL.revokeObjectURL(a.href)
  ElMessage.success('已导出')
}

// === 快捷键 ===
function onKeyDown(e) {
  if (e.target.tagName === 'TEXTAREA' || e.target.tagName === 'INPUT') return
  if (e.ctrlKey && e.key === 'k') { e.preventDefault(); newConversation() }
  if (e.ctrlKey && e.key === 'j' && conversations.value.length > 1) {
    e.preventDefault(); const idx = conversations.value.findIndex(c => c.id === currentConvId.value)
    const next = (idx + 1) % conversations.value.length; switchConv(conversations.value[next].id)
  }
}

// === 时间格式化 ===
function formatTime(t) {
  if (!t) return ''
  const d = new Date(t); const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return (diff / 60000 | 0) + '分钟前'
  if (d.toDateString() === now.toDateString()) return d.toLocaleTimeString('zh-CN', { hour:'2-digit', minute:'2-digit' })
  return d.toLocaleDateString('zh-CN', { month:'short', day:'numeric' }) + ' ' + d.toLocaleTimeString('zh-CN', { hour:'2-digit', minute:'2-digit' })
}

function onCitationClick(e) {
  const citation = e.target.closest('.citation'); if (!citation) return
  const refNum = citation.dataset.ref; if (!refNum) return
  const msgEl = e.target.closest('.msg-body'); if (!msgEl) return
  const collapse = msgEl.querySelector('.el-collapse-item')
  if (collapse && !collapse.classList.contains('is-active')) {
    const header = collapse.querySelector('.el-collapse-item__header'); if (header) header.click()
  }
  const msgRow = msgEl.closest('.msg-row')
  const refBlock = msgRow?.querySelector(`.ref-block[data-idx="${refNum}"]`)
  if (refBlock) { refBlock.scrollIntoView({ behavior:'smooth', block:'center' }); refBlock.classList.add('ref-highlight'); setTimeout(() => refBlock.classList.remove('ref-highlight'), 2000) }
}

function onScroll() {
  const el = msgArea.value; if (!el) return
  showScrollBtn.value = el.scrollHeight - el.scrollTop - el.clientHeight > 60
}

function scrollToBottom() {
  if (msgArea.value) { msgArea.value.scrollTop = msgArea.value.scrollHeight; showScrollBtn.value = false }
}

function copyText(text) {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制')).catch(() => ElMessage.error('复制失败'))
}

function autoResize() { const e = inputEl.value; if (e) { e.style.height = 'auto'; e.style.height = Math.min(e.scrollHeight, 120) + 'px' } }
function parseRefs(r) { if (!r) return []; try { return JSON.parse(r) } catch { return [r] } }
function statusText(s) { const m = { COMPLETED:'已完成', PROCESSING:'处理中', FAILED:'失败' }; return m[s] || s }
</script>

<style scoped>
.chat-full { display: flex; flex-direction: column; height: 100%; position: relative; }
.chat-top { display: flex; align-items: center; gap: 10px; padding: 10px 20px; background: #fff; border-bottom: 1px solid #e8eaed; flex-shrink: 0; }
.chat-doc-name { font-size: 14px; font-weight: 600; color: #1a1a2e; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.rename-input { width: 200px; font-size: 14px; font-weight: 600; padding: 2px 6px; border: 1px solid #2563eb; border-radius: 4px; outline: none; color: #1a1a2e; }
.badge { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.badge.completed { background: #e6f4ea; color: #137333; }
.badge.processing { background: #fef7e0; color: #92600e; }
.badge.failed { background: #fce8e6; color: #c5221f; }
.btn-back { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: none; background: none; cursor: pointer; border-radius: 6px; color: #5f6368; }
.btn-back:hover { background: #f1f3f4; }
.btn-primary-sm { padding: 6px 16px; border: none; background: #2563eb; color: #fff; font-size: 14px; border-radius: 6px; cursor: pointer; }
.btn-ghost-sm { padding: 4px 10px; border: 1px solid #e8eaed; background: #fff; font-size: 13px; color: #5f6368; border-radius: 6px; cursor: pointer; }
.btn-ghost-sm:hover { background: #f1f3f4; }
.btn-icon { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border: none; background: none; cursor: pointer; border-radius: 6px; color: #5f6368; }
.btn-icon:hover { background: #f1f3f4; }

.chat-messages { flex: 1; overflow-y: auto; padding: 20px 24px 20px 20px; margin-right: 260px; position: relative; }
.chat-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; height: 100%; color: #80868b; font-size: 15px; }

.msg-row { display: flex; gap: 10px; margin-bottom: 20px; animation: fadeInUp 0.3s; }
@keyframes fadeInUp { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:translateY(0)} }
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.avatar-text { width: 28px; height: 28px; border-radius: 8px; background: #e8eaed; color: #5f6368; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; }
.msg-body { max-width: 72%; position: relative; }
.msg-bubble { border-radius: 14px; padding: 12px 16px; font-size: 15px; line-height: 1.7; position: relative; }
.msg-row.user .msg-bubble { background: #2563eb; color: #fff; border-bottom-right-radius: 4px; padding-right: 28px; }
.msg-row.assistant .msg-bubble { background: #fff; border: 1px solid #e8eaed; border-bottom-left-radius: 4px; padding-right: 28px; }
.msg-text { white-space: pre-wrap; word-break: break-word; }
.msg-bubble:hover .msg-copy { opacity: 1; }
.msg-copy { position: absolute; bottom: 6px; right: 6px; width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; border: none; background: none; cursor: pointer; border-radius: 4px; opacity: 0; transition: opacity 0.12s; }
.msg-row.user .msg-copy { color: rgba(255,255,255,.6); }
.msg-row.assistant .msg-copy { color: #bdc1c6; }
.msg-copy:hover { background: rgba(0,0,0,.08); }

/* 消息元信息 + 重新生成 */
.msg-meta { display: flex; align-items: center; gap: 8px; margin-top: 4px; padding: 0 4px; }
.msg-time { font-size: 12px; color: #bdc1c6; }
.msg-retry-btn { display: flex; align-items: center; gap: 3px; border: none; background: none; font-size: 12px; color: #bdc1c6; cursor: pointer; padding: 2px 6px; border-radius: 4px; transition: all 0.12s; }
.msg-retry-btn:hover { background: #f1f3f4; color: #5f6368; }

/* 追问建议 */
.suggestions-row { margin: 0 0 20px; padding: 12px 16px; background: #f8f9fa; border: 1px solid #e8eaed; border-radius: 12px; }
.suggestions-label { font-size: 13px; color: #5f6368; margin-bottom: 8px; font-weight: 500; }
.suggestions-list { display: flex; flex-wrap: wrap; gap: 6px; }
.suggestion-chip { padding: 6px 14px; border: 1px solid #d2e3fc; background: #e8f0fe; color: #2563eb; font-size: 13px; border-radius: 16px; cursor: pointer; transition: all 0.15s; white-space: nowrap; }
.suggestion-chip:hover { background: #2563eb; color: #fff; border-color: #2563eb; }

/* Markdown */
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(strong) { font-weight: 700; }
.markdown-body :deep(em) { font-style: italic; }
.markdown-body :deep(code) { background: #f1f3f4; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; font-family: 'SF Mono', 'Fira Code', monospace; }
.markdown-body :deep(pre) { background: #1e1e1e; color: #e8eaed; padding: 12px 16px; border-radius: 8px; overflow-x: auto; margin: 8px 0; font-size: 14px; line-height: 1.5; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; font-size: inherit; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 6px 0; }
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(blockquote) { border-left: 3px solid #2563eb; padding: 4px 12px; margin: 8px 0; color: #5f6368; background: #f8f9fa; border-radius: 0 6px 6px 0; }
.markdown-body :deep(h1),.markdown-body :deep(h2),.markdown-body :deep(h3) { font-weight: 700; margin: 10px 0 6px; color: #1a1a2e; }
.markdown-body :deep(h1) { font-size: 1.2em; } .markdown-body :deep(h2) { font-size: 1.1em; } .markdown-body :deep(h3) { font-size: 1.05em; }
.markdown-body :deep(table) { border-collapse: collapse; margin: 8px 0; width: 100%; }
.markdown-body :deep(th),.markdown-body :deep(td) { border: 1px solid #e8eaed; padding: 6px 10px; text-align: left; font-size: 14px; }
.markdown-body :deep(th) { background: #f8f9fa; font-weight: 600; }
.markdown-body :deep(hr) { border: none; border-top: 1px solid #e8eaed; margin: 12px 0; }
.markdown-body :deep(.citation) { display: inline-flex; align-items: center; justify-content: center; background: #e8f0fe; color: #2563eb; font-size: 0.75em; font-weight: 600; padding: 1px 5px; border-radius: 3px; cursor: pointer; vertical-align: middle; transition: all 0.12s; user-select: none; margin: 0 1px; }
.markdown-body :deep(.citation:hover) { background: #2563eb; color: #fff; }

.stream-cursor { display: inline-block; width: 8px; height: 16px; background: #2563eb; border-radius: 2px; animation: blink 1s infinite; vertical-align: middle; margin-left: 2px; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.msg-refs { margin-top: 8px; }
.refs-title { font-size: 13px; color: #5f6368; display: flex; align-items: center; gap: 5px; }
.ref-block { padding: 10px 12px; background: #fafbfc; border: 1px solid #e8eaed; border-radius: 8px; margin-bottom: 6px; transition: all 0.3s; }
.ref-block:last-child { margin-bottom: 0; }
.ref-highlight { background: #fff9e6 !important; border-color: #f9ab00 !important; box-shadow: 0 0 0 3px rgba(249,171,0,.15); }
.ref-badge { display: inline-flex; align-items: center; justify-content: center; background: #e8f0fe; color: #2563eb; font-size: 12px; font-weight: 600; padding: 2px 6px; border-radius: 3px; margin-right: 6px; vertical-align: middle; }
.ref-index { font-size: 13px; color: #5f6368; line-height: 1.5; }
.ref-full { font-size: 13px; color: #5f6368; line-height: 1.6; margin-top: 6px; padding-top: 6px; border-top: 1px dashed #e8eaed; }

.scroll-bottom-btn { position: absolute; bottom: 100px; right: 290px; width: 36px; height: 36px; background: #fff; border: 1px solid #e8eaed; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,.1); color: #5f6368; z-index: 50; transition: all 0.15s; }
.scroll-bottom-btn:hover { border-color: #2563eb; color: #2563eb; box-shadow: 0 4px 14px rgba(37,99,235,.2); }

.chat-input-bar { padding: 14px 24px 14px 20px; background: #fff; border-top: 1px solid #e8eaed; margin-right: 260px; }
.input-wrap { display: flex; align-items: flex-end; gap: 10px; padding: 10px 14px; background: #f5f6f8; border: 1px solid #e8eaed; border-radius: 12px; }
.input-wrap:focus-within { border-color: #2563eb; }
.input-wrap textarea { flex: 1; border: none; background: none; outline: none; font-size: 15px; line-height: 1.5; resize: none; max-height: 120px; font-family: inherit; }
.send-btn { width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; border: none; background: #2563eb; color: #fff; border-radius: 10px; cursor: pointer; flex-shrink: 0; }
.send-btn:disabled { background: #e8eaed; color: #bdc1c6; cursor: not-allowed; }
.model-switch { width: 100px; flex-shrink: 0; }
.stop-btn { width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; border: none; background: #ea4335; color: #fff; border-radius: 10px; cursor: pointer; flex-shrink: 0; transition: all 0.15s; }
.stop-btn:hover { background: #c5221f; }

.chat-sidebar { position: absolute; right: 0; top: 48px; bottom: 0; width: 260px; background: #fff; border-left: 1px solid #e8eaed; overflow-y: auto; display: flex; flex-direction: column; }
.chat-side-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px 10px; font-size: 12px; font-weight: 700; text-transform: uppercase; color: #80868b; letter-spacing: 0.5px; }
.chat-side-item { display: flex; align-items: center; padding: 10px 16px; cursor: pointer; border-left: 2px solid transparent; transition: all 0.1s; gap: 4px; }
.chat-side-item:hover { background: #f8f9fa; }
.chat-side-item.active { border-left-color: #2563eb; background: #e8f0fe; }
.chat-side-item-text { flex: 1; font-size: 14px; color: #5f6368; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chat-side-item.active .chat-side-item-text { color: #2563eb; font-weight: 500; }
.chat-side-actions { display: flex; gap: 2px; opacity: 0; transition: opacity 0.1s; }
.chat-side-item:hover .chat-side-actions { opacity: 1; }
.side-btn { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; border: none; background: none; cursor: pointer; border-radius: 4px; color: #bdc1c6; }
.side-btn:hover { background: #f1f3f4; color: #5f6368; }
.side-btn.danger:hover { background: #fce8e6; color: #ea4335; }
</style>
