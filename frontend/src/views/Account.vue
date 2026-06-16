<template>
  <div class="account-page">
    <!-- 顶部标题栏 -->
    <div class="acct-topbar">
      <div class="acct-topbar-left">
        <h1>账户设置</h1>
        <span class="acct-id">ID: #{{ userId }}</span>
      </div>
    </div>

    <div class="acct-body">
      <!-- 左侧导航 -->
      <nav class="acct-nav">
        <div :class="['acct-nav-item', { active: section==='profile' }]" @click="section='profile'">
          <svg width="18" height="18" viewBox="0 0 18 18"><circle cx="9" cy="5.5" r="3.5" stroke="currentColor" stroke-width="1.4"/><path d="M2 16c0-3.5 3-6.5 7-6.5s7 3 7 6.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
          <span>个人信息</span>
        </div>
        <div :class="['acct-nav-item', { active: section==='security' }]" @click="section='security'">
          <svg width="18" height="18" viewBox="0 0 18 18"><rect x="2.5" y="7" width="13" height="9" rx="2" stroke="currentColor" stroke-width="1.4"/><circle cx="9" cy="11.5" r="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M9 11.5V14M5 7V4.5a4 4 0 0 1 8 0V7" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
          <span>安全设置</span>
        </div>
        <div :class="['acct-nav-item', { active: section==='activity' }]" @click="section='activity'">
          <svg width="18" height="18" viewBox="0 0 18 18"><circle cx="9" cy="9" r="7" stroke="currentColor" stroke-width="1.4"/><path d="M9 4v5l3.5 3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
          <span>活动概览</span>
        </div>
      </nav>

      <!-- 右侧内容区 -->
      <div class="acct-content">

        <!-- ========== 个人信息 ========== -->
        <div v-if="section==='profile'" class="section-panel">
          <div class="profile-banner">
            <div class="pb-left">
              <div class="pb-avatar">{{ username?.charAt(0)?.toUpperCase() }}</div>
              <div class="pb-info">
                <h2>{{ username }}</h2>
                <p class="pb-meta">注册于 {{ formatDate(info.createTime) }} · 账户状态 <span class="status-ok">正常</span></p>
              </div>
            </div>
            <button class="pb-edit-btn" @click="section='security'">
              <svg width="14" height="14" viewBox="0 0 14 14"><rect x="1.5" y="5" width="11" height="7.5" rx="1.8" stroke="currentColor" stroke-width="1.2"/><circle cx="7" cy="8.5" r="1.2" stroke="currentColor" stroke-width="1"/><path d="M7 8.5v2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
              修改密码
            </button>
          </div>

          <!-- 统计 -->
          <div class="stats-row">
            <div class="stats-card">
              <div class="stats-card-icon c-blue">
                <svg width="20" height="20" viewBox="0 0 20 20"><rect x="3" y="2" width="14" height="16" rx="2" stroke="currentColor" stroke-width="1.4"/><path d="M7 7h6M7 11h6M7 15h3" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
              </div>
              <div class="stats-card-val">{{ info.docCount ?? 0 }}</div>
              <div class="stats-card-label">上传文档</div>
            </div>
            <div class="stats-card">
              <div class="stats-card-icon c-green">
                <svg width="20" height="20" viewBox="0 0 20 20"><path d="M3 14V4.667A1.333 1.333 0 0 1 4.333 3.333h11.334A1.333 1.333 0 0 1 17 4.667V12a1.333 1.333 0 0 1-1.333 1.333H7l-4 4V14Z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="stats-card-val">{{ info.convCount ?? 0 }}</div>
              <div class="stats-card-label">对话总数</div>
            </div>
            <div class="stats-card">
              <div class="stats-card-icon c-purple">
                <svg width="20" height="20" viewBox="0 0 20 20"><circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.4"/><path d="M10 5v5l3.5 3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              </div>
              <div class="stats-card-val">{{ daysAgo(info.createTime) }} 天</div>
              <div class="stats-card-label">加入时长</div>
            </div>
            <div class="stats-card">
              <div class="stats-card-icon c-amber">
                <svg width="20" height="20" viewBox="0 0 20 20"><path d="M10 2 2 7v6l8 5 8-5V7L10 2z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/><circle cx="10" cy="10" r="2" stroke="currentColor" stroke-width="1.2"/></svg>
              </div>
              <div class="stats-card-val">{{ themeLabel }}</div>
              <div class="stats-card-label">当前主题</div>
            </div>
          </div>

          <!-- 详情 -->
          <div class="detail-grid">
            <div class="detail-card">
              <h3 class="dc-title">基本信息</h3>
              <div class="dc-row">
                <div class="dc-item"><span class="dc-label">用户 ID</span><span class="dc-value mono">#{{ userId }}</span></div>
                <div class="dc-item"><span class="dc-label">用户名</span><span class="dc-value">{{ username }}</span></div>
              </div>
              <div class="dc-row">
                <div class="dc-item"><span class="dc-label">注册时间</span><span class="dc-value">{{ formatDateTime(info.createTime) }}</span></div>
                <div class="dc-item"><span class="dc-label">密码保护</span><span class="dc-value"><span class="pill">已启用</span></span></div>
              </div>
            </div>
            <div class="detail-card">
              <h3 class="dc-title">用量统计</h3>
              <div class="dc-row">
                <div class="dc-item"><span class="dc-label">文档总数</span><span class="dc-value strong">{{ info.docCount ?? 0 }}</span></div>
                <div class="dc-item"><span class="dc-label">对话次数</span><span class="dc-value strong">{{ info.convCount ?? 0 }}</span></div>
              </div>
              <div class="dc-row">
                <div class="dc-item"><span class="dc-label">平均每文档对话</span><span class="dc-value strong">{{ avgConvPerDoc }}</span></div>
                <div class="dc-item"><span class="dc-label">账户创建于</span><span class="dc-value">{{ daysAgo(info.createTime) }} 天前</span></div>
              </div>
            </div>
          </div>
        </div>

        <!-- ========== 安全设置 ========== -->
        <div v-if="section==='security'" class="section-panel">
          <div class="sec-header">
            <h3>修改登录密码</h3>
            <p>定期更换密码可以有效保护你的账户安全</p>
          </div>

          <div class="pwd-form-card">
            <div class="pwd-field">
              <label>当前密码</label>
              <div class="pwd-input-wrap">
                <svg width="16" height="16" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.2"/><circle cx="8" cy="7" r="1.8" stroke="currentColor" stroke-width="1"/><path d="M5.5 11c1-2 4-2 5 0" stroke="currentColor" stroke-width="1" stroke-linecap="round"/></svg>
                <input v-model="pwd.oldPassword" type="password" placeholder="输入当前密码" />
              </div>
            </div>

            <div class="pwd-separator"><span>设置新密码</span></div>

            <div class="pwd-field">
              <label>新密码</label>
              <div class="pwd-input-wrap">
                <svg width="16" height="16" viewBox="0 0 16 16"><rect x="2.5" y="6" width="11" height="8" rx="1.8" stroke="currentColor" stroke-width="1.2"/><circle cx="8" cy="10" r="1.2" stroke="currentColor" stroke-width="1"/><path d="M8 10v2M4.5 6V4a3.5 3.5 0 0 1 7 0v2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                <input v-model="pwd.newPassword" type="password" placeholder="输入新密码，至少 6 位" />
              </div>
            </div>

            <div class="pwd-field">
              <label>确认新密码</label>
              <div class="pwd-input-wrap" :class="{ err: pwd.confirmPassword && pwd.newPassword !== pwd.confirmPassword }">
                <svg width="16" height="16" viewBox="0 0 16 16"><rect x="2.5" y="6" width="11" height="8" rx="1.8" stroke="currentColor" stroke-width="1.2"/><path d="M5 10l2 2 4-4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <input v-model="pwd.confirmPassword" type="password" placeholder="再次输入新密码" />
              </div>
              <p v-if="pwd.confirmPassword && pwd.newPassword !== pwd.confirmPassword" class="pwd-err">两次输入的密码不一致</p>
            </div>

            <!-- 强度 -->
            <div class="pwd-strength" v-if="pwd.newPassword">
              <div class="pwd-strength-top">
                <span>密码强度</span>
                <span :style="{color: strengthColor}" class="pwd-strength-label">{{ strengthText }}</span>
              </div>
              <div class="pwd-strength-track">
                <div class="pwd-strength-bar" :style="{width: strengthPercent+'%', background: strengthColor}"></div>
              </div>
              <div class="pwd-strength-rules">
                <span :class="{ok: pwd.newPassword.length >= 6}"><span class="chk">{{ pwd.newPassword.length >= 6 ? '✓' : '○' }}</span> 至少 6 位字符</span>
                <span :class="{ok: /[A-Z]/.test(pwd.newPassword)}"><span class="chk">{{ /[A-Z]/.test(pwd.newPassword) ? '✓' : '○' }}</span> 包含大写字母</span>
                <span :class="{ok: /[0-9]/.test(pwd.newPassword)}"><span class="chk">{{ /[0-9]/.test(pwd.newPassword) ? '✓' : '○' }}</span> 包含数字</span>
                <span :class="{ok: /[^A-Za-z0-9]/.test(pwd.newPassword)}"><span class="chk">{{ /[^A-Za-z0-9]/.test(pwd.newPassword) ? '✓' : '○' }}</span> 包含特殊符号</span>
              </div>
            </div>
          </div>

          <button class="pwd-submit-btn" @click="submitChangePwd" :disabled="!canSubmit || submitting">
            <svg v-if="submitting" class="spin" width="16" height="16" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="2" fill="none" stroke-dasharray="30 10"/></svg>
            {{ submitting ? '正在提交...' : '更新密码' }}
          </button>

          <div class="sec-tips">
            <div class="tips-row">
              <div class="tip-item">
                <div class="tip-num">1</div>
                <div>
                  <strong>密码强度</strong>
                  <p>建议使用 8 位以上，包含大小写字母、数字和特殊符号的组合</p>
                </div>
              </div>
              <div class="tip-item">
                <div class="tip-num">2</div>
                <div>
                  <strong>定期更换</strong>
                  <p>建议每 3-6 个月更换一次密码，保证账户安全</p>
                </div>
              </div>
              <div class="tip-item">
                <div class="tip-num">3</div>
                <div>
                  <strong>不要重复</strong>
                  <p>避免使用与其他平台相同的密码，防止撞库攻击</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ========== 活动概览 ========== -->
        <div v-if="section==='activity'" class="section-panel">
          <div class="sec-header">
            <h3>账户活动</h3>
            <p>你的使用情况一目了然</p>
          </div>

          <div class="activity-grid">
            <div class="activity-card">
              <div class="act-icon" style="background:#eff6ff;color:#2563eb">
                <svg width="28" height="28" viewBox="0 0 28 28"><rect x="4" y="3" width="20" height="22" rx="3" stroke="currentColor" stroke-width="1.5"/><path d="M9 9h10M9 14h10M9 19h6" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
              </div>
              <div class="act-body">
                <div class="act-num">{{ info.docCount ?? 0 }}</div>
                <div class="act-label">累计上传文档</div>
              </div>
              <div class="act-footer">支持 PDF / TXT 格式</div>
            </div>
            <div class="activity-card">
              <div class="act-icon" style="background:#f0fdf4;color:#16a34a">
                <svg width="28" height="28" viewBox="0 0 28 28"><path d="M5 21V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H11l-6 5.5V21Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="act-body">
                <div class="act-num">{{ info.convCount ?? 0 }}</div>
                <div class="act-label">发起对话次数</div>
              </div>
              <div class="act-footer">含追问与重新生成</div>
            </div>
            <div class="activity-card">
              <div class="act-icon" style="background:#f5f3ff;color:#7c3aed">
                <svg width="28" height="28" viewBox="0 0 28 28"><circle cx="14" cy="14" r="11" stroke="currentColor" stroke-width="1.5"/><path d="M14 7v7l5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </div>
              <div class="act-body">
                <div class="act-num">{{ daysAgo(info.createTime) }}</div>
                <div class="act-label">账户活跃天数</div>
              </div>
              <div class="act-footer">自 {{ formatDate(info.createTime) }} 起</div>
            </div>
          </div>

          <div class="activity-summary">
            <h4>系统信息</h4>
            <div class="summary-grid">
              <div class="summary-item"><span class="sm-label">LLM 引擎</span><span class="sm-value">DeepSeek + Kimi 双模型</span></div>
              <div class="summary-item"><span class="sm-label">向量存储</span><span class="sm-value">Apache Lucene 9 (HNSW)</span></div>
              <div class="summary-item"><span class="sm-label">文档处理</span><span class="sm-value">PDFBox + 智能分块</span></div>
              <div class="summary-item"><span class="sm-label">系统状态</span><span class="sm-value"><span class="status-ok">● 运行中</span></span></div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const section = ref('profile')
const username = sessionStorage.getItem('username') || '用户'
const userId = sessionStorage.getItem('userId') || '-'
const info = ref({})
const submitting = ref(false)

const pwd = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const canSubmit = computed(() => {
  return pwd.value.oldPassword &&
         pwd.value.newPassword.length >= 6 &&
         pwd.value.newPassword === pwd.value.confirmPassword
})

const strengthPercent = computed(() => {
  const p = pwd.value.newPassword; if (!p) return 0
  let s = Math.min(p.length * 12, 50)
  if (/[A-Z]/.test(p)) s += 15
  if (/[0-9]/.test(p)) s += 15
  if (/[^A-Za-z0-9]/.test(p)) s += 20
  return Math.min(s, 100)
})
const strengthColor = computed(() => {
  const s = strengthPercent.value
  if (s < 40) return '#ea4335'; if (s < 70) return '#d97706'; return '#16a34a'
})
const strengthText = computed(() => {
  const s = strengthPercent.value
  if (s < 40) return '较弱'; if (s < 70) return '中等'; return '安全'
})

const avgConvPerDoc = computed(() => {
  if (!info.value.docCount || info.value.docCount === 0) return '-'
  return ((info.value.convCount || 0) / info.value.docCount).toFixed(1)
})

const themeLabel = computed(() => {
  const t = localStorage.getItem('theme') || 'blue'
  const m = { blue:'蓝色', green:'绿色', purple:'紫色', orange:'橙色', dark:'暗色' }
  return m[t] || '蓝色'
})

onMounted(async () => {
  try { const r = await api.getUserInfo(); info.value = r.data } catch {}
})

async function submitChangePwd() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await api.changePassword(pwd.value.oldPassword, pwd.value.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => { sessionStorage.clear(); window.location.href = '/login' }, 1500)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  } finally { submitting.value = false }
}

function formatDate(t) {
  if (!t) return '-'
  return new Date(t).toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric' })
}
function formatDateTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' })
}
function daysAgo(t) {
  if (!t) return '-'
  return Math.max(1, Math.floor((Date.now() - new Date(t).getTime()) / 86400000))
}
</script>

<style scoped>
.account-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
  overflow: hidden;
}

.acct-topbar {
  padding: 20px 32px;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.acct-topbar-left h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}
.acct-id {
  font-size: 12px;
  color: var(--color-text-muted);
  font-family: 'SF Mono', Consolas, monospace;
  margin-left: 12px;
  background: var(--color-bg);
  padding: 3px 10px;
  border-radius: 6px;
}

/* 主体 */
.acct-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 侧边栏 */
.acct-nav {
  width: 220px;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-light);
  padding: 16px 10px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.acct-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition);
  font-weight: 500;
}
.acct-nav-item:hover { background: var(--color-bg); color: var(--color-text); }
.acct-nav-item.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 600;
}
.acct-nav-item.active svg { color: var(--color-primary); }

/* 内容区 */
.acct-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  min-width: 0;
}
.section-panel {
  width: 100%;
}

/* ---- 个人信息 ---- */
.profile-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 24px;
}
.pb-left { display: flex; align-items: center; gap: 18px; }
.pb-avatar {
  width: 68px;
  height: 68px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  box-shadow: 0 6px 20px rgba(37,99,235,.3);
  flex-shrink: 0;
}
.pb-info h2 { font-size: 20px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.pb-meta { font-size: 13px; color: var(--color-text-muted); }
.status-ok { color: #16a34a; font-weight: 600; }
.pb-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  border: 1px solid var(--border-card);
  background: var(--bg-card);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--transition);
}
.pb-edit-btn:hover { background: var(--color-primary-bg); border-color: var(--color-primary); }

/* 统计行 */
.stats-row { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 14px; margin-bottom: 24px; }
.stats-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 20px;
  text-align: center;
  transition: all var(--transition);
}
.stats-card:hover { border-color: var(--color-primary); transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stats-card-icon {
  width: 42px; height: 42px; border-radius: 12px;
  display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.c-blue  { background: #eff6ff; color: #2563eb; }
.c-green { background: #f0fdf4; color: #16a34a; }
.c-purple{ background: #f5f3ff; color: #7c3aed; }
.c-amber { background: #fff7ed; color: #ea580c; }
.stats-card-val { font-size: 24px; font-weight: 700; color: var(--color-text); }
.stats-card-label { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; text-transform: uppercase; letter-spacing: .5px; }

/* 详情网格 */
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.detail-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 24px;
}
.dc-title { font-size: 14px; font-weight: 600; color: var(--color-text); margin-bottom: 18px; padding-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.dc-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 12px; }
.dc-row:last-child { margin-bottom: 0; }
.dc-item { display: flex; flex-direction: column; gap: 4px; }
.dc-label { font-size: 11px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: .4px; font-weight: 500; }
.dc-value { font-size: 14px; color: var(--color-text); font-weight: 500; }
.dc-value.mono { font-family: 'SF Mono', Consolas, monospace; color: var(--color-primary); }
.dc-value.strong { font-size: 18px; font-weight: 700; }
.pill { display: inline-block; padding: 2px 10px; background: #e6f4ea; color: #137333; border-radius: 20px; font-size: 12px; font-weight: 600; }

/* ---- 安全设置 ---- */
.sec-header { margin-bottom: 24px; }
.sec-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.sec-header p { font-size: 13px; color: var(--color-text-muted); }

.pwd-form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 20px;
}
.pwd-field { margin-bottom: 20px; }
.pwd-field:last-of-type { margin-bottom: 0; }
.pwd-field label { display: block; font-size: 13px; font-weight: 600; color: var(--color-text); margin-bottom: 8px; }
.pwd-input-wrap {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 16px;
  border: 1.5px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-input);
  transition: all var(--transition);
}
.pwd-input-wrap:focus-within { border-color: var(--color-primary); box-shadow: 0 0 0 3px var(--color-primary-bg); }
.pwd-input-wrap.err { border-color: #ea4335; }
.pwd-input-wrap svg { color: var(--color-text-muted); flex-shrink: 0; }
.pwd-input-wrap input {
  flex: 1; border: none; background: none; outline: none;
  font-size: 14px; color: var(--color-text); font-family: inherit;
}
.pwd-input-wrap input::placeholder { color: var(--color-text-muted); }
.pwd-err { font-size: 12px; color: #ea4335; margin-top: 6px; }
.pwd-separator {
  text-align: center; margin: 24px 0 20px; position: relative;
  font-size: 12px; color: var(--color-text-muted); font-weight: 500;
}
.pwd-separator::before {
  content: ''; position: absolute; top: 50%; left: 0; right: 0; height: 1px; background: var(--border-light);
}
.pwd-separator span {
  position: relative; background: var(--bg-card); padding: 0 16px;
}

.pwd-strength { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--border-light); }
.pwd-strength-top { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 8px; color: var(--color-text-secondary); }
.pwd-strength-label { font-weight: 600; }
.pwd-strength-track { height: 6px; background: var(--border-light); border-radius: 3px; overflow: hidden; margin-bottom: 14px; }
.pwd-strength-bar { height: 100%; border-radius: 3px; transition: all .4s ease; }
.pwd-strength-rules { display: flex; gap: 14px; flex-wrap: wrap; }
.pwd-strength-rules span { font-size: 12px; color: var(--color-text-muted); display: flex; align-items: center; gap: 4px; }
.pwd-strength-rules .chk { font-family: monospace; font-size: 13px; }
.pwd-strength-rules span.ok { color: #137333; }

.pwd-submit-btn {
  width: 100%; padding: 14px;
  border: none; background: var(--color-primary); color: #fff;
  font-size: 15px; font-weight: 600; border-radius: 10px; cursor: pointer;
  transition: all var(--transition); display: flex; align-items: center; justify-content: center; gap: 8px;
  margin-bottom: 28px;
}
.pwd-submit-btn:hover { background: var(--color-primary-dark); box-shadow: 0 4px 16px rgba(37,99,235,.3); }
.pwd-submit-btn:disabled { opacity: .4; cursor: not-allowed; box-shadow: none; }

.sec-tips { background: #fffbeb; border: 1px solid #fde68a; border-radius: 14px; padding: 24px; }
.tips-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 24px; }
.tip-item { display: flex; gap: 14px; }
.tip-num {
  width: 28px; height: 28px; border-radius: 8px; background: #f59e0b; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; flex-shrink: 0;
}
.tip-item strong { display: block; font-size: 13px; color: #92400e; margin-bottom: 4px; }
.tip-item p { font-size: 12px; color: #a16207; line-height: 1.5; margin: 0; }

/* ---- 活动概览 ---- */
.activity-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; margin-bottom: 24px; }
.activity-card {
  background: var(--bg-card); border: 1px solid var(--border-card);
  border-radius: 14px; padding: 24px; text-align: center;
}
.act-icon { width: 56px; height: 56px; border-radius: 16px; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 14px; }
.act-body { margin-bottom: 12px; }
.act-num { font-size: 28px; font-weight: 700; color: var(--color-text); }
.act-label { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; }
.act-footer { font-size: 11px; color: var(--color-text-muted); padding-top: 10px; border-top: 1px solid var(--border-light); }

.activity-summary {
  background: var(--bg-card); border: 1px solid var(--border-card);
  border-radius: 14px; padding: 24px;
}
.activity-summary h4 { font-size: 14px; font-weight: 600; color: var(--color-text); margin-bottom: 16px; }
.summary-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.summary-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border-light); }
.summary-item:last-child { border-bottom: none; }
.sm-label { font-size: 13px; color: var(--color-text-secondary); }
.sm-value { font-size: 13px; color: var(--color-text); font-weight: 500; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }
</style>
