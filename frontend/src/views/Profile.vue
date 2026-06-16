<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- 顶部头像区 -->
      <div class="profile-banner">
        <div class="banner-bg"></div>
        <div class="banner-content">
          <div class="profile-avatar-lg">{{ username?.charAt(0)?.toUpperCase() }}</div>
          <div class="banner-info">
            <h2>{{ username }}</h2>
            <span class="banner-badge"><span class="dot green"></span> 正常</span>
            <p class="banner-date">加入于 {{ formatDate(info.createTime) }}</p>
          </div>
        </div>
      </div>

      <!-- 统计卡片行 -->
      <div class="stat-row">
        <div class="stat-box">
          <div class="stat-box-icon blue">
            <svg width="20" height="20" viewBox="0 0 20 20"><rect x="3" y="2" width="14" height="16" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M7 7h6M7 11h6M7 15h3" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </div>
          <div class="stat-box-body">
            <div class="stat-box-num">{{ info.docCount ?? 0 }}</div>
            <div class="stat-box-label">上传文档</div>
          </div>
        </div>
        <div class="stat-box">
          <div class="stat-box-icon green">
            <svg width="20" height="20" viewBox="0 0 20 20"><path d="M3 14V4.667A1.333 1.333 0 0 1 4.333 3.333h11.334A1.333 1.333 0 0 1 17 4.667V12a1.333 1.333 0 0 1-1.333 1.333H7l-4 4V14Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="stat-box-body">
            <div class="stat-box-num">{{ info.convCount ?? 0 }}</div>
            <div class="stat-box-label">对话发起</div>
          </div>
        </div>
        <div class="stat-box">
          <div class="stat-box-icon purple">
            <svg width="20" height="20" viewBox="0 0 20 20"><circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.5"/><path d="M10 5v5l3.5 3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="stat-box-body">
            <div class="stat-box-num">{{ daysAgo(info.createTime) }}</div>
            <div class="stat-box-label">加入天数</div>
          </div>
        </div>
      </div>

      <!-- 详情卡片 -->
      <div class="detail-card">
        <h3 class="card-title">账户详情</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">用户名</span>
            <span class="detail-value">{{ username }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">用户 ID</span>
            <span class="detail-value mono">#{{ userId }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">注册时间</span>
            <span class="detail-value">{{ formatDateTime(info.createTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">密码状态</span>
            <span class="detail-value"><span class="pill green">已设置</span></span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-row">
        <button class="btn-primary" @click="$router.push('/change-password')">
          <svg width="16" height="16" viewBox="0 0 16 16"><rect x="1.5" y="5.5" width="13" height="9" rx="2" stroke="currentColor" stroke-width="1.3"/><circle cx="8" cy="10" r="1.5" stroke="currentColor" stroke-width="1"/><path d="M8 10v2M4.5 5.5V3.5a3.5 3.5 0 0 1 7 0v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
          修改密码
        </button>
        <button class="btn-ghost" @click="$router.push('/')">
          <svg width="16" height="16" viewBox="0 0 16 16"><path d="M10 3 5 8l5 5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
          返回首页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const username = sessionStorage.getItem('username') || '用户'
const userId = sessionStorage.getItem('userId') || '-'
const info = ref({})

onMounted(async () => {
  try {
    const r = await api.getUserInfo()
    info.value = r.data
  } catch (e) { ElMessage.error('加载失败') }
})

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
.profile-page {
  height: 100%;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  padding: 32px 24px;
}
.profile-container {
  width: 100%;
  max-width: 600px;
}

/* 顶部 Banner */
.profile-banner {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
}
.banner-bg {
  height: 64px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
}
.banner-content {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  padding: 0 24px 20px;
  margin-top: -32px;
}
.profile-avatar-lg {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  border: 4px solid var(--bg-card);
  box-shadow: 0 4px 16px rgba(37,99,235,.25);
  flex-shrink: 0;
}
.banner-info { padding-bottom: 4px; }
.banner-info h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.banner-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #137333;
  font-weight: 500;
  margin-bottom: 4px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}
.dot.green { background: #16a34a; }
.banner-date {
  font-size: 13px;
  color: var(--color-text-muted);
}

/* 统计行 */
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}
.stat-box {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-box-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-box-icon.blue   { background: #eff6ff; color: #2563eb; }
.stat-box-icon.green  { background: #f0fdf4; color: #16a34a; }
.stat-box-icon.purple { background: #f5f3ff; color: #7c3aed; }
.stat-box-body { min-width: 0; }
.stat-box-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.1;
}
.stat-box-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 详情卡片 */
.detail-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-label {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.detail-value {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}
.detail-value.mono {
  font-family: 'SF Mono', 'Consolas', monospace;
  color: var(--color-primary);
}
.pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.pill.green { background: #e6f4ea; color: #137333; }

/* 操作按钮 */
.action-row {
  display: flex;
  gap: 12px;
}
.btn-primary {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition);
}
.btn-primary:hover { background: var(--color-primary-dark); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(37,99,235,.3); }
.btn-ghost {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1px solid var(--border-card);
  background: var(--bg-card);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition);
}
.btn-ghost:hover { background: var(--color-bg); border-color: var(--color-border); }
</style>
