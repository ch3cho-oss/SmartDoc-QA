<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="auth-brand">
        <div class="brand-content">
          <div class="brand-icon">
            <svg viewBox="0 0 48 48" fill="none"><rect width="48" height="48" rx="14" fill="rgba(255,255,255,.2)"/><path d="M14 18h8v12h-8zM20 18l4-6 4 6M28 22h6M28 26h6" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="32" cy="30" r="5" stroke="#fff" stroke-width="2"/><path d="M35 33l3 3" stroke="#fff" stroke-width="2" stroke-linecap="round"/></svg>
          </div>
          <h1>智能文档问答系统</h1>
          <p>基于 RAG 技术，让文档问答更智能</p>
          <div class="features">
            <div class="feature-item"><span class="feature-dot"></span><span>上传 PDF / TXT 文档</span></div>
            <div class="feature-item"><span class="feature-dot"></span><span>自然语言智能问答</span></div>
            <div class="feature-item"><span class="feature-dot"></span><span>原文引用溯源高亮</span></div>
          </div>
        </div>
      </div>

      <div class="auth-form-area">
        <div class="form-wrapper">
          <h2>欢迎回来</h2>
          <p class="form-subtitle">请登录您的账号继续使用</p>
          <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="login">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable><template #prefix><span class="input-icon">@</span></template></el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password clearable><template #prefix><span class="input-icon"><svg width="16" height="16" viewBox="0 0 16 16"><rect x="2" y="7" width="12" height="7" rx="2" stroke="#9ca3af" stroke-width="1.5"/><path d="M5 7V5a3 3 0 0 1 6 0v2" stroke="#9ca3af" stroke-width="1.5" stroke-linecap="round"/><circle cx="8" cy="10" r="1" fill="#9ca3af"/></svg></span></template></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" @click="login" :loading="loading" class="submit-btn">登 录</el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer"><span>还没有账号？</span><router-link to="/register" class="link">立即注册</router-link></div>
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
const form = reactive({ username: '', password: '' })
const rules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }] }

async function login() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await api.login(form)
    sessionStorage.setItem('userId', res.data.id)
    sessionStorage.setItem('username', res.data.username)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {} finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f1f5f9; padding: 20px; }
.auth-container { display: flex; width: 900px; min-height: 560px; background: #fff; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,.08), 0 0 0 1px rgba(0,0,0,.03); overflow: hidden; }
.auth-brand { width: 420px; background: #2563eb; display: flex; align-items: center; justify-content: center; padding: 48px; }
.brand-content { color: #fff; text-align: left; }
.brand-icon { margin-bottom: 24px; }
.brand-content h1 { font-size: 26px; font-weight: 700; margin-bottom: 8px; letter-spacing: -0.5px; }
.brand-content > p { font-size: 15px; color: rgba(255,255,255,.75); margin-bottom: 32px; }
.features { display: flex; flex-direction: column; gap: 12px; }
.feature-item { display: flex; align-items: center; gap: 10px; font-size: 15px; color: rgba(255,255,255,.85); }
.feature-dot { width: 6px; height: 6px; border-radius: 50%; background: #60a5fa; flex-shrink: 0; }
.auth-form-area { flex: 1; display: flex; align-items: center; justify-content: center; padding: 48px 40px; }
.form-wrapper { width: 100%; max-width: 340px; }
.form-wrapper h2 { font-size: 26px; font-weight: 700; color: #111827; margin-bottom: 6px; }
.form-subtitle { font-size: 15px; color: #6b7280; margin-bottom: 32px; }
.input-icon { display: flex; align-items: center; justify-content: center; width: 16px; color: #9ca3af; font-size: 15px; font-weight: 500; }
.submit-btn { width: 100%; height: 44px; font-size: 15px; font-weight: 600; letter-spacing: 1px; border-radius: 8px; transition: all .2s ease; }
.submit-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(37,99,235,.3); }
.form-footer { text-align: center; font-size: 15px; color: #6b7280; }
.link { color: #2563eb; text-decoration: none; font-weight: 500; margin-left: 4px; cursor: pointer; }
.link:hover { color: #1d4ed8; }
@media (max-width: 768px) { .auth-container { flex-direction: column; width: 100%; } .auth-brand { width: 100%; padding: 32px 24px; } .auth-form-area { padding: 32px 24px; } }
</style>
