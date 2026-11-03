<template>
  <div class="login-page">
    <div class="panel">
      <div class="intro">
        <h1>道路救援</h1>
        <p>道路救援调度管理平台</p>
        <ul>
          <li>救援工单全流程跟踪</li>
          <li>救援师傅认证审核</li>
          <li>用户与权限管理</li>
        </ul>
      </div>
      <el-card class="form-card" shadow="never">
        <h2>管理员登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码" prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">登 录</el-button>
        </el-form>
        <p class="tip">演示账号：admin / 111111</p>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'
import { TOKEN_KEY } from '../utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '111111' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    localStorage.setItem(TOKEN_KEY, data.token)
    router.replace('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #0f2a4a 0%, #1d4f86 60%, #f56c2d 140%); }
.panel { display: flex; width: 860px; background: rgba(255, 255, 255, 0.06); border-radius: 12px; overflow: hidden; }
.intro { flex: 1; color: #fff; padding: 48px; }
.intro h1 { margin: 0; font-size: 36px; letter-spacing: 4px; }
.intro p { opacity: 0.85; margin: 8px 0 24px; }
.intro li { line-height: 2; opacity: 0.9; }
.form-card { width: 360px; border: none; border-radius: 0; padding: 24px 12px; }
.form-card h2 { margin: 0 0 24px; font-size: 20px; }
.tip { color: #909399; font-size: 12px; margin-top: 16px; }
</style>
