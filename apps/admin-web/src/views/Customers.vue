<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="昵称 / 手机号" clearable style="width: 220px" />
      <el-button type="primary" icon="Search" @click="load(1)">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="用户" min-width="160">
        <template #default="{ row }">
          <div class="who">
            <el-avatar :size="28" :src="row.avatarUrl" />
            <span>{{ row.nickname }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">{{ row.gender === '1' ? '男' : row.gender === '2' ? '女' : '-' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="v => toggle(row, v)" />
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total" :page-size="limit" :current-page="page" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { findCustomerPage, updateCustomerStatus } from '../api'

const query = reactive({ keyword: '' })
const list = ref([])
const total = ref(0)
const page = ref(1)
const limit = 10
const loading = ref(false)

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const res = await findCustomerPage(p, limit, query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function toggle(row, enabled) {
  await updateCustomerStatus(row.id, enabled ? 1 : 2)
  row.status = enabled ? 1 : 2
  ElMessage.success(enabled ? '已启用' : '已禁用')
}

onMounted(() => load(1))
</script>

<style scoped>
.who { display: flex; align-items: center; gap: 8px; }
</style>
