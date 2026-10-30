<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="姓名 / 手机号 / 昵称" clearable style="width: 220px" />
      <el-select v-model="query.status" placeholder="认证状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in AUTH_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" icon="Search" @click="load(1)">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="师傅" min-width="160">
        <template #default="{ row }">
          <div class="who">
            <el-avatar :size="28" :src="row.avatarUrl" />
            <span>{{ row.name || row.nickname }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="driverLicenseClazz" label="准驾车型" width="90" />
      <el-table-column prop="orderCount" label="接单数" width="80" />
      <el-table-column prop="score" label="评分" width="70" />
      <el-table-column label="认证" width="100">
        <template #default="{ row }">
          <el-tag :type="statusOf(AUTH_STATUS, row.authStatus).type">{{ statusOf(AUTH_STATUS, row.authStatus).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
          <el-button link type="success" :disabled="row.authStatus === 2" @click="audit(row, 2)">通过</el-button>
          <el-button link type="danger" :disabled="row.authStatus === -1" @click="audit(row, -1)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total" :page-size="limit" :current-page="page" @current-change="load" />
  </el-card>

  <el-drawer v-model="drawer" title="师傅详情" size="480px">
    <el-descriptions v-if="detail" :column="1" border>
      <el-descriptions-item label="姓名">{{ detail.name || '-' }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="性别">{{ detail.gender === '1' ? '男' : detail.gender === '2' ? '女' : '-' }}</el-descriptions-item>
      <el-descriptions-item label="身份证号">{{ detail.idcardNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="驾驶证号">{{ detail.driverLicenseNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="准驾车型">{{ detail.driverLicenseClazz || '-' }}</el-descriptions-item>
      <el-descriptions-item label="紧急联系人">{{ detail.contactName || '-' }} {{ detail.contactPhone || '' }}</el-descriptions-item>
      <el-descriptions-item label="工号">{{ detail.jobNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="认证状态">{{ statusOf(AUTH_STATUS, detail.authStatus).label }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { findDriverPage, getDriver, updateDriverAuth } from '../api'
import { AUTH_STATUS, statusOf } from '../utils/dict'

const query = reactive({ keyword: '', status: undefined })
const list = ref([])
const total = ref(0)
const page = ref(1)
const limit = 10
const loading = ref(false)
const drawer = ref(false)
const detail = ref(null)

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const res = await findDriverPage(p, limit, query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function showDetail(id) {
  detail.value = await getDriver(id)
  drawer.value = true
}

async function audit(row, authStatus) {
  await ElMessageBox.confirm(`确认${authStatus === 2 ? '通过' : '驳回'}「${row.name || row.nickname}」的认证？`, '认证审核')
  await updateDriverAuth(row.id, authStatus)
  ElMessage.success('已更新')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.who { display: flex; align-items: center; gap: 8px; }
</style>
