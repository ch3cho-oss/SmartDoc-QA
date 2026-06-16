<template>
  <div class="pwd-page">
    <div class="pwd-container">
      <!-- 顶部 -->
      <div class="pwd-header">
        <div class="pwd-header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24"><rect x="3" y="8" width="18" height="13" rx="2.5" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="14.5" r="2" stroke="currentColor" stroke-width="1.3"/><path d="M12 14.5v2.5M6 8V5.5a6 6 0 0 1 12 0V8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
        </div>
        <h2>修改密码</h2>
        <p class="hint">保护你的账户安全，建议使用强密码</p>
      </div>

      <!-- 表单卡片 -->
      <div class="form-card">
        <!-- 旧密码 -->
        <div class="field">
          <label>当前密码</label>
          <div class="input-box">
            <svg width="16" height="16" viewBox="0 0 16 16"><rect x="2" y="5.5" width="12" height="8.5" rx="1.5" stroke="currentColor" stroke-width="1.2"/><circle cx="8" cy="9.5" r="1.3" stroke="currentColor" stroke-width="1"/></svg>
            <input v-model="form.oldPassword" type="password" placeholder="输入旧密码以验证身份" />
          </div>
        </div>

        <div class="divider"></div>

        <!-- 新密码 -->
        <div class="field">
          <label>新密码</label>
          <div class="input-box">
            <svg width="16" height="16" viewBox="0 0 16 16"><path d="M5 8V5a3 3 0 0 1 6 0v3M3 8h10a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            <input v-model="form.newPassword" type="password" placeholder="至少 6 位字母、数字或符号" />
          </div>
        </div>

        <!-- 确认新密码 -->
        <div class="field">
          <label>确认新密码</label>
          <div class="input-box" :class="{ error: form.confirmPassword && form.newPassword !== form.confirmPassword }">
            <svg width="16" height="16" viewBox="0 0 16 16"><rect x="2" y="2" width="12" height="12" rx="2" stroke="currentColor" stroke-width="1.2"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <input v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" />
          </div>
          <span v-if="form.confirmPassword && form.newPassword !== form.confirmPassword" class="field-error">两次输入不一致</span>
        </div>

        <!-- 强度条 -->
        <div class="strength-section" v-if="form.newPassword">
          <div class="strength-header">
            <span>密码强度</span>
            <span :style="{ color: strengthColor }" class="strength-text">{{ strengthText }}</span>
          </div>
          <div class="strength-track">
            <div class="strength-fill" :style="{ width: strengthPercent + '%', background: strengthColor }"></div>
          </div>
          <div class="strength-checks">
            <span :class="{ pass: form.newPassword.length >= 6 }">至少 6 位</span>
            <span :class="{ pass: /[A-Z]/.test(form.newPassword) }">含大写字母</span>
            <span :class="{ pass: /[0-9]/.test(form.newPassword) }">含数字</span>
            <span :class="{ pass: /[^A-Za-z0-9]/.test(form.newPassword) }">含特殊符号</span>
          </div>
        </div>
      </div>

      <!-- 操作 -->
      <div class="btn-row">
        <button class="btn-save" @click="submit" :disabled="!canSubmit || submitting">
          {{ submitting ? '提交中...' : '确认修改密码' }}
        </button>
        <button class="btn-back" @click="$router.push('/profile')">取消，返回</button>
      </div>

      <!-- 提示 -->
      <div class="tips-card">
        <div class="tips-icon">!</div>
        <div>
          <strong>安全提示</strong>
          <p>密码修改成功后你需要使用新密码重新登录。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const form = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const submitting = ref(false)

const canSubmit = computed(() => {
  return form.value.oldPassword &&
         form.value.newPassword.length >= 6 &&
         form.value.newPassword === form.value.confirmPassword
})

const strengthPercent = computed(() => {
  const p = form.value.newPassword
  if (!p) return 0
  let s = Math.min(p.length * 12, 50)
  if (/[A-Z]/.test(p)) s += 15
  if (/[0-9]/.test(p)) s += 15
  if (/[^A-Za-z0-9]/.test(p)) s += 20
  return Math.min(s, 100)
})
const strengthColor = computed(() => {
  const s = strengthPercent.value
  if (s < 40) return '#ea4335'
  if (s < 70) return '#d97706'
  return '#16a34a'
})
const strengthText = computed(() => {
  const s = strengthPercent.value
  if (s < 40) return '较弱'
  if (s < 70) return '中等'
  return '安全'
})

async function submit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await api.changePassword(form.value.oldPassword, form.value.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => { sessionStorage.clear(); router.push('/login') }, 1500)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  } finally { submitting.value = false }
}
</script>

<style scoped>
.pwd-page {
  height: 100%;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  padding: 32px 24px;
}
.pwd-container {
  width: 100%;
  max-width: 480px;
}

/* 头部 */
.pwd-header {
  text-align: center;
  margin-bottom: 24px;
}
.pwd-header-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--color-primary-bg), #dbeafe);
  color: var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.pwd-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
}
.hint {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 6px;
}

/* 表单卡片 */
.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}
.field {
  margin-bottom: 18px;
}
.field:last-of-type { margin-bottom: 0; }
.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}
.input-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-input);
  transition: all var(--transition);
}
.input-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-bg);
}
.input-box.error {
  border-color: #ea4335;
}
.input-box svg {
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.input-box input {
  flex: 1;
  border: none;
  background: none;
  font-size: 14px;
  color: var(--color-text);
  outline: none;
  font-family: inherit;
}
.input-box input::placeholder { color: var(--color-text-muted); }
.field-error {
  display: block;
  font-size: 12px;
  color: #ea4335;
  margin-top: 6px;
}
.divider {
  height: 1px;
  background: var(--border-light);
  margin: 18px 0;
}

/* 强度 */
.strength-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}
.strength-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
.strength-text { font-weight: 600; }
.strength-track {
  height: 5px;
  background: var(--border-light);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 10px;
}
.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: all 0.3s ease;
}
.strength-checks {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.strength-checks span {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  background: var(--border-light);
  color: var(--color-text-muted);
  transition: all 0.2s;
}
.strength-checks span.pass {
  background: #e6f4ea;
  color: #137333;
}

/* 按钮 */
.btn-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}
.btn-save {
  padding: 13px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition);
}
.btn-save:hover { background: var(--color-primary-dark); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(37,99,235,.3); }
.btn-save:disabled { opacity: 0.4; transform: none; box-shadow: none; cursor: not-allowed; }
.btn-back {
  padding: 11px;
  border: 1px solid var(--border-card);
  background: var(--bg-card);
  color: var(--color-text-secondary);
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition);
}
.btn-back:hover { background: var(--color-bg); }

/* 提示 */
.tips-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: #fef9e7;
  border: 1px solid #fde68a;
  border-radius: 12px;
}
.tips-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #d97706;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.tips-card strong {
  display: block;
  font-size: 13px;
  color: #92600e;
  margin-bottom: 3px;
}
.tips-card p {
  font-size: 12px;
  color: #a16207;
  margin: 0;
  line-height: 1.5;
}
</style>
