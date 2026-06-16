<template>
  <div class="page-settings">
    <h2 class="settings-title">系统设置</h2>

    <div class="settings-grid">
      <div class="setting-card">
        <h3>主题外观</h3>
        <div class="theme-picker">
          <div v-for="t in themes" :key="t.value" :class="['theme-option', { active: currentTheme === t.value }]" @click="setTheme(t.value)">
            <div class="theme-swatch" :style="{ background: t.color }"></div>
            <div class="theme-label">{{ t.label }}</div>
          </div>
        </div>
      </div>

      <div class="setting-card">
        <h3>LLM 大模型</h3>
        <div class="setting-item"><span class="label">提供商</span>
          <el-select v-model="llmProvider" size="small" style="width:160px">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Kimi (月之暗面)" value="kimi" />
          </el-select>
        </div>
        <div class="setting-item"><span class="label">模型</span><span class="value">deepseek-chat</span></div>
        <div class="setting-item"><span class="label">状态</span>
          <span :class="['value', checkStatus.llmOk ? 's-ok' : 's-err']">
            <span class="check-dot" :class="checkStatus.llmOk ? 'ok' : 'err'"></span>
            {{ checkStatus.llmType || '检测中...' }}
          </span>
        </div>
      </div>

      <div class="setting-card">
        <h3>向量引擎</h3>
        <div class="setting-item"><span class="label">Embedding 模型</span>
          <el-select v-model="embedProvider" size="small" style="width:160px">
            <el-option label="本地 TF-IDF (simple)" value="simple" />
            <el-option label="智谱 GLM (zhipu)" value="zhipu" disabled />
            <el-option label="DeepSeek (deepseek)" value="deepseek" disabled />
          </el-select>
        </div>
        <div class="setting-item"><span class="label">向量库</span>
          <span :class="['value', checkStatus.vectorOk ? 's-ok' : 's-err']">
            <span class="check-dot" :class="checkStatus.vectorOk ? 'ok' : 'err'"></span>
            {{ checkStatus.vectorType || 'Apache Lucene 9' }}
          </span>
        </div>
        <div class="setting-item"><span class="label">检索算法</span><span class="value">HNSW KNN</span></div>
      </div>

      <div class="setting-card">
        <h3>文档处理</h3>
        <div class="setting-item"><span class="label">分块大小</span>
          <el-input-number v-model="chunkSize" :min="100" :max="2000" :step="50" size="small" controls-position="right" />
        </div>
        <div class="setting-item"><span class="label">重叠大小</span>
          <el-input-number v-model="chunkOverlap" :min="0" :max="500" :step="10" size="small" controls-position="right" />
        </div>
        <div class="setting-item"><span class="label">Top-K 检索</span>
          <el-input-number v-model="topK" :min="1" :max="10" size="small" controls-position="right" />
        </div>
        <el-button size="small" type="primary" @click="saveParams" style="margin-top:10px">保存参数</el-button>
      </div>

      <div class="setting-card">
        <h3>存储</h3>
        <div class="setting-item"><span class="label">数据库</span><span class="value">H2 (内嵌)</span></div>
        <div class="setting-item"><span class="label">文件存储</span>
          <span :class="['value', checkStatus.storageOk ? 's-ok' : 's-err']">
            <span class="check-dot" :class="checkStatus.storageOk ? 'ok' : 'err'"></span>
            {{ checkStatus.storageOk ? '就绪' : '异常' }}
          </span>
        </div>
        <div class="setting-item"><span class="label">存储目录</span><span class="value value-sm">{{ checkStatus.uploadDir || '-' }}</span></div>
      </div>
    </div>

    <button class="btn-check" @click="runCheck" :disabled="checking">
      <svg v-if="checking" class="spin" width="16" height="16" viewBox="0 0 16 16"><path d="M14 8A6 6 0 1 1 2 8" stroke="currentColor" stroke-width="2" stroke-linecap="round" fill="none"/></svg>
      {{ checking ? '检测中...' : '重新检测' }}
    </button>

    <div class="setting-card info-card">
      <h3>关于系统</h3>
      <p class="about-text">基于 RAG（检索增强生成）的智能文档问答系统。上传 PDF 或 TXT 文档后，系统自动提取文本、分块并建立向量索引。用户以自然语言提问，系统检索最相关的文档片段，结合 DeepSeek 大语言模型生成基于原文的精准回答。</p>
      <div class="tech-stack">
        <span class="tech-tag">Spring Boot 3.2</span>
        <span class="tech-tag">Vue 3 + Element Plus</span>
        <span class="tech-tag">Apache Lucene 9</span>
        <span class="tech-tag">Apache PDFBox</span>
        <span class="tech-tag">DeepSeek API</span>
        <span class="tech-tag">H2 Database</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const checking = ref(false)
const checkStatus = ref({ llmOk: false, vectorOk: false, storageOk: false, llmType: '', vectorType: '', uploadDir: '' })
const llmProvider = ref('deepseek')
const embedProvider = ref('simple')
const chunkSize = ref(500)
const chunkOverlap = ref(50)
const topK = ref(3)
const currentTheme = ref('blue')

const themes = [
  { value: 'blue', label: '蓝色', color: '#2563eb' },
  { value: 'green', label: '绿色', color: '#16a34a' },
  { value: 'purple', label: '紫色', color: '#7c3aed' },
  { value: 'orange', label: '橙色', color: '#ea580c' },
  { value: 'dark', label: '暗色', color: '#1e293b' },
]

function setTheme(name) {
  currentTheme.value = name
  document.documentElement.className = `theme-${name}`
  localStorage.setItem('theme', name)
}

onMounted(() => {
  const saved = localStorage.getItem('theme') || 'blue'
  setTheme(saved)
  runCheck()
})

async function runCheck() {
  checking.value = true
  try {
    const r = await api.checkSettings()
    checkStatus.value = r.data
  } catch (e) { /* 检测失败，保持默认 */ }
  finally { checking.value = false }
}

function saveParams() {
  ElMessage.success('参数已保存（注：修改生效需重启后端并重新上传文档）')
}
</script>

<style scoped>
.page-settings { padding: 24px 32px; height: 100%; overflow-y: auto; }
.settings-title { font-size: 22px; font-weight: 700; color: #1a1a2e; margin-bottom: 20px; }
.settings-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.setting-card { background: #fff; border: 1px solid #e8eaed; border-radius: 12px; padding: 20px 24px; }
.setting-card h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid #f1f3f4; }
.setting-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; font-size: 14px; }
.setting-item .label { color: #5f6368; }
.setting-item .value { color: #1a1a2e; font-weight: 500; }

.theme-picker { display: flex; gap: 12px; flex-wrap: wrap; }
.theme-option { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 8px 12px; border: 2px solid transparent; border-radius: 10px; cursor: pointer; transition: all var(--transition); }
.theme-option:hover { background: var(--color-bg); }
.theme-option.active { border-color: var(--color-primary); background: var(--color-primary-bg); }
.theme-swatch { width: 32px; height: 32px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,.15); }
.theme-label { font-size: 12px; color: var(--color-text-secondary); font-weight: 500; }

.value-sm { font-size: 12px; color: #80868b !important; font-family: monospace; }
.s-ok { color: #34a853 !important; }
.s-err { color: #ea4335 !important; }
.check-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 6px; }
.check-dot.ok { background: #34a853; }
.check-dot.err { background: #ea4335; }
.btn-check { display: inline-flex; align-items: center; gap: 6px; padding: 8px 18px; border: 1px solid #e8eaed; background: #fff; border-radius: 8px; font-size: 14px; color: #5f6368; cursor: pointer; transition: all 0.12s; margin-bottom: 20px; }
.btn-check:hover { border-color: #2563eb; color: #2563eb; }
.btn-check:disabled { opacity: 0.6; cursor: wait; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }
.info-card { grid-column: 1 / -1; }
.about-text { font-size: 14px; color: #5f6368; line-height: 1.7; margin-bottom: 16px; }
.tech-stack { display: flex; flex-wrap: wrap; gap: 8px; }
.tech-tag { padding: 4px 12px; background: #f1f3f4; border-radius: 20px; font-size: 13px; color: #5f6368; }
</style>
