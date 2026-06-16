<template>
  <div class="page-home">
    <!-- 统计卡片 — 含今日数据 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon docs"><svg width="22" height="22" viewBox="0 0 20 20"><rect x="3" y="2" width="14" height="16" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M6 7h8M6 10h8M6 13h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></div>
        <div class="stat-body"><div class="stat-num">{{ stats.documentCount }}</div><div class="stat-label">文档总数</div></div>
        <div class="stat-today" v-if="stats.todayDocCount">+{{ stats.todayDocCount }} 今日</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon convs"><svg width="22" height="22" viewBox="0 0 20 20"><path d="M3 15V4.333A1.333 1.333 0 0 1 4.333 3h11.334A1.333 1.333 0 0 1 17 4.333v8A1.333 1.333 0 0 1 15.667 13.667H6.667L3 17.667V15Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg></div>
        <div class="stat-body"><div class="stat-num">{{ stats.conversationCount }}</div><div class="stat-label">对话次数</div></div>
        <div class="stat-today" v-if="stats.todayConvCount">+{{ stats.todayConvCount }} 今日</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon msgs"><svg width="22" height="22" viewBox="0 0 20 20"><path d="M4 5h12M4 9h12M4 13h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="16" cy="14" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M17.5 15.5 19 17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></div>
        <div class="stat-body"><div class="stat-num">{{ stats.messageCount }}</div><div class="stat-label">问答消息</div></div>
        <div class="stat-today" v-if="stats.todayMsgCount">+{{ stats.todayMsgCount }} 今日</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fce8e6;color:#ea4335"><svg width="22" height="22" viewBox="0 0 20 20"><path d="M10 1v2M10 17v2M1 10h2M17 10h2M4.93 4.93l1.41 1.41M13.66 13.66l1.41 1.41M4.93 15.07l1.41-1.41M13.66 6.34l1.41-1.41" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="10" cy="10" r="4" stroke="currentColor" stroke-width="1.5"/></svg></div>
        <div class="stat-body"><div class="stat-num">{{ formatSize(stats.totalSize) }}</div><div class="stat-label">存储用量</div></div>
      </div>
    </div>

    <!-- Hero + 文档状态概览 -->
    <div class="db-row">
      <div class="db-card hero-card" :class="{ 'hero-empty': stats.documentCount === 0 }">
        <div class="hero-left">
          <h2>{{ stats.documentCount === 0 ? '欢迎使用 SmartDoc QA' : '开始使用' }}</h2>
          <p class="hero-desc">{{ stats.documentCount === 0 ? '上传您的第一份 PDF 或 TXT 文档，体验 AI 智能问答' : '上传 PDF 或 TXT 文档，AI 将基于文档内容为您答疑解惑' }}</p>
          <div class="hero-steps">
            <div class="hs"><span class="hs-num">1</span><span>上传文档</span><span class="hs-arr">→</span></div>
            <div class="hs"><span class="hs-num">2</span><span>智能处理</span><span class="hs-arr">→</span></div>
            <div class="hs"><span class="hs-num">3</span><span>自然提问</span></div>
          </div>
        </div>
        <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="handleUpload" accept=".pdf,.txt">
          <button class="btn-upload-hero">上传文档</button>
        </el-upload>
      </div>
      <div class="db-card" v-if="stats.documentCount > 0">
        <h3 class="db-card-title">文档状态</h3>
        <div class="doc-status-bar">
          <div class="status-bar-item"><div class="status-bar-track"><div class="status-bar-fill completed" :style="{ width: statusPercent.completed + '%' }"></div></div><span class="status-bar-label">已完成 {{ stats.completedCount || 0 }}</span></div>
          <div class="status-bar-item"><div class="status-bar-track"><div class="status-bar-fill processing" :style="{ width: statusPercent.processing + '%' }"></div></div><span class="status-bar-label">处理中 {{ stats.processingCount || 0 }}</span></div>
          <div class="status-bar-item"><div class="status-bar-track"><div class="status-bar-fill failed" :style="{ width: statusPercent.failed + '%' }"></div></div><span class="status-bar-label">失败 {{ stats.failedCount || 0 }}</span></div>
        </div>
      </div>
    </div>

    <!-- 活动时间线 + 最近文档 -->
    <div class="db-row" v-if="stats.documentCount > 0">
      <div class="db-card">
        <h3 class="db-card-title">最近活动</h3>
        <div class="activity-list" v-if="stats.activities?.length">
          <div v-for="(act, i) in stats.activities" :key="i" class="activity-item" @click="act.type === 'upload' ? selectDoc({ id: act.docId }) : selectDoc({ id: act.docId })">
            <div :class="['activity-dot', act.type]"></div>
            <div class="activity-body">
              <div class="activity-text">
                <template v-if="act.type === 'upload'">上传了 <strong>{{ act.docName }}</strong></template>
                <template v-else>在对话中提问</template>
              </div>
              <div class="activity-time">{{ formatTime(act.time) }}</div>
            </div>
          </div>
        </div>
        <div v-else class="activity-empty">暂无活动记录</div>
      </div>
      <div class="db-card">
        <h3 class="db-card-title">最近文档</h3>
        <div v-for="doc in documents.slice(0,5)" :key="doc.id" class="recent-doc" @click="selectDoc(doc)">
          <svg v-if="doc.fileName?.endsWith('.pdf')" width="16" height="16" viewBox="0 0 16 16"><rect x="2" y="1" width="12" height="14" rx="2" stroke="#dc2626" stroke-width="1.2"/><path d="M5 5h6M5 8h6M5 11h4" stroke="#dc2626" stroke-width="1.2" stroke-linecap="round"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 16 16"><rect x="2" y="1" width="12" height="14" rx="2" stroke="#6b7280" stroke-width="1.2"/><path d="M5 5h6M5 8h6M5 11h4" stroke="#6b7280" stroke-width="1.2" stroke-linecap="round"/></svg>
          <span class="recent-doc-name">{{ doc.fileName }}</span>
          <span class="recent-doc-status"><span :class="['status-dot', doc.status?.toLowerCase()]"></span>{{ statusText(doc.status) }}</span>
          <svg width="12" height="12" viewBox="0 0 12 12" class="recent-arrow"><path d="M4 2 8 6 4 10" stroke="#9ca3af" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
      </div>
    </div>

    <!-- 三列卡片 -->
    <div class="db-row db-row-3">
      <div class="db-card">
        <h3 class="db-card-title">核心能力</h3>
        <div class="cap-list">
          <div class="cap-item"><span class="cap-dot"></span><div><strong>RAG 检索增强生成</strong><p>向量化语义检索 + LLM 精准回答</p></div></div>
          <div class="cap-item"><span class="cap-dot"></span><div><strong>原文溯源引用</strong><p>回答均标注来源，可展开查看原文</p></div></div>
          <div class="cap-item"><span class="cap-dot"></span><div><strong>多轮记忆对话</strong><p>保持上下文连贯，支持连续问答</p></div></div>
          <div class="cap-item"><span class="cap-dot"></span><div><strong>多格式文档解析</strong><p>支持 PDF / TXT 文档上传分析</p></div></div>
        </div>
      </div>
      <div class="db-card">
        <h3 class="db-card-title">系统状态</h3>
        <div class="sys-status">
          <div class="sys-item"><span class="status-dot completed"></span>LLM 引擎<span class="sys-val">DeepSeek Chat</span></div>
          <div class="sys-item"><span class="status-dot completed"></span>向量数据库<span class="sys-val">Apache Lucene 9</span></div>
          <div class="sys-item"><span class="status-dot completed"></span>文档解析<span class="sys-val">Apache PDFBox</span></div>
          <div class="sys-item"><span class="status-dot completed"></span>数据存储<span class="sys-val">H2 / MySQL</span></div>
          <div class="sys-item"><span :class="['status-dot', stats.documentCount > 0 ? 'completed' : '']"></span>知识库<span class="sys-val">{{ stats.documentCount }} 个文档</span></div>
        </div>
      </div>
      <div class="db-card">
        <h3 class="db-card-title">快速操作</h3>
        <div class="quick-actions">
          <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="handleUpload" accept=".pdf,.txt">
            <div class="qa-card">
              <div class="qa-card-icon qa-upload"><svg width="20" height="20" viewBox="0 0 20 20"><path d="M10 3v10M5 8l5-5 5 5M3 15v1.5A1.5 1.5 0 0 0 4.5 18h11a1.5 1.5 0 0 0 1.5-1.5V15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg></div>
              <div class="qa-card-body"><div class="qa-card-title">上传新文档</div><div class="qa-card-desc">支持 PDF、TXT 格式</div></div>
              <svg width="14" height="14" viewBox="0 0 14 14" class="qa-arrow"><path d="M5 3 9 7 5 11" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            </div>
          </el-upload>
          <div class="qa-card" v-if="documents.length > 0" @click="documents[0] && selectDoc(documents[0])">
            <div class="qa-card-icon qa-recent"><svg width="20" height="20" viewBox="0 0 20 20"><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M10 6v4l3 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg></div>
            <div class="qa-card-body"><div class="qa-card-title">打开最近文档</div><div class="qa-card-desc">{{ documents[0]?.fileName }}</div></div>
            <svg width="14" height="14" viewBox="0 0 14 14" class="qa-arrow"><path d="M5 3 9 7 5 11" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </div>
          <div class="qa-card" @click="refresh">
            <div class="qa-card-icon qa-refresh"><svg width="20" height="20" viewBox="0 0 20 20"><path d="M3 10a7 7 0 0 1 12.5-3.5M17 10a7 7 0 0 1-12.5 3.5M17 3v4h-4M3 17v-4h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg></div>
            <div class="qa-card-body"><div class="qa-card-title">刷新数据</div><div class="qa-card-desc">更新统计和文档状态</div></div>
            <svg width="14" height="14" viewBox="0 0 14 14" class="qa-arrow"><path d="M5 3 9 7 5 11" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 对话热度 TOP3 -->
    <div class="db-row" v-if="stats.hotDocs && stats.hotDocs.length > 0">
      <div class="db-card">
        <h3 class="db-card-title">热门文档</h3>
        <div class="hot-list">
          <div v-for="(hd, i) in stats.hotDocs" :key="hd.docId" class="hot-item" @click="selectDoc({ id: hd.docId })">
            <span class="hot-rank">#{{ i+1 }}</span>
            <span class="hot-name">{{ hd.docName }}</span>
            <span class="hot-count">{{ hd.conversationCount }} 次对话</span>
          </div>
        </div>
      </div>
      <div></div>
    </div>

    <div class="tip-bar" v-if="stats.documentCount > 0">💡 上传文档后，等待状态变为「已完成」即可开始提问</div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const documents = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const stats = ref({ documentCount: 0, conversationCount: 0, messageCount: 0 })

onMounted(() => { loadDocuments(); loadStats() })

async function loadStats() { try { const r = await api.getStats(); stats.value = r.data } catch {} }
async function loadDocuments() { try { const r = await api.getDocuments(); documents.value = r.data } catch (e) { ElMessage.error('文档列表加载失败: ' + (e?.message || '未知错误')) } }

const statusPercent = computed(() => {
  const total = Math.max(1, (stats.value.completedCount || 0) + (stats.value.processingCount || 0) + (stats.value.failedCount || 0))
  return {
    completed: Math.round((stats.value.completedCount || 0) / total * 100),
    processing: Math.round((stats.value.processingCount || 0) / total * 100),
    failed: Math.round((stats.value.failedCount || 0) / total * 100)
  }
})

function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 KB'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t); const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return (diff / 60000 | 0) + '分钟前'
  if (diff < 86400000) return (diff / 3600000 | 0) + '小时前'
  return (diff / 86400000 | 0) + '天前'
}

function beforeUpload(f) { if (!['pdf','txt'].includes(f.name.split('.').pop().toLowerCase())) { ElMessage.error('仅支持 PDF / TXT'); return false }; return true }
async function handleUpload({ file }) {
  uploading.value = true
  const fd = new FormData(); fd.append('file', file)
  try {
    await api.uploadDocument(fd, e => { uploadProgress.value = Math.round((e.loaded / e.total) * 100) })
    ElMessage.success('上传成功，正在处理文档...')
    uploadProgress.value = 0
    await loadDocuments()
    await loadStats()
    let polls = 0
    const pollInterval = setInterval(async () => {
      polls++
      await loadDocuments(); await loadStats()
      const processingDocs = documents.value.filter(d => d.status === 'PROCESSING')
      if (processingDocs.length === 0 || polls >= 30) clearInterval(pollInterval)
    }, 2000)
  } catch (e) { ElMessage.error('上传失败: ' + (e?.message || '网络错误')) }
  finally { uploading.value = false }
}

function selectDoc(doc) { router.push(`/chat/${doc.id}`) }
function statusText(s) { const m = { COMPLETED:'已完成', PROCESSING:'处理中', FAILED:'处理失败' }; return m[s] || s }
function refresh() { loadDocuments(); loadStats() }
</script>

<style scoped>
.page-home { padding: 24px 32px; height: 100%; overflow-y: auto; }

/* 统计卡片 */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 18px 22px; background: #fff; border: 1px solid #e8eaed; border-radius: 12px; transition: all var(--transition); cursor: default; position: relative; }
.stat-card:hover { border-color: #c4c7cc; box-shadow: 0 2px 8px rgba(0,0,0,.04); transform: translateY(-1px); }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-icon.docs { background: #e8f0fe; color: #2563eb; }
.stat-icon.convs { background: #e6f4ea; color: #34a853; }
.stat-icon.msgs { background: #fef7e0; color: #f9ab00; }
.stat-body { min-width: 0; }
.stat-num { font-size: 26px; font-weight: 700; color: #1a1a2e; line-height: 1.1; }
.stat-label { font-size: 13px; color: #80868b; margin-top: 2px; }
.stat-today { position: absolute; top: 14px; right: 16px; font-size: 12px; font-weight: 500; color: #34a853; background: #e6f4ea; padding: 2px 8px; border-radius: 10px; }

/* Hero */
.db-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.db-row-3 { grid-template-columns: 1fr 1fr 1fr; }
.db-card { background: #fff; border: 1px solid #e8eaed; border-radius: 12px; padding: 24px; transition: box-shadow var(--transition); }
.db-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.04); }
.db-card-title { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 16px; }
.hero-card { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.hero-empty { padding: 32px 24px; }
.hero-empty .hero-left h2 { font-size: 22px; }
.hero-left h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin-bottom: 6px; }
.hero-desc { font-size: 14px; color: #5f6368; margin-bottom: 18px; }
.hero-steps { display: flex; align-items: center; gap: 8px; }
.hs { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #5f6368; }
.hs-num { width: 22px; height: 22px; border-radius: 50%; background: #e8f0fe; color: #2563eb; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.hs-arr { color: #bdc1c6; font-size: 15px; }
.btn-upload-hero { padding: 12px 28px; border: none; background: #2563eb; color: #fff; font-size: 15px; font-weight: 600; border-radius: 8px; cursor: pointer; white-space: nowrap; transition: all var(--transition); }
.btn-upload-hero:hover { background: #1d4ed8; box-shadow: 0 4px 14px rgba(37,99,235,.3); transform: translateY(-1px); }

/* 文档状态条 */
.doc-status-bar { display: flex; flex-direction: column; gap: 14px; }
.status-bar-item { display: flex; align-items: center; gap: 10px; }
.status-bar-track { flex: 1; height: 8px; background: #f1f3f4; border-radius: 4px; overflow: hidden; }
.status-bar-fill { height: 100%; border-radius: 4px; transition: width 0.6s; }
.status-bar-fill.completed { background: #34a853; }
.status-bar-fill.processing { background: #f9ab00; }
.status-bar-fill.failed { background: #ea4335; }
.status-bar-label { font-size: 13px; color: #80868b; white-space: nowrap; min-width: 80px; text-align: right; }

/* 活动时间线 */
.activity-list { display: flex; flex-direction: column; gap: 2px; }
.activity-item { display: flex; gap: 10px; padding: 8px 0; cursor: pointer; border-radius: 6px; transition: background 0.12s; }
.activity-item:hover { background: #f8f9fa; }
.activity-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.activity-dot.upload { background: #2563eb; }
.activity-dot.conversation { background: #34a853; }
.activity-body { flex: 1; min-width: 0; }
.activity-text { font-size: 14px; color: #5f6368; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.activity-text strong { color: #1a1a2e; }
.activity-time { font-size: 12px; color: #bdc1c6; margin-top: 2px; }
.activity-empty { font-size: 14px; color: #bdc1c6; text-align: center; padding: 20px 0; }

/* 最近文档 */
.recent-doc { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid #f1f3f4; cursor: pointer; transition: all var(--transition); }
.recent-doc:last-child { border-bottom: none; }
.recent-doc:hover { color: #2563eb; padding-left: 4px; }
.recent-doc:hover .recent-arrow { opacity: 1; }
.recent-doc-name { flex: 1; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.recent-doc-status { font-size: 12px; color: #80868b; display: flex; align-items: center; gap: 4px; }
.recent-arrow { opacity: 0; transition: opacity var(--transition); flex-shrink: 0; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.status-dot.completed { background: #34a853; }
.status-dot.processing { background: #f9ab00; }
.status-dot.failed { background: #ea4335; }

/* 热门文档 */
.hot-list { display: flex; flex-direction: column; gap: 8px; }
.hot-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: background 0.12s; }
.hot-item:hover { background: #f8f9fa; }
.hot-rank { font-size: 15px; font-weight: 700; color: #2563eb; width: 24px; }
.hot-name { flex: 1; font-size: 14px; color: #1a1a2e; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hot-count { font-size: 13px; color: #80868b; }

/* 核心能力 */
.cap-list { display: flex; flex-direction: column; gap: 12px; }
.cap-item { display: flex; gap: 10px; align-items: flex-start; padding: 2px 0; }
.cap-dot { width: 8px; height: 8px; border-radius: 50%; background: #2563eb; margin-top: 6px; flex-shrink: 0; transition: transform var(--transition); }
.cap-item:hover .cap-dot { transform: scale(1.4); }
.cap-item strong { font-size: 14px; color: #1a1a2e; }
.cap-item p { font-size: 13px; color: #80868b; margin: 2px 0 0; }

/* 系统状态 */
.sys-status { display: flex; flex-direction: column; gap: 10px; }
.sys-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #5f6368; }
.sys-val { margin-left: auto; font-size: 13px; color: #80868b; }

/* 快速操作 */
.quick-actions { display: flex; flex-direction: column; gap: 8px; }
.qa-card { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid #e8eaed; border-radius: 10px; cursor: pointer; transition: all var(--transition); }
.qa-card:hover { border-color: #c4c7cc; background: #fafbfc; box-shadow: 0 1px 4px rgba(0,0,0,.03); }
.qa-card:hover .qa-arrow { opacity: 1; }
.qa-card-icon { width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.qa-upload { background: #e8f0fe; color: #2563eb; }
.qa-recent { background: #e6f4ea; color: #34a853; }
.qa-refresh { background: #fef7e0; color: #f9ab00; }
.qa-card-body { flex: 1; min-width: 0; }
.qa-card-title { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.qa-card-desc { font-size: 12px; color: #80868b; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qa-arrow { opacity: 0; transition: opacity var(--transition); color: #bdc1c6; flex-shrink: 0; }

/* 提示条 */
.tip-bar { margin-top: 20px; padding: 12px 18px; background: #fef7e0; border: 1px solid #fde293; border-radius: 10px; font-size: 14px; color: #92600e; }
</style>
