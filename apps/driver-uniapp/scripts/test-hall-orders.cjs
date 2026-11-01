const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { buildSync } = require('esbuild')
const root = path.resolve(__dirname, '..')

async function main() {
  let locationCode = 200
  let queueReads = 0
  let detailMine = false
  let grabResult = { code: 200, data: true }
  const output = buildSync({
    entryPoints: [path.join(root, 'mock/real-adapter.js')], bundle: true, write: false,
    platform: 'node', format: 'cjs', alias: { '@': root }, define: { 'import.meta.env': '{}' }
  })
  const module = { exports: {} }
  vm.runInNewContext(output.outputFiles[0].text, {
    module, exports: module.exports, console, setTimeout,
    uni: {
      getStorageSync: () => 'test-token',
      request: options => {
        if (options.url.endsWith('/order/robNewOrder/13')) return Promise.resolve({ data: grabResult })
        if (options.url.endsWith('/order/getOrderInfo/13')) {
          return Promise.resolve({ data: { code: 200, data: {
            orderId: 13, status: detailMine ? 2 : 1, mine: detailMine,
            remark: JSON.stringify({ serviceType: 'tow', price: 180, phone: detailMine ? '13000000000' : undefined })
          } } })
        }
        if (options.url.endsWith('/location/updateDriverLocation')) {
          return Promise.resolve({ data: { code: locationCode, message: '未开启接单服务' } })
        }
        assert.ok(options.url.endsWith('/order/findNewOrderQueueData'))
        queueReads++
        return Promise.resolve({ data: { code: 200, data: [
          { orderId: 13, distance: 13.03, startLocation: '同景苑', remark: '{"serviceType":"battery","price":139}' },
          { orderId: 14, distance: 5, startLocation: '附近地点' }
        ] } })
      }
    }
  })
  const send = query => module.exports.default({ method: 'GET', url: '/hall/orders', data: {
    latitude: 31.22, longitude: 121.54, ...query
  } })
  assert.equal((await send({ scope: 'nearby', radius: 10 })).data.length, 1)
  const nearby = await send({ scope: 'nearby', radius: 30 })
  assert.equal(nearby.data.length, 2)
  assert.equal(nearby.data[0].serviceType, 'repair')
  assert.equal(nearby.data[0].price, 139)
  assert.equal((await send({ scope: 'all' })).data.length, 2)
  const previousReads = queueReads
  locationCode = 500
  assert.equal((await send({ scope: 'all' })).msg, '未开启接单服务')
  assert.equal(queueReads, previousReads, '位置上报失败不应读取派单队列')

  const detail = () => module.exports.default({ method: 'GET', url: '/my/orders/13' })
  const preview = (await detail()).data
  assert.equal(preview.myStatus, null)
  assert.equal(preview.poolStatus, 'open')
  assert.equal(preview.customerPhone, '')
  detailMine = true
  const mine = (await detail()).data
  assert.equal(mine.myStatus, 'waiting')
  assert.equal(mine.poolStatus, 'mine')
  assert.equal(mine.customerPhone, '13000000000')
  const grab = () => module.exports.default({ method: 'POST', url: '/hall/orders/13/grab' })
  for (const data of [null, false]) {
    grabResult = { code: 200, data }
    assert.equal((await grab()).code, 217, '下游返回空或 false 不能提示成功')
  }
  grabResult = { code: 217, message: '订单已失效' }
  assert.equal((await grab()).msg, '订单已失效')
  grabResult = { code: 200, data: true }
  detailMine = false
  assert.equal((await grab()).code, 217, '必须确认订单已归属当前司机')
  detailMine = true
  assert.equal((await grab()).data.myStatus, 'waiting')

  const hooks = {}
  const timers = new Map()
  let nextTimer = 0
  let fetches = 0
  const script = fs.readFileSync(path.join(root, 'pages/hall/index.vue'), 'utf8')
    .split('<script setup>')[1].split('</script>')[0].replace(/^\s*import .+$/gm, '')
  vm.runInNewContext(script, {
    ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
    useUserStore: () => ({ ensureLogin: () => Promise.resolve(), fetchInfo: () => Promise.resolve() }),
    onShow: fn => { hooks.show = fn }, onHide: fn => { hooks.hide = fn },
    onUnload: fn => { hooks.unload = fn }, onPullDownRefresh: fn => { hooks.refresh = fn },
    getLocation: () => Promise.resolve({ latitude: 31.22, longitude: 121.54 }),
    listHallOrders: () => { fetches++; return Promise.resolve([]) },
    setInterval: (fn, ms) => { assert.equal(ms, 5000); timers.set(++nextTimer, fn); return nextTimer },
    clearInterval: id => timers.delete(id), setTimeout, uni: {}
  })
  const flush = () => new Promise(resolve => setImmediate(resolve))
  hooks.show()
  await flush()
  assert.equal(fetches, 1)
  assert.equal(timers.size, 1)
  await [...timers.values()][0]()
  assert.equal(fetches, 2, '大厅停留时应自动刷新')
  hooks.hide()
  assert.equal(timers.size, 0, '隐藏页面后停止刷新')
  hooks.show()
  await flush()
  assert.equal(timers.size, 1, '返回大厅仅保留一个定时器')
  hooks.unload()
  assert.equal(timers.size, 0)

  let resolveGrab
  let resolveStale
  let grabCalls = 0
  const detailContext = {
    ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
    onLoad: fn => fn({ id: '13' }), onShow: () => {}, onHide: () => {}, onUnload: () => {},
    getOrder: () => new Promise(resolve => { resolveStale = resolve }),
    grabOrder: () => { grabCalls++; return new Promise(resolve => { resolveGrab = resolve }) },
    uni: { showToast: () => {} }, CODE_AUTH_REQUIRED: 4001,
    departOrder: () => {}, finishOrder: () => {}, setInterval, clearInterval
  }
  const detailScript = fs.readFileSync(path.join(root, 'pages/order/detail.vue'), 'utf8')
    .split('<script setup>')[1].split('</script>')[0].replace(/^\s*import .+$/gm, '')
  vm.runInNewContext(detailScript + '\nglobalThis.testApi = { grab, load, order, grabbing, actions, statusTitle }', detailContext)
  const api = detailContext.testApi
  const oldLoad = api.load()
  const submitting = api.grab()
  api.grab()
  assert.equal(grabCalls, 1, '抢单中应拦截重复点击')
  assert.equal(api.grabbing.value, true)
  resolveGrab({ myStatus: 'waiting', customerPhone: '13000000000' })
  await submitting
  assert.equal(api.order.value.myStatus, 'waiting')
  assert.equal(api.statusTitle.value, '待发车')
  assert.ok(api.actions.value.some(action => action.text === '确认发车'))
  assert.equal(api.grabbing.value, false)
  resolveStale({ myStatus: null, poolStatus: 'open' })
  await oldLoad
  assert.equal(api.order.value.myStatus, 'waiting', '抢单前发出的旧详情请求不能覆盖抢单成功状态')
  console.log('通过：大厅范围/刷新、详情权限映射、抢单失败透传、成功后立即待发车、重复点击拦截、旧请求防覆盖。')
}

main().catch(error => { console.error(error); process.exitCode = 1 })
