const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { buildSync } = require('esbuild')

const root = path.resolve(__dirname, '..')
const state = { requests: [], writes: 0, storage: {} }
const uni = {
  getStorageSync: key => state.storage[key],
  setStorageSync: (key, value) => { state.storage[key] = value; state.writes++ },
  request: options => {
    state.requests.push(options)
    return Promise.resolve({ data: { code: 200, data: 123 } })
  }
}

function load(file) {
  const result = buildSync({
    entryPoints: [path.join(root, file)], bundle: true, write: false,
    platform: 'node', format: 'cjs', alias: { '@': root },
    define: { 'import.meta.env': '{}' }
  })
  const module = { exports: {} }
  vm.runInNewContext(result.outputFiles[0].text, {
    module, exports: module.exports, uni, setTimeout: fn => fn(), console
  })
  return module.exports
}

async function main() {
  const { validateOrder } = load('utils/order-validation.js')
  const realRequest = load('mock/real-adapter.js').default
  const mockRequest = load('mock/index.js').default
  const address = { name: '同景苑', latitude: 31.23, longitude: 121.65 }
  const valid = {
    serviceType: 'tow', startAddress: address, endAddress: address,
    carType: '9座以下（含9座）客车', position: '地面', plate: '沪B8K6T9', phone: '13000000000', distance: 15
  }
  const invalidCases = [
    [{ plate: '', phone: '' }, '请选择车牌号'],
    [{ plate: ' ' }, '请选择车牌号'],
    [{ phone: '' }, '请输入联系电话'],
    [{ phone: '123' }, '请输入正确的联系电话'],
    [{ plate: 'ABC' }, '请输入正确的车牌号'],
    [{ startAddress: null }, '请选择救援地点'],
    [{ startAddress: { ...address, latitude: 91 } }, '请选择救援地点'],
    [{ endAddress: null }, '请选择拖车终点'],
    [{ carType: '' }, '请选择车型'],
    [{ position: '' }, '请选择车辆当前位置']
  ]
  // 直接执行页面提交函数，确认必填校验失败时没有调用创建订单。
  const page = fs.readFileSync(path.join(root, 'pages/index.vue'), 'utf8')
  const submitSource = page.match(/function submit\(\) \{[\s\S]*?(?=\r?\n<\/script>)/)[0]
  const pageSubmit = new Function('submitting', 'current', 'startAddress', 'endAddress',
    'distance', 'form', 'validateOrder', 'uni', 'createOrder', `${submitSource}; return submit()`)
  for (const [patch, message] of invalidCases) {
    const data = { ...valid, ...patch }
    assert.equal(validateOrder(data, { needDestination: true }), message)
    let toast
    const submitting = { value: false }
    pageSubmit(submitting, { value: { type: 'tow', needDestination: true } },
      { value: data.startAddress }, { value: data.endAddress }, { value: 15 }, data,
      validateOrder, { showToast: options => { toast = options.title } },
      () => assert.fail('不完整表单不应调用创建订单'))
    assert.equal(toast, message)
    assert.equal(submitting.value, false)
    for (const send of [realRequest, mockRequest]) {
      const requests = state.requests.length
      const writes = state.writes
      const response = await send({ method: 'POST', url: '/order', data })
      assert.notEqual(response.code, 200)
      assert.equal(response.msg, message)
      assert.equal(state.requests.length, requests, '不应发送真实下单请求')
      assert.equal(state.writes, writes, '不应保存订单或消耗优惠券')
    }
  }
  for (const serviceType of ['tow', 'battery', 'tire', 'charge']) {
    const data = { ...valid, serviceType, plate: '沪AD12345', endAddress: serviceType === 'tow' ? address : null }
    assert.equal(validateOrder(data, { needDestination: serviceType === 'tow' }), '')
    const response = await realRequest({ method: 'POST', url: '/order', data })
    assert.equal(response.code, 200)
    const request = state.requests.at(-1)
    assert.equal(request.url, '/customer-api/order/submitOrder')
    assert.equal(JSON.parse(request.data.remark).phone, valid.phone)
    assert.equal(JSON.parse(request.data.remark).plate, data.plate)
  }
  const response = await mockRequest({ method: 'POST', url: '/order', data: valid })
  assert.equal(response.code, 200)
  assert.equal(response.data.plate, valid.plate)
  console.log('通过：10 组缺项/格式错误的页面、Mock、真实适配层校验；4 种服务正常提交；Mock 正常创建订单。')
}

main().catch(error => { console.error(error); process.exitCode = 1 })
