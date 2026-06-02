<template>
  <div class="trip-wrapper">
    <div class="trip-container">
      <div class="page-header">
        <h2 class="page-title">出差申请</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新增出差</span>
        </button>
      </div>

      <div class="tabs">
        <button class="tab-item" :class="{ active: activeTab === 'my' }" @click="activeTab = 'my'; loadData()">
          <span class="tab-indicator"></span>我提交的
        </button>
        <button class="tab-item" :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'; loadData()">
          <span class="tab-indicator"></span>待我审批
        </button>
      </div>

      <div class="table-wrapper">
        <OaTable
          :data="tableData"
          :columns="columns"
          :total="total"
          :page="query.page"
          :size="query.size"
          @update:page="p => { query.page = p; loadData() }"
          @update:size="s => { query.size = s; query.page = 1; loadData() }"
        >
          <template #reason="{ row }">
            <span class="reason-cell">{{ row.reason }}</span>
          </template>
          <template #status="{ row }">
            <OaStatusBadge
              :type="getBadgeType(row.status)"
              :text="row.statusName || getStatusText(row.status)"
            />
          </template>
          <template #actions="{ row }">
            <OaButton variant="ghost" size="small" @click="handleView(row)">查看</OaButton>
            <OaButton
              v-if="row.status === 'PENDING' && activeTab === 'my'"
              variant="danger"
              size="small"
              @click="handleCancel(row)"
            >
              撤回
            </OaButton>
            <OaButton
              v-if="activeTab === 'pending'"
              variant="primary"
              size="small"
              @click="handleApprove(row)"
            >
              审批
            </OaButton>
          </template>
        </OaTable>
      </div>
    </div>

    <!-- 出差表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog dialog-xl">
        <div class="dialog-header">
          <h3 class="dialog-title">新增出差</h3>
          <button class="dialog-close" @click="dialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label">出差事由</label>
            <textarea class="form-textarea" v-model="form.reason" placeholder="请输入出差事由" rows="2"></textarea>
          </div>

          <!-- 行程列表 -->
          <div class="itinerary-section">
            <div class="section-title">行程信息</div>
            <div v-for="(item, index) in form.itineraries" :key="index" class="itinerary-item">
              <div class="itinerary-header">
                <span class="itinerary-label">行程{{ index + 1 }}</span>
                <button v-if="index > 0" class="remove-btn" @click="removeItinerary(index)">删除</button>
              </div>
              <div class="itinerary-row">
                <div class="form-item">
                  <label class="form-label">交通工具</label>
                  <select class="form-input" v-model="item.transport">
                    <option value="">请选择</option>
                    <option value="PLANE">飞机</option>
                    <option value="TRAIN">火车</option>
                    <option value="BUS">汽车</option>
                    <option value="OTHER">其他</option>
                  </select>
                </div>
                <div class="form-item">
                  <label class="form-label">单程/往返</label>
                  <select class="form-input" v-model="item.tripType">
                    <option value="ONE_WAY">单程</option>
                    <option value="ROUND">往返</option>
                  </select>
                </div>
              </div>
              <div class="itinerary-row">
                <div class="form-item">
                  <label class="form-label">出发城市</label>
                  <div class="city-select-group">
                    <select class="form-input" v-model="item.departureProvince" @change="onDepartureProvinceChange(index)">
                      <option value="">省</option>
                      <option v-for="p in provinceList" :key="p" :value="p">{{ p }}</option>
                    </select>
                    <select class="form-input" v-model="item.departure" :disabled="isDirectCity(item.departureProvince) && !item.departureProvince">
                      <option value="">{{ isDirectCity(item.departureProvince) ? '市' : '市' }}</option>
                      <option v-for="c in getCityList(item.departureProvince)" :key="c" :value="isDirectCity(item.departureProvince) ? c : c">{{ c }}</option>
                    </select>
                  </div>
                </div>
                <div class="form-item">
                  <label class="form-label">目的城市</label>
                  <div class="city-select-group">
                    <select class="form-input" v-model="item.destinationProvince" @change="onDestinationProvinceChange(index)">
                      <option value="">省</option>
                      <option v-for="p in provinceList" :key="p" :value="p">{{ p }}</option>
                    </select>
                    <select class="form-input" v-model="item.destination" :disabled="isDirectCity(item.destinationProvince) && !item.destinationProvince">
                      <option value="">市</option>
                      <option v-for="c in getCityList(item.destinationProvince)" :key="c" :value="c">{{ c }}</option>
                    </select>
                  </div>
                </div>
              </div>
              <div class="itinerary-row">
                <div class="form-item">
                  <label class="form-label">开始时间</label>
                  <input type="datetime-local" class="form-input" v-model="item.startTime" @change="calcItineraryDays(item)" />
                </div>
                <div class="form-item">
                  <label class="form-label">结束时间</label>
                  <input type="datetime-local" class="form-input" v-model="item.endTime" @change="calcItineraryDays(item)" />
                </div>
                <div class="form-item">
                  <label class="form-label">时长(天)</label>
                  <input type="number" class="form-input readonly" :value="item.days" readonly step="0.5" min="0.5" />
                </div>
              </div>
            </div>
            <button class="add-itinerary-btn" @click="addItinerary">
              <span>+</span> 增加行程
            </button>
          </div>

          <div class="form-item">
            <label class="form-label">出行人（同行人）</label>
            <input type="text" class="form-input" v-model="form.travelers" placeholder="请输入同行人姓名，多人以逗号分隔" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" @click="handleSubmit">提交</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog dialog-xl">
        <div class="dialog-header">
          <h3 class="dialog-title">出差详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">出差单号</span>
              <span class="detail-value mono">{{ currentDetail.tripNo || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请日期</span>
              <span class="detail-value">{{ currentDetail.applyDate || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请人</span>
              <span class="detail-value">{{ currentDetail.userName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请部门</span>
              <span class="detail-value">{{ currentDetail.deptName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">出行人</span>
              <span class="detail-value">{{ currentDetail.travelers || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">总时长</span>
              <span class="detail-value highlight">{{ currentDetail.totalDays || 0 }} 天</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态</span>
              <span class="detail-value">
                <span class="status-badge" :class="getStatusClass(currentDetail.status)">
                  <span class="status-dot"></span>
                  {{ getStatusText(currentDetail.status) }}
                </span>
              </span>
            </div>
            <div class="detail-item full-width">
              <span class="detail-label">出差事由</span>
              <span class="detail-value reason">{{ currentDetail.reason || '-' }}</span>
            </div>
          </div>

          <!-- 行程详情 -->
          <div class="itinerary-detail" v-if="currentDetail.itineraries && currentDetail.itineraries.length">
            <div class="section-title">行程详情</div>
            <div v-for="(item, index) in currentDetail.itineraries" :key="index" class="itinerary-row-detail">
              <div class="row-header">行程{{ index + 1 }}</div>
              <div class="row-info">
                <span>交通工具：{{ getTransportName(item.transport) }}</span>
                <span>{{ item.tripType === 'ONE_WAY' ? '单程' : '往返' }}</span>
              </div>
              <div class="row-info">
                <span>{{ item.departure }} → {{ item.destination }}</span>
              </div>
              <div class="row-info">
                <span>{{ item.startTime }} - {{ item.endTime }}</span>
                <span class="highlight">{{ item.days }} 天</span>
              </div>
            </div>
          </div>

          <!-- 审批流程图（V2.0 接入 State Machine） -->
          <OaApprovalCard
            v-if="currentDetail && currentDetail.id"
            title="审批流程"
            business-type="BUSINESS_TRIP"
            :business-id="currentDetail.id"
            class="detail-flow-card"
          />
        </div>
        <div class="dialog-footer" v-if="activeTab === 'pending' && currentDetail.status === 'PENDING'">
          <button class="cyber-btn" @click="detailVisible = false">关闭</button>
          <button class="cyber-btn danger" @click="handleReject">拒绝</button>
          <button class="cyber-btn primary" @click="handleApproveConfirm">通过</button>
        </div>
        <div class="dialog-footer" v-else>
          <button class="cyber-btn primary" @click="detailVisible = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { hrApi } from '@/api/hr'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('my')
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const currentDetail = ref({})

// 表格列定义
const columns = [
  { prop: 'tripNo', label: '出差单号', width: 160 },
  { prop: 'startDate', label: '申请日期', width: 220, formatter: (val, row) => `${row.startDate || ''} ~ ${row.endDate || ''}` },
  { prop: 'reason', label: '出差事由', minWidth: 200 },
  { prop: 'tripCount', label: '行程数', width: 80 },
  { prop: 'days', label: '总时长(天)', width: 110 },
  { prop: 'companions', label: '出行人', minWidth: 120 },
  { prop: 'status', label: '状态', width: 110 }
]

// 状态 -> OaStatusBadge type
const getBadgeType = (status) => ({
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info'
}[status] || 'default')

const transportMap = { PLANE: '飞机', TRAIN: '火车', BUS: '汽车', OTHER: '其他' }

// 省市数据
const DIRECT_CITIES = ['北京市', '上海市', '天津市', '重庆市']
const provinceList = ['北京市', '上海市', '天津市', '重庆市', '广东省', '浙江省', '江苏省', '四川省', '湖北省', '湖南省', '河南省', '河北省', '山东省', '福建省', '安徽省', '江西省', '陕西省', '辽宁省', '吉林省', '黑龙江省', '云南省', '贵州省', '甘肃省', '海南省', '内蒙古', '广西', '宁夏', '青海省', '新疆', '西藏']
const cityMap = {
  '广东省': ['广州市', '深圳市', '佛山市', '东莞市', '珠海市', '中山市', '惠州市', '汕头市', '湛江市', '江门市', '茂名市', '肇庆市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '韶关市', '揭阳市', '潮州市', '云浮市'],
  '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市', '金华市'],
  '江苏省': ['南京市', '苏州市', '无锡市', '常州市', '南通市', '徐州市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市'],
  '四川省': ['成都市', '绵阳市', '德阳市', '南充市', '宜宾市', '自贡市', '攀枝花市', '泸州市', '广元市', '遂宁市', '内江市', '乐山市', '资阳市', '眉山市', '雅安市', '广安市', '达州市', '巴中市', '雅安市'],
  '湖北省': ['武汉市', '宜昌市', '襄阳市', '荆州市', '黄石市', '十堰市', '孝感市', '荆门市', '鄂州市', '黄冈市', '咸宁市', '随州市', '恩施市'],
  '湖南省': ['长沙市', '株洲市', '湘潭市', '衡阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西市'],
  '山东省': ['济南市', '青岛市', '烟台市', '威海市', '潍坊市', '淄博市', '临沂市', '济宁市', '泰安市', '德州市', '聊城市', '滨州市', '菏泽市', '枣庄市', '日照市', '东营市'],
  '福建省': ['福州市', '厦门市', '泉州市', '漳州市', '莆田市', '三明市', '龙岩市', '南平市', '宁德市'],
  '河南省': ['郑州市', '洛阳市', '开封市', '南阳市', '新乡市', '安阳市', '焦作市', '许昌市', '漯河市', '三门峡市', '商丘市', '周口市', '驻马店市', '信阳市', '平顶山市', '濮阳市', '鹤壁市'],
  '河北省': ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市'],
  '安徽省': ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '六安市', '亳州市', '池州市', '宣城市'],
  '江西省': ['南昌市', '景德镇市', '九江市', '赣州市', '吉安市', '宜春市', '抚州市', '上饶市', '鹰潭市', '新余市', '萍乡市'],
  '陕西省': ['西安市', '宝鸡市', '咸阳市', '铜川市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市'],
  '辽宁省': ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市'],
  '吉林省': ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延吉市'],
  '黑龙江省': ['哈尔滨市', '齐齐哈尔市', '鸡西市', '鹤岗市', '双鸭山市', '大庆市', '伊春市', '佳木斯市', '七台河市', '牡丹江市', '黑河市', '绥化市'],
  '云南省': ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄市', '红河市', '文山市', '西双版纳市', '大理市', '德宏市', '怒江市', '迪庆市'],
  '贵州省': ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南市', '黔东南市', '黔南市'],
  '甘肃省': ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏市', '甘南市'],
  '海南省': ['海口市', '三亚市', '三沙市', '儋州市'],
  '内蒙古': ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟'],
  '广西': ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市'],
  '宁夏': ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市'],
  '青海省': ['西宁市', '海东市', '海北市', '黄南市', '海南市', '果洛市', '玉树市', '海西市'],
  '新疆': ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉市', '博尔塔拉市', '巴音郭楞市', '阿克苏市', '克孜勒苏市', '喀什市', '和田市', '伊犁市', '塔城市', '阿勒泰市'],
  '西藏': ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区']
}

const getCityList = (province) => cityMap[province] || []
const isDirectCity = (province) => DIRECT_CITIES.includes(province)

const onDepartureProvinceChange = (index) => {
  const item = form.itineraries[index]
  if (isDirectCity(item.departureProvince)) {
    item.departure = item.departureProvince
  } else {
    item.departure = ''
  }
}

const onDestinationProvinceChange = (index) => {
  const item = form.itineraries[index]
  if (isDirectCity(item.destinationProvince)) {
    item.destination = item.destinationProvince
  } else {
    item.destination = ''
  }
}

const defaultItinerary = () => ({
  transport: '',
  tripType: 'ONE_WAY',
  departureProvince: '',
  departure: '',
  destinationProvince: '',
  destination: '',
  startTime: '',
  endTime: '',
  days: 0
})

const form = reactive({
  reason: '',
  itineraries: [defaultItinerary()],
  travelers: ''
})

let submitLock = false
const withDebounce = async (fn, delay = 500) => {
  if (submitLock) { ElMessage.info('操作过于频繁，请稍后'); return true }
  submitLock = true
  try { return await fn() } finally { setTimeout(() => { submitLock = false }, delay) }
}

const getTypeClass = (type) => ({ LOCAL: 'local', SHORT: 'short', LONG: 'long' }[type] || '')
const getStatusClass = (status) => ({ PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' }[status] || '')
const getStatusText = (status) => ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status)
const getTransportName = (type) => transportMap[type] || type || '-'

// HTML转义函数，防止XSS攻击
const escapeHtml = (str) => {
  if (!str) return '-'
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

// 上班时间配置（单位：小时）
const WORK_CONFIG = {
  morningStart: 9,    // 上午上班时间
  morningEnd: 12,     // 上午下班时间
  afternoonStart: 13, // 下午上班时间
  afternoonEnd: 18,   // 下午下班时间
  dailyHours: 8       // 每日标准工作时长
}

// 计算单日工作时数
const calcDailyWorkHours = (date, startHour, endHour) => {
  const dateStr = date.toDateString()
  let workMinutes = 0

  // 上午段
  const morningStartMin = 9 * 60
  const morningEndMin = 12 * 60
  const amStart = Math.max(startHour, morningStartMin)
  const amEnd = Math.min(endHour, morningEndMin)
  if (amEnd > amStart) workMinutes += amEnd - amStart

  // 下午段
  const afternoonStartMin = 13 * 60
  const afternoonEndMin = 18 * 60
  const pmStart = Math.max(startHour, afternoonStartMin)
  const pmEnd = Math.min(endHour, afternoonEndMin)
  if (pmEnd > pmStart) workMinutes += pmEnd - pmStart

  return workMinutes / 60
}

// 计算行程天数（按上班时间计算）
const calcItineraryDays = (item) => {
  if (item.startTime && item.endTime) {
    const start = new Date(item.startTime)
    const end = new Date(item.endTime)

    // 如果是同一天
    if (start.toDateString() === end.toDateString()) {
      const startHour = start.getHours() * 60 + start.getMinutes()
      const endHour = end.getHours() * 60 + end.getMinutes()
      const hours = calcDailyWorkHours(start, startHour, endHour)
      item.days = Math.max(0.5, Math.round(hours / WORK_CONFIG.dailyHours * 2) / 2)
      return
    }

    // 跨天计算
    let totalWorkHours = 0
    const current = new Date(start)

    // 第一天：从开始时间到当天18:00
    const firstDayStartHour = start.getHours() * 60 + start.getMinutes()
    const firstDayEndHour = 18 * 60
    totalWorkHours += calcDailyWorkHours(start, firstDayStartHour, firstDayEndHour)

    // 中间完整天数
    current.setDate(current.getDate() + 1)
    while (current.toDateString() !== end.toDateString()) {
      totalWorkHours += WORK_CONFIG.dailyHours
      current.setDate(current.getDate() + 1)
    }

    // 最后一天：从当天9:00到结束时间
    const lastDayStartHour = 9 * 60
    const lastDayEndHour = end.getHours() * 60 + end.getMinutes()
    totalWorkHours += calcDailyWorkHours(end, lastDayStartHour, lastDayEndHour)

    item.days = Math.max(0.5, Math.round(totalWorkHours / WORK_CONFIG.dailyHours * 2) / 2)
  }
}

const addItinerary = () => {
  form.itineraries.push(defaultItinerary())
}

const removeItinerary = (index) => {
  form.itineraries.splice(index, 1)
}

const loadData = async () => {
  console.log('[出差] 开始加载')
  try {
    const res = activeTab.value === 'my' ? await hrApi.getBusinessTripList(query) : await workflowApi.getMyTasks(query)
    console.log('[出差] res.data:', res.data)
    const data = res.data?.data
    tableData.value = data?.records || res.data?.records || []
    total.value = data?.total || tableData.value.length
  } catch (error) {
    console.error('[出差] 加载失败', error)
  }
}

const handleAdd = () => {
  Object.assign(form, { reason: '', itineraries: [defaultItinerary()], travelers: '' })
  dialogVisible.value = true
}

const handleView = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const handleApprove = (row) => {
  currentDetail.value = row
  ElMessageBox.confirm(
    `<div style="text-align:left">
      <p><strong>出差事由：</strong>${escapeHtml(row.reason)}</p>
      <p><strong>行程数：</strong>${escapeHtml(row.itineraryCount || 1)}</p>
      <p><strong>出行人：</strong>${escapeHtml(row.travelers)}</p>
    </div>`,
    '确认通过出差申请？',
    { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    await withDebounce(async () => {
      await hrApi.approveBusinessTrip(row.id, { approveResult: 'APPROVE', comment: '同意' })
      ElMessage.success('审批成功')
      loadData()
    })
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await withDebounce(async () => {
    await hrApi.approveBusinessTrip(currentDetail.value.id, { approveResult: 'APPROVE', comment: '同意' })
    ElMessage.success('审批成功')
    detailVisible.value = false
    loadData()
  })
}

const handleReject = async () => {
  const { value: comment } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
    confirmButtonText: '确认', cancelButtonText: '取消',
    inputPattern: /\S+/, inputErrorMessage: '请输入拒绝原因'
  })
  await withDebounce(async () => {
    await hrApi.approveBusinessTrip(currentDetail.value.id, { approveResult: 'REJECT', comment })
    ElMessage.success('已拒绝')
    detailVisible.value = false
    loadData()
  })
}

const handleCancel = async (row) => {
  if (row.status !== 'PENDING') { ElMessage.warning('只能撤回待审批状态的申请'); return }
  await ElMessageBox.confirm('确定要撤回该出差申请吗？', '提示', { type: 'warning' })
  await withDebounce(async () => {
    await hrApi.cancelBusinessTrip(row.id)
    ElMessage.success('撤回成功')
    loadData()
  })
}

const handleSubmit = async () => {
  if (!form.reason.trim()) { ElMessage.warning('请输入出差事由'); return }
  if (!form.travelers.trim()) { ElMessage.warning('请输入出行人'); return }

  // 验证行程数据
  const firstTrip = form.itineraries[0]
  if (!firstTrip.departure) { ElMessage.warning('请选择出发城市'); return }
  if (!firstTrip.destination) { ElMessage.warning('请选择目的城市'); return }
  if (!firstTrip.startTime) { ElMessage.warning('请选择开始时间'); return }
  if (!firstTrip.endTime) { ElMessage.warning('请选择结束时间'); return }

  await withDebounce(async () => {
    try {
      // 转换数据格式以适配后端接口
      const startDate = new Date(firstTrip.startTime)
      const endDate = new Date(firstTrip.endTime)

      const submitData = {
        destination: firstTrip.destination,
        tripType: firstTrip.transport || 'OTHER',
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0],
        reason: form.reason,
        companions: form.travelers,
        itineraries: form.itineraries.map(item => ({
          transport: item.transport,
          tripType: item.tripType,
          departure: item.departure,
          destination: item.destination,
          startTime: item.startTime,
          endTime: item.endTime
        }))
      }

      await hrApi.createBusinessTrip(submitData)
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('提交失败')
      console.error('提交失败', error)
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2; $bg-card: #ffffff; $primary: #60A5FA; $success: #34D399; $warning: #FBBF24; $danger: #FCA5A5; $text-primary: #3B3B3B; $text-secondary: #9CA3AF; $border: #F0EDE9;

.trip-wrapper { min-height: 100vh; background: $bg-primary; }
.trip-container { padding: 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.page-title { font-size: 18px; font-weight: 500; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } .btn-icon { font-size: 16px; } }
.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }
.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .center { text-align: center; } .highlight { color: $primary; font-weight: 600; } .reason-cell { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $text-secondary; } .empty-cell { text-align: center; padding: 60px 16px; .empty-text { color: $text-secondary; font-size: 14px; } } } }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: darken($warning, 20%); .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: darken($success, 12%); .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: darken($danger, 8%); .status-dot { background: $danger; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: $primary; background: rgba($primary, 0.05); } &.cancel:hover { color: darken($warning, 20%); border-color: $warning; } &.approve:hover { color: darken($success, 12%); border-color: $success; } }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.dialog { width: 100%; max-width: 640px; background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 500; color: $text-primary; }
.dialog-close { width: 32px; height: 32px; font-size: 20px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; max-height: 70vh; overflow-y: auto; }
.form-item { margin-bottom: 16px; }
.form-label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 500; color: $text-primary; }
.form-row { display: flex; gap: 12px; .form-item { flex: 1; } }
.form-input { width: 100%; padding: 10px 14px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } &.readonly { background: darken($bg-primary, 3%); color: $primary; font-weight: 600; } }
.form-textarea { width: 100%; padding: 10px 14px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; resize: vertical; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; font-family: inherit; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; }
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: darken($danger, 8%); } } }

.itinerary-section { margin: 20px 0; padding: 16px; background: $bg-primary; border-radius: 12px; }
.section-title { font-size: 14px; font-weight: 600; color: $text-primary; margin-bottom: 16px; }
.itinerary-item { background: $bg-card; border: 1px solid $border; border-radius: 10px; padding: 16px; margin-bottom: 12px; }
.itinerary-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.itinerary-label { font-size: 14px; font-weight: 600; color: $primary; }
.remove-btn { font-size: 12px; color: $danger; background: none; border: none; cursor: pointer; &:hover { text-decoration: underline; } }
.itinerary-row { display: flex; gap: 12px; margin-bottom: 12px; .form-item { flex: 1; margin-bottom: 0; } }
.city-hint { flex: 1; display: flex; align-items: center; gap: 6px; padding: 10px 14px; font-size: 13px; color: #10B981; background: rgba(16, 185, 129, 0.1); border-radius: 8px; }
.city-select-group { display: flex; gap: 8px; select { flex: 1; } }
.add-itinerary-btn { display: flex; align-items: center; justify-content: center; gap: 6px; width: 100%; padding: 12px; font-size: 14px; color: $primary; background: rgba($primary, 0.08); border: 1px dashed $primary; border-radius: 10px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { background: rgba($primary, 0.15); } span { font-size: 18px; } }

.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 20px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; &.full-width { grid-column: 1 / -1; } }
.detail-label { font-size: 12px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } &.highlight { color: $primary; font-weight: 600; } &.reason { line-height: 1.6; color: $text-secondary; } }
.itinerary-detail { margin-top: 16px; padding: 16px; background: $bg-primary; border-radius: 12px; .section-title { margin-bottom: 12px; } }
.itinerary-row-detail { background: $bg-card; border: 1px solid $border; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.row-header { font-size: 13px; font-weight: 600; color: $primary; margin-bottom: 8px; }
.row-info { font-size: 13px; color: $text-secondary; margin-bottom: 4px; display: flex; gap: 16px; .highlight { color: $primary; font-weight: 600; margin-left: auto; } }

// 响应式设计
@media (max-width: 768px) {
  .trip-container { padding: 16px; }
  .page-header { flex-direction: column; gap: 16px; align-items: flex-start; }
  .page-title { font-size: 16px; }
  .btn-primary { width: 100%; justify-content: center; padding: 12px 20px; }
  .tabs { flex-direction: column; }
  .table-wrapper { overflow-x: auto; }
  .data-table { min-width: 600px; }
  .action-cell { flex-direction: column; gap: 6px; }
  .action-btn { width: 100%; padding: 10px 14px; }
  .dialog { max-width: 100%; margin: 16px; border-radius: 12px; }
  .dialog-body { padding: 16px; }
  .itinerary-row { flex-direction: column; }
  .dialog-footer { flex-direction: column; gap: 8px; padding: 16px; }
  .cyber-btn { width: 100%; text-align: center; }
}

@media (max-width: 480px) {
  .trip-container { padding: 12px; }
  .detail-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 15px; }
}
</style>