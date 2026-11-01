const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { buildSync } = require('esbuild')
const root = path.resolve(__dirname, '..')

async function main() {
  const states = [1, 2, 5, 6, 7, 8, -1]
  const bundled = buildSync({ entryPoints: [path.join(root, 'mock/real-adapter.js')], bundle: true,
    write: false, platform: 'node', format: 'cjs', alias: { '@': root }, define: { 'import.meta.env': '{}' } })
  const module = { exports: {} }
  const requests = []
  vm.runInNewContext(bundled.outputFiles[0].text, { module, exports: module.exports, setTimeout,
    uni: { getStorageSync: () => 'token', request: options => {
      requests.push(options)
      if (options.url.includes('findCustomerOrderPage')) return Promise.resolve({ data: { code: 200, data: {
        records: states.map((status, i) => ({ id: i + 1, status, amount: 180, remark: '{"price":180}' }))
      } } })
      if (options.url.includes('/payment/create')) return Promise.resolve({ data: { code: 503, message: '沙箱配置缺失' } })
      return Promise.resolve({ data: { code: 200, data: false } })
    } }
  })
  const send = module.exports.default
  const ongoing = await send({ url: '/order/list?type=ongoing', method: 'GET' })
  assert.equal(ongoing.data.length, 5)
  assert.equal(ongoing.data[3].status, 'settling')
  assert.equal(ongoing.data[4].status, 'unpaid')
  const history = await send({ url: '/order/list?type=history', method: 'GET' })
  assert.equal(history.data.length, 2)
  assert.equal((await send({ url: '/payment/create', method: 'POST', data: { orderNo: 'order16', channel: 'ALIPAY_WAP_SANDBOX' } })).code, 503)
  assert.ok(requests.at(-1).url.endsWith('/order/payment/create'))
  assert.equal((await send({ url: '/payment/status/order16', method: 'GET' })).data, false)

  const source = fs.readFileSync(path.join(root, 'pages/order/detail.vue'), 'utf8')
    .split('<script setup>')[1].split('</script>')[0].replace(/^\s*import .+$/gm, '')
  async function page(mini = false) {
    const server = { id: '16', orderNo: 'order16', status: 'unpaid', payAmount: 180 }
    let paid = false
    let creates = 0
    let queries = 0
    let redirect
    const storage = {}
    const hooks = {}
    const context = {
      ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
      onLoad: fn => fn({ id: '16', paymentReturn: '1', paid: 'true' }),
      onShow: fn => { hooks.show = fn }, onHide: () => {}, onUnload: () => {},
      config: { appInfo: { servicePhone: '021-66898194' } }, STATUS_STEPS: ['waiting', 'dispatched', 'rescuing', 'unpaid', 'finished'],
      getOrder: async () => ({ ...server }), cancelOrder: async () => {},
      createPayment: async data => { creates++; assert.equal(data.channel, 'ALIPAY_WAP_SANDBOX'); assert.equal(Object.hasOwn(data, 'amount'), false); return { redirectUrl: 'https://openapi-sandbox.dl.alipaydev.com/gateway.do?signed=1' } },
      queryPayment: async () => { queries++; return paid },
      uni: { showToast: () => {}, getStorageSync: key => storage[key], setStorageSync: (key, value) => { storage[key] = value }, removeStorageSync: key => delete storage[key] },
      window: { location: { assign: url => { redirect = url } } }, URL,
      setInterval: () => 1, clearInterval: () => {}
    }
    const script = mini ? source.replace(/\/\/ #ifdef H5[\s\S]*?\/\/ #endif/g, '') : source
    vm.runInNewContext(script + '\nglobalThis.api = { load, pay, checkPayment, canPay, order }', context)
    await context.api.load()
    assert.equal(context.api.canPay.value, true)
    hooks.show()
    await new Promise(resolve => setImmediate(resolve))
    if (!mini) {
      assert.ok(queries > 0, '回跳必须由服务端核验支付结果')
      assert.equal(context.api.order.value.status, 'unpaid', '回跳参数不能把订单直接改成已付款')
      await context.api.pay()
      assert.ok(redirect.startsWith('https://openapi-sandbox.dl.alipaydev.com/'))
      assert.equal(creates, 1)
      await context.api.checkPayment(true)
      assert.equal(context.api.order.value.status, 'unpaid')
      paid = true; server.status = 'finished'
      await context.api.checkPayment(true)
      assert.equal(context.api.order.value.status, 'finished')
      assert.equal(storage.zd_pending_payment, undefined)
    } else {
      await context.api.pay()
      assert.equal(creates, 0, '小程序微信入口预留但不发起支付')
      assert.equal(redirect, undefined)
    }
  }
  await page()
  await page(true)
  console.log('通过：待结算/待付款/已付款映射、支付错误透传、H5 沙箱跳转、回跳服务端核验、未支付保留状态、微信入口不启用。')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
