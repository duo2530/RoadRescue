// 省级行政区，用于全国订单的出发地/目的地筛选
export const PROVINCES = [
  '北京市', '天津市', '上海市', '重庆市', '河北省', '山西省', '辽宁省', '吉林省', '黑龙江省',
  '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省', '河南省', '湖北省', '湖南省',
  '广东省', '海南省', '四川省', '贵州省', '云南省', '陕西省', '甘肃省', '青海省', '台湾省',
  '内蒙古自治区', '广西壮族自治区', '西藏自治区', '宁夏回族自治区', '新疆维吾尔自治区',
  '香港特别行政区', '澳门特别行政区'
]

// 演示订单用到的地点：label 为卡片上展示的“省-市”（直辖市为“市-区”）
export const PLACES = {
  pudongZhangjiang: { province: '上海市', label: '上海市-浦东新区', address: '上海市浦东新区张江路665号', latitude: 31.2050, longitude: 121.5810 },
  pudongLujiazui: { province: '上海市', label: '上海市-浦东新区', address: '上海市浦东新区高行镇东靖路1588号', latitude: 31.2650, longitude: 121.6250 },
  pudongJinqiao: { province: '上海市', label: '上海市-浦东新区', address: '上海市浦东新区曹路镇川沙路3999号', latitude: 31.2900, longitude: 121.6400 },
  pudongTangzhen: { province: '上海市', label: '上海市-浦东新区', address: '上海市浦东新区唐镇达秀路151号', latitude: 31.2381, longitude: 121.6533 },
  pudongHuamu: { province: '上海市', label: '上海市-浦东新区', address: '上海市浦东新区金科路2889号', latitude: 31.2420, longitude: 121.5800 },
  xuhui: { province: '上海市', label: '上海市-徐汇区', address: '上海市徐汇区漕溪北路88号', latitude: 31.1880, longitude: 121.4365 },
  minhang: { province: '上海市', label: '上海市-闵行区', address: '上海市闵行区莘庄镇沪闵路6088号', latitude: 31.1112, longitude: 121.3817 },
  baoshan: { province: '上海市', label: '上海市-宝山区', address: '上海市宝山区牡丹江路1569号', latitude: 31.4050, longitude: 121.4890 },
  jiading: { province: '上海市', label: '上海市-嘉定区', address: '上海市嘉定区安亭镇墨玉南路888号', latitude: 31.2880, longitude: 121.1700 },
  songjiang: { province: '上海市', label: '上海市-松江区', address: '上海市松江区人民北路2288号', latitude: 31.0320, longitude: 121.2270 },
  wuxi: { province: '江苏省', label: '江苏省-无锡市', address: '江苏省无锡市梁溪区中山路333号', latitude: 31.5747, longitude: 120.2966 },
  suzhou: { province: '江苏省', label: '江苏省-苏州市', address: '江苏省苏州市姑苏区人民路1000号', latitude: 31.2989, longitude: 120.5853 },
  hangzhou: { province: '浙江省', label: '浙江省-杭州市', address: '浙江省杭州市上城区延安路258号', latitude: 30.2741, longitude: 120.1551 },
  ningbo: { province: '浙江省', label: '浙江省-宁波市', address: '浙江省宁波市海曙区中山西路138号', latitude: 29.8683, longitude: 121.5440 },
  hefei: { province: '安徽省', label: '安徽省-合肥市', address: '安徽省合肥市蜀山区长江西路100号', latitude: 31.8206, longitude: 117.2272 },
  meizhou: { province: '广东省', label: '广东省-梅州市', address: '广东省梅州市梅江区彬芳大道118号', latitude: 24.2886, longitude: 116.1226 },
  loudi: { province: '湖南省', label: '湖南省-娄底市', address: '湖南省娄底市娄星区长青中街66号', latitude: 27.7281, longitude: 112.0085 },
  tonghua: { province: '吉林省', label: '吉林省-通化市', address: '吉林省通化市东昌区新华大街1号', latitude: 41.7212, longitude: 125.9365 },
  jinan: { province: '山东省', label: '山东省-济南市', address: '山东省济南市历下区泉城路180号', latitude: 36.6512, longitude: 117.1201 },
  wuhan: { province: '湖北省', label: '湖北省-武汉市', address: '湖北省武汉市江汉区解放大道690号', latitude: 30.5928, longitude: 114.3055 },
  chengdu: { province: '四川省', label: '四川省-成都市', address: '四川省成都市锦江区春熙路1号', latitude: 30.6570, longitude: 104.0665 }
}
