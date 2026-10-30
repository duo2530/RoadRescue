<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="工单号" clearable style="width: 200px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in ORDER_STATUS" :key="k" :label="v.label" :value="Number(k)" />
      </el-select>
      <el-button type="primary" icon="Search" @click="load(1)">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="工单号" min-width="160" />
      <el-table-column prop="startLocation" label="救援地点" min-width="180" show-overflow-tooltip />
      <el-table-column prop="endLocation" label="目的地" min-width="180" show-overflow-tooltip />
      <el-table-column prop="expectAmount" label="预估(元)" width="100" />
      <el-table-column prop="realAmount" label="实收(元)" width="100" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusOf(ORDER_STATUS, row.status).type">{{ statusOf(ORDER_STATUS, row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total" :page-size="limit" :current-page="page" @current-change="load" />
  </el-card>

  <el-drawer v-model="drawer" title="工单详情" size="480px">
    <el-descriptions v-if="detail" :column="1" border>
      <el-descriptions-item label="工单号">{{ detail.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusOf(ORDER_STATUS, detail.status).label }}</el-descriptions-item>
      <el-descriptions-item label="用户ID">{{ detail.customerId }}</el-descriptions-item>
      <el-descriptions-item label="师傅ID">{{ detail.driverId || '未派单' }}</el-descriptions-item>
      <el-descriptions-item label="救援地点">{{ detail.startLocation }}</el-descriptions-item>
      <el-descriptions-item label="目的地">{{ detail.endLocation }}</el-descriptions-item>
      <el-descriptions-item label="预估里程(km)">{{ detail.expectDistance }}</el-descriptions-item>
      <el-descriptions-item label="实际里程(km)">{{ detail.realDistance ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="预估金额">{{ detail.expectAmount }}</el-descriptions-item>
      <el-descriptions-item label="实收金额">{{ detail.realAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="车牌">{{ detail.carLicense || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
      <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="开始服务">{{ detail.startServiceTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="结束服务">{{ detail.endServiceTime || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { findOrderPage, getOrder } from '../api'
import { ORDER_STATUS, statusOf } from '../utils/dict'

const query = reactive({ orderNo: '', status: undefined })
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
    const res = await findOrderPage(p, limit, query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function showDetail(id) {
  detail.value = await getOrder(id)
  drawer.value = true
}

onMounted(() => load(1))
</script>
