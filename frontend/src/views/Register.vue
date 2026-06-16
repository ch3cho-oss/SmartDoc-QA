<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- 左侧品牌区 -->
      <div class="auth-brand">
        <div class="brand-content">
          <div class="brand-icon">
            <svg viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="12" fill="#2563eb"/>
              <path d="M14 18h8v12h-8zM20 18l4-6 4 6M28 22h6M28 26h6" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="32" cy="30" r="5" stroke="#fff" stroke-width="2"/>
              <path d="M35 33l3 3" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <h1>加入智能问答</h1>
          <p>创建账号，开启智能文档问答之旅</p>
          <div class="features">
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>个人文档知识库管理</span>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>多轮对话历史保存</span>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>完全免费使用</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form-area">
        <div class="form-wrapper">
          <h2>创建账号</h2>
          <p class="form-subtitle">填写信息完成注册</p>

          <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="register">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名（3-20位）"
                size="large"
                clearable
              >
                <template #prefix>
                  <span class="input-icon">@</span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码（至少6位）"
                size="large"
                show-password
                clearable
              >
                <template #prefix>
                  <span class="input-icon">
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <rect x="2" y="7" width="12" height="7" rx="2" stroke="#9ca3af" stroke-width="1.5"/>
                      <path d="M5 7V5a3 3 0 0 1 6 0v2" stroke="#9ca3af" stroke-width="1.5" stroke-linecap="round"/>
                      <circle cx="8" cy="10" r="1" fill="#9ca3af"/>
                    </svg>
                  </span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                show-password
                clearable
              >
                <template #prefix>
                  <span class="input-icon">
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <rect x="2" y="7" width="12" height="7" rx="2" stroke="#9ca3af" stroke-width="1.5"/>
                      <path d="M5 7V5a3 3 0 0 1 6 0v2" stroke="#9ca3af" stroke-width="1.5" stroke-linecap="round"/>
                      <circle cx="8" cy="10" r="1" fill="#9ca3af"/>
                    </svg>
                  </span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                @click="register"
                :loading="loading"
                class="submit-btn"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="link">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function register() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await api.register({ username: form.username, password: form.password })
    ElMessage.success('注册成功')
    router.push('/login')
  } catch (e) { /* API 拦截器已处理 */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  padding: 20px;
}

.auth-container {
  display: flex;
  width: 900px;
  min-height: 560px;
  background: #fff;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.auth-brand {
  width: 420px;
  background: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.brand-content {
  color: #fff;
  text-align: left;
}

.brand-icon {
  margin-bottom: 24px;
}

.brand-content h1 {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.brand-content > p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 32px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #60a5fa;
  flex-shrink: 0;
}

.auth-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 340px;
}

.form-wrapper h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
}

.form-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.input-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  color: var(--color-text-muted);
  font-size: 15px;
  font-weight: 500;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: var(--radius);
}

.form-footer {
  text-align: center;
  font-size: 15px;
  color: var(--color-text-secondary);
}

.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
  cursor: pointer;
}

.link:hover {
  color: var(--color-primary-dark);
}

@media (max-width: 768px) {
  .auth-container { flex-direction: column; width: 100%; }
  .auth-brand { width: 100%; padding: 32px 24px; }
  .auth-form-area { padding: 32px 24px; }
}
</style>
