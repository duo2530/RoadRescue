import request from '../utils/request'

// 登录与当前用户
export const login = data => request.post('/securityLogin/login', data)
export const getUserInfo = () => request.get('/securityLogin/getUserInfo')

// 看板
export const getDashboard = () => request.get('/order/info/dashboard')

// 订单（救援工单）
export const findOrderPage = (page, limit, query = {}) => request.post(`/order/info/${page}/${limit}`, query)
export const getOrder = id => request.get(`/order/info/getById/${id}`)

// 司机（救援师傅）
export const findDriverPage = (page, limit, query = {}) => request.post(`/driver/info/${page}/${limit}`, query)
export const getDriver = id => request.get(`/driver/info/getById/${id}`)
export const updateDriverAuth = (id, authStatus) => request.get(`/driver/info/updateAuthStatus/${id}/${authStatus}`)

// 乘客（求助用户）
export const findCustomerPage = (page, limit, query = {}) => request.post(`/customer/info/${page}/${limit}`, query)
export const updateCustomerStatus = (id, status) => request.get(`/customer/info/updateStatus/${id}/${status}`)

// 系统：用户 / 角色 / 菜单
export const findUserPage = (page, limit, query = {}) => request.post(`/sysUser/${page}/${limit}`, query)
export const saveUser = data => request.post('/sysUser/save', data)
export const updateUser = data => request.put('/sysUser/update', data)
export const removeUser = id => request.delete(`/sysUser/remove/${id}`)
export const updateUserStatus = (id, status) => request.get(`/sysUser/updateStatus/${id}/${status}`)
export const toAssignRole = userId => request.get(`/sysRole/toAssign/${userId}`)
export const doAssignRole = data => request.post('/sysRole/doAssign', data)

export const findRolePage = (page, limit, query = {}) => request.post(`/sysRole/${page}/${limit}`, query)
export const saveRole = data => request.post('/sysRole/save', data)
export const updateRole = data => request.put('/sysRole/update', data)
export const removeRole = id => request.delete(`/sysRole/remove/${id}`)

export const findMenuNodes = () => request.get('/sysMenu/findNodes')
