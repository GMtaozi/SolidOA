<template>
  <div class="expense-wrapper">
    <div class="expense-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">差旅报销</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新增报销</span>
        </button>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon blue">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2"/></svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ formatAmount(stats.totalAmount || 0) }}</span>
            <span class="stat-label">累计报销</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/><polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2"/></svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.pendingCount || 0 }}</span>
            <span class="stat-label">待审批</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" stroke-width="2"/><polyline points="22,4 12,14.01 9,11.01" stroke="currentColor" stroke-width="2"/></svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.approvedCount || 0 }}</span>
            <span class="stat-label">已通过</span>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button class="tab-item" :class="{ active: activeTab === 'my' }" @click="activeTab = 'my'; loadData()">
          <span class="tab-indicator"></span>我提交的
        </button>
        <button class="tab-item" :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'; loadData()">
          <span class="tab-indicator"></span>待我审批
        </button>
      </div>

      <!-- 数据表格 -->
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
          <template #status="{ row }">
            <OaStatusBadge
              :type="getBadgeType(row.status)"
              :text="getStatusText(row.status)"
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

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <button class="page-btn" :disabled="page <= 1" @click="page--; loadData()">上一页</button>
        <span class="page-info">{{ page }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="page >= totalPages" @click="page++; loadData()">下一页</button>
      </div>
    </div>

    <!-- 差旅报销表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog dialog-xl">
        <div class="dialog-header">
          <h3 class="dialog-title">新增差旅报销</h3>
          <button class="dialog-close" @click="dialogVisible = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <!-- 关联出差审批 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" stroke="currentColor" stroke-width="2"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">关联出差审批</span>
              <span class="required-tag">必填</span>
            </div>
            <div class="relation-select">
              <select class="form-input" v-model="form.relatedTripId" @change="onTripChange">
                <option value="">请选择已通过出差审批单</option>
                <option v-for="trip in approvedTrips" :key="trip.id" :value="trip.id">
                  {{ trip.tripNo }} ({{ trip.travelStartDate }} ~ {{ trip.travelEndDate }}, {{ trip.totalDays || 0 }}天)
                </option>
              </select>
            </div>
            <div class="relation-info" v-if="selectedTrip">
              <div class="trip-info-card">
                <div class="trip-header">
                  <span class="trip-no">{{ selectedTrip.tripNo }}</span>
                  <span class="trip-status approved">已通过</span>
                </div>
                <div class="trip-detail">
                  <span>出差时间：{{ selectedTrip.travelStartDate }} ~ {{ selectedTrip.travelEndDate }}</span>
                  <span>时长：{{ selectedTrip.totalDays || 0 }}天</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 出差信息 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">出差信息</span>
            </div>
            <div class="region-selector">
              <div class="region-label">出差区域</div>
              <div class="region-chips">
                <div v-for="(region, index) in form.travelRegions" :key="index" class="region-chip">
                  <span>{{ region.city }}</span>
                  <button class="chip-remove" @click="removeRegion(index)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
                  </button>
                </div>
                <button class="add-region-btn" @click="showRegionPicker = true">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><line x1="12" y1="5" x2="12" y2="19" stroke="currentColor" stroke-width="2"/><line x1="5" y1="12" x2="19" y2="12" stroke="currentColor" stroke-width="2"/></svg>
                  添加区域
                </button>
              </div>
            </div>

            <div class="time-row">
              <div class="form-item">
                <label class="form-label">实际开始时间</label>
                <input type="date" class="form-input" v-model="form.travelStartDate" @change="calcDuration" />
              </div>
              <div class="form-item">
                <label class="form-label">实际结束时间</label>
                <input type="date" class="form-input" v-model="form.travelEndDate" @change="calcDuration" />
              </div>
              <div class="form-item duration-item">
                <label class="form-label">出差时长</label>
                <div class="duration-display">
                  <span class="duration-value">{{ form.duration }}</span>
                  <span class="duration-unit">天</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 费用明细 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><line x1="12" y1="1" x2="12" y2="23" stroke="currentColor" stroke-width="2"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">费用明细</span>
              <span class="section-total">合计：<strong>{{ formatAmount(totalAmount) }}</strong></span>
            </div>

            <div class="expense-items">
              <div v-for="(item, index) in form.expenseItems" :key="index" class="expense-item">
                <div class="expense-header">
                  <span class="item-index">费用{{ index + 1 }}</span>
                  <button v-if="form.expenseItems.length > 1" class="item-remove" @click="removeExpenseItem(index)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
                  </button>
                </div>
                <div class="expense-row">
                  <div class="form-item">
                    <label class="form-label">费用类型</label>
                    <select class="form-input" v-model="item.expenseType">
                      <option value="">请选择</option>
                      <option value="TRANSPORT">交通费</option>
                      <option value="MEAL">餐饮费</option>
                      <option value="ACCOMMODATION">住宿费</option>
                    </select>
                  </div>
                  <div class="form-item">
                    <label class="form-label">报销金额</label>
                    <input type="number" class="form-input" v-model.number="item.amount" placeholder="0.00" step="0.01" @input="calcTotalAmount" />
                  </div>
                </div>
                <div class="expense-row">
                  <div class="form-item">
                    <label class="form-label">费用发生时间</label>
                    <input type="date" class="form-input" v-model="item.expenseDate" />
                  </div>
                  <div class="form-item">
                    <label class="form-label">费用说明</label>
                    <input type="text" class="form-input" v-model="item.description" placeholder="简要说明费用用途" />
                  </div>
                </div>
              </div>
              <button class="add-expense-btn" @click="addExpenseItem">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><line x1="12" y1="5" x2="12" y2="19" stroke="currentColor" stroke-width="2"/><line x1="5" y1="12" x2="19" y2="12" stroke="currentColor" stroke-width="2"/></svg>
                添加费用明细
              </button>
            </div>
          </div>

          <!-- 发票与附件 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2"/><polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">发票与附件</span>
            </div>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="10"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :file-list="form.attachments"
              accept=".jpg,.jpeg,.png,.pdf"
              multiple
              class="expense-upload"
            >
              <el-button type="primary" plain size="small">
                <el-icon><Plus /></el-icon> 选择文件
              </el-button>
              <template #tip>
                <div class="upload-tip">支持 jpg、png、pdf 格式，最多10个文件</div>
              </template>
            </el-upload>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" :disabled="isSubmitting || uploading" @click="handleSubmit">
            <span v-if="isSubmitting || uploading" class="loading-spinner"></span>
            {{ isSubmitting || uploading ? '提交中...' : '提交报销' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 区域选择器弹窗 -->
    <div class="dialog-overlay" v-if="showRegionPicker" @click.self="showRegionPicker = false">
      <div class="dialog dialog-sm">
        <div class="dialog-header">
          <h3 class="dialog-title">选择出差区域</h3>
          <button class="dialog-close" @click="showRegionPicker = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label">省份/直辖市</label>
            <select class="form-input" v-model="tempRegion.province">
              <option value="">请选择</option>
              <option v-for="p in provinceList" :key="p" :value="p">{{ p }}</option>
            </select>
          </div>
          <div class="form-item" v-if="tempRegion.province && !isDirectCity(tempRegion.province)">
            <label class="form-label">城市</label>
            <select class="form-input" v-model="tempRegion.city">
              <option value="">请选择城市</option>
              <option v-for="c in cityList" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="city-hint" v-if="tempRegion.province && isDirectCity(tempRegion.province)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/><line x1="12" y1="16" x2="12" y2="12" stroke="currentColor" stroke-width="2"/><line x1="12" y1="8" x2="12.01" y2="8" stroke="currentColor" stroke-width="2"/></svg>
            <span>直辖市已自动选择</span>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="showRegionPicker = false">取消</button>
          <button class="cyber-btn primary" @click="confirmRegion">确定</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h3 class="dialog-title">报销详情</h3>
          <button class="dialog-close" @click="detailVisible = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="detail-card">
            <div class="detail-header">
              <span class="detail-no">{{ currentRow?.expenseNo }}</span>
              <span class="status-badge" :class="getStatusClass(currentRow?.status)">
                <span class="status-dot"></span>
                {{ getStatusText(currentRow?.status) }}
              </span>
            </div>

            <!-- 审批流程图（V2.0 接入 State Machine） -->
            <OaApprovalCard
              v-if="currentRow && currentRow.id"
              title="审批流程"
              business-type="EXPENSE"
              :business-id="currentRow.id"
              class="detail-flow-card"
            />
            <div class="detail-amount">
              <span class="currency">¥</span>
              <span class="amount">{{ formatAmount(currentRow?.amount) }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-title">出差信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">出差区域</span>
                <span class="detail-value">{{ currentRow?.travelRegions || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">出差时长</span>
                <span class="detail-value highlight">{{ currentRow?.duration || 0 }}天</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">开始时间</span>
                <span class="detail-value mono">{{ currentRow?.travelStartDate }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">结束时间</span>
                <span class="detail-value mono">{{ currentRow?.travelEndDate }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section" v-if="currentRow?.expenseItems?.length">
            <div class="section-title">费用明细</div>
            <div class="expense-list">
              <div v-for="(item, index) in currentRow.expenseItems" :key="index" class="expense-list-item">
                <div class="expense-type">
                  <span class="type-badge" :class="getExpenseTypeClass(item.expenseType)">
                    {{ getExpenseTypeName(item.expenseType) }}
                  </span>
                </div>
                <div class="expense-info">
                  <span class="expense-desc">{{ item.description || '-' }}</span>
                  <span class="expense-date">{{ item.expenseDate }}</span>
                </div>
                <div class="expense-amount">{{ formatAmount(item.amount) }}</div>
              </div>
            </div>
          </div>

          <!-- 审批流程 -->
          <div class="approval-flow" v-if="flowData?.nodes?.length">
            <div class="flow-title">审批记录</div>
            <div v-for="(node, index) in flowData.nodes" :key="index" class="flow-item">
              <div class="flow-dot" :class="node.status === 'APPROVED' ? 'approved' : node.status === 'REJECTED' ? 'rejected' : 'pending'"></div>
              <div class="flow-content">
                <div class="flow-header">
                  <span class="flow-user">{{ node.name }}</span>
                  <span class="flow-time" v-if="node.approvedTime">{{ formatTime(node.approvedTime) }}</span>
                </div>
                <div class="flow-action" v-if="node.status">
                  <span class="flow-badge" :class="node.status === 'APPROVED' ? 'approved' : 'rejected'">
                    {{ node.status === 'APPROVED' ? '同意' : node.status === 'REJECTED' ? '拒绝' : '待审批' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-footer" v-if="activeTab === 'pending' && currentRow?.status === 'PENDING'">
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
import { reactive, ref, computed, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { hrApi } from '@/api/hr'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadFiles } from '@/utils/upload'

// 直辖市列表
const DIRECT_CITIES = ['北京市', '上海市', '天津市', '重庆市']

const activeTab = ref('my')
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const uploading = ref(false)
const currentRow = ref(null)

// 表格列定义
const columns = [
  { prop: 'expenseNo', label: '报销单号', width: 160 },
  { prop: 'travelRegions', label: '出差区域', minWidth: 140 },
  { prop: 'travelDate', label: '出差时间', width: 220, formatter: (val, row) => `${row.travelStartDate || ''} ~ ${row.travelEndDate || ''}` },
  { prop: 'duration', label: '时长', width: 100, formatter: (val) => `${val || 0}天` },
  { prop: 'amount', label: '报销金额', width: 130, formatter: (val) => formatAmount(val) },
  { prop: 'status', label: '状态', width: 110 }
]

// 状态 -> OaStatusBadge type
const getBadgeType = (status) => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', PAID: 'primary'
}[status] || 'default')
const flowData = ref(null)
const showRegionPicker = ref(false)
const uploadRef = ref(null)
const approvedTrips = ref([])
const selectedTrip = ref(null)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const stats = ref({ totalAmount: 0, pendingCount: 0, approvedCount: 0 })

// 省市数据
const provinceList = ['北京市', '上海市', '天津市', '重庆市', '广东省', '浙江省', '江苏省', '四川省', '湖北省', '湖南省', '河南省', '河北省', '山东省', '福建省', '安徽省', '江西省', '陕西省', '辽宁省', '吉林省', '黑龙江省', '云南省', '贵州省', '甘肃省', '海南省', '内蒙古', '广西', '宁夏', '青海省', '新疆', '西藏']

const cityMap = {
  '北京市': [],
  '上海市': [],
  '天津市': [],
  '重庆市': [],
  '广东省': ['广州市', '深圳市', '佛山市', '东莞市', '珠海市', '中山市', '惠州市', '汕头市', '湛江市', '江门市'],
  '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市'],
  '江苏省': ['南京市', '苏州市', '无锡市', '常州市', '南通市', '徐州市', '连云港市', '淮安市', '盐城市', '扬州市'],
  '四川省': ['成都市', '绵阳市', '德阳市', '南充市', '宜宾市', '自贡市', '攀枝花市', '泸州市'],
  '湖北省': ['武汉市', '宜昌市', '襄阳市', '荆州市', '黄石市', '十堰市', '孝感市', '荆门市'],
  '湖南省': ['长沙市', '株洲市', '湘潭市', '衡阳市', '岳阳市', '常德市', '张家界市', '益阳市'],
  '山东省': ['济南市', '青岛市', '烟台市', '威海市', '潍坊市', '淄博市', '临沂市', '济宁市'],
  '福建省': ['福州市', '厦门市', '泉州市', '漳州市', '莆田市', '三明市', '龙岩市', '南平市']
}

const tempRegion = reactive({ province: '', city: '' })
const cityList = computed(() => cityMap[tempRegion.province] || [])

const isDirectCity = (province) => DIRECT_CITIES.includes(province)

const form = reactive({
  relatedTripId: '',
  travelRegions: [],
  travelStartDate: '',
  travelEndDate: '',
  duration: 0,
  expenseItems: [{ expenseType: '', amount: null, expenseDate: '', description: '' }],
  attachments: []
})

const totalAmount = computed(() => {
  return form.expenseItems.reduce((sum, item) => sum + (item.amount || 0), 0)
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const expenseTypeMap = {
  TRANSPORT: '交通费',
  MEAL: '餐饮费',
  ACCOMMODATION: '住宿费'
}

const getExpenseTypeClass = (type) => ({ TRANSPORT: 'transport', MEAL: 'meal', ACCOMMODATION: 'accommodation' }[type] || '')
const getExpenseTypeName = (type) => expenseTypeMap[type] || type || '-'

const getStatusClass = (status) => ({ PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' }[status] || '')
const getStatusText = (status) => ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已撤回' }[status] || status)

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return parseFloat(amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// 移除未使用的formatFileSize函数，使用Vue的computed在上传组件中直接显示文件大小

const calcDuration = () => {
  if (form.travelStartDate && form.travelEndDate) {
    const start = new Date(form.travelStartDate)
    const end = new Date(form.travelEndDate)
    const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
    form.duration = Math.max(0, diff)
  }
}

const calcTotalAmount = () => {}

const confirmRegion = () => {
  if (!tempRegion.province) {
    ElMessage.warning('请选择省份')
    return
  }
  // 直辖市直接取省名作为市名
  const city = isDirectCity(tempRegion.province) ? tempRegion.province : tempRegion.city
  if (!city) {
    ElMessage.warning('请选择城市')
    return
  }
  form.travelRegions.push({ province: tempRegion.province, city })
  tempRegion.province = ''
  tempRegion.city = ''
  showRegionPicker.value = false
}

const removeRegion = (index) => {
  form.travelRegions.splice(index, 1)
}

const addExpenseItem = () => {
  form.expenseItems.push({ expenseType: '', amount: null, expenseDate: '', description: '' })
}

const removeExpenseItem = (index) => {
  form.expenseItems.splice(index, 1)
}

const triggerUpload = () => {}

const handleFileChange = (file, fileList) => {
  form.attachments = fileList
}

const handleFileRemove = (file, fileList) => {
  form.attachments = fileList
}

const removeAttachment = (index) => {
  form.attachments.splice(index, 1)
}

// 选择出差审批单
const onTripChange = () => {
  if (form.relatedTripId) {
    selectedTrip.value = approvedTrips.value.find(t => t.id === form.relatedTripId)
    if (selectedTrip.value) {
      form.travelStartDate = selectedTrip.value.travelStartDate || selectedTrip.value.startDate
      form.travelEndDate = selectedTrip.value.travelEndDate || selectedTrip.value.endDate
      form.duration = selectedTrip.value.totalDays || selectedTrip.value.days || 0
      // 自动解析出差区域
      if (selectedTrip.value.itineraries && selectedTrip.value.itineraries.length) {
        form.travelRegions = selectedTrip.value.itineraries.map(it => ({
          province: it.departure?.split('-')[0] || '',
          city: it.destination?.split('-')[1] || it.destination || ''
        })).filter(r => r.city)
      }
    }
  } else {
    selectedTrip.value = null
  }
}

// 加载已通过出差审批单列表
const loadApprovedTrips = async () => {
  try {
    const res = await hrApi.getApprovedBusinessTrips()
    approvedTrips.value = res.data?.data || res.data || []
  } catch (error) {
    console.error('加载出差审批单失败', error)
    approvedTrips.value = []
  }
}

const loadStatistics = async () => {
  try {
    const res = await hrApi.getStatistics()
    if (res.data) {
      stats.value = {
        totalAmount: res.data.totalAmount || 0,
        pendingCount: res.data.pendingCount || 0,
        approvedCount: res.data.approvedCount || 0
      }
    }
  } catch (e) {
    stats.value = { totalAmount: 0, pendingCount: 0, approvedCount: 0 }
  }
}

const loadData = async () => {
  try {
    let res
    if (activeTab.value === 'my') {
      res = await hrApi.getExpenseList(query)
    } else {
      res = await workflowApi.getMyTasks(query)
    }
    const data = res.data?.data
    tableData.value = data?.records || res.data?.data || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const handleAdd = () => {
  loadApprovedTrips()
  Object.assign(form, {
    relatedTripId: '',
    travelRegions: [],
    travelStartDate: '',
    travelEndDate: '',
    duration: 0,
    expenseItems: [{ expenseType: '', amount: null, expenseDate: '', description: '' }],
    attachments: []
  })
  selectedTrip.value = null
  dialogVisible.value = true
}

const handleView = async (row) => {
  currentRow.value = row
  detailVisible.value = true
  try {
    const res = await workflowApi.getApprovalRecordFlow(row.id)
    flowData.value = res.data
  } catch (error) {
    flowData.value = null
  }
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定要撤回该报销申请吗？', '提示', { type: 'warning' })
  await hrApi.cancelExpense(row.id)
  ElMessage.success('撤回成功')
  loadData()
}

const handleApprove = async (row) => {
  currentRow.value = row
  await ElMessageBox.confirm(
    `<div style="text-align:left">
      <p><strong>报销单号：</strong>${row.expenseNo}</p>
      <p><strong>报销金额：</strong>¥${formatAmount(row.amount)}</p>
    </div>`,
    '确认通过报销申请？',
    { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    await hrApi.approveExpense(row.id, { approveType: 'APPROVED', comment: '同意' })
    ElMessage.success('审批成功')
    loadData()
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await hrApi.approveExpense(currentRow.value.id, { approveType: 'APPROVED', comment: '同意' })
  ElMessage.success('审批成功')
  detailVisible.value = false
  loadData()
}

const handleReject = async () => {
  const { value: comment } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
    confirmButtonText: '确认', cancelButtonText: '取消',
    inputPattern: /\S+/, inputErrorMessage: '请输入拒绝原因'
  })
  await hrApi.approveExpense(currentRow.value.id, { approveType: 'REJECTED', comment })
  ElMessage.success('已拒绝')
  detailVisible.value = false
  loadData()
}

const handleSubmit = async () => {
  if (!form.relatedTripId) { ElMessage.warning('请选择关联出差审批单'); return }
  if (!form.travelStartDate) { ElMessage.warning('请选择出差开始时间'); return }
  if (!form.travelEndDate) { ElMessage.warning('请选择出差结束时间'); return }
  if (form.expenseItems.every(item => !item.expenseType || !item.amount)) {
    ElMessage.warning('请至少填写一条费用明细')
    return
  }

  isSubmitting.value = true
  uploading.value = true
  try {
    // 上传文件
    const fileList = form.attachments.filter(f => f.raw).map(f => f.raw)
    let attachmentUrls = []
    if (fileList.length > 0) {
      attachmentUrls = await uploadFiles(fileList)
    }

    const payload = {
      relatedTripId: form.relatedTripId,
      travelRegions: form.travelRegions.map(r => r.city).join(','),
      travelStartDate: form.travelStartDate,
      travelEndDate: form.travelEndDate,
      duration: form.duration,
      amount: totalAmount.value,
      expenseItems: form.expenseItems.filter(item => item.expenseType && item.amount),
      attachments: attachmentUrls.join(',')
    }
    await hrApi.createExpense(payload)
    ElMessage.success('提交成功')
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败', error)
    ElMessage.error('提交失败：' + (error.message || '系统异常'))
  } finally {
    isSubmitting.value = false
    uploading.value = false
  }
}

onMounted(() => { loadStatistics(); loadData() })
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$danger: #FCA5A5;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border: #F0EDE9;

.expense-wrapper { min-height: 100vh; background: $bg-primary; }
.expense-container { padding: 30px; max-width: 1400px; margin: 0 auto; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: #5a95f7; transform: translateY(-1px); } .btn-icon { font-size: 16px; } }

/* 统计卡片 */
.stats-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 16px; padding: 20px; background: $bg-card; border-radius: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08); } }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; &.blue { background: rgba($primary, 0.12); color: $primary; } &.yellow { background: rgba($warning, 0.12); color: $warning; } &.green { background: rgba($success, 0.12); color: $success; } }
.stat-content { display: flex; flex-direction: column; gap: 2px; }
.stat-value { font-size: 24px; font-weight: 700; color: $text-primary; line-height: 1; }
.stat-label { font-size: 13px; color: $text-secondary; }

.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }

.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .highlight { color: $primary; font-weight: 600; } .amount { color: #ea7c3b; font-weight: 600; } .region-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $text-secondary; } .time-cell { font-size: 12px; color: $text-secondary; } .empty-cell { text-align: center; padding: 80px 16px; } } }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 12px; color: $text-secondary; svg { opacity: 0.4; } }

.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: #d49b1f; .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: #2eb385; .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: #e57373; .status-dot { background: $danger; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: rgba($primary, 0.5); background: rgba($primary, 0.05); } &.cancel:hover { color: #d49b1f; border-color: rgba($warning, 0.5); } &.approve:hover { color: #2eb385; border-color: rgba($success, 0.5); } }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 20px; }
.page-btn { padding: 8px 16px; font-size: 14px; color: $text-secondary; background: $bg-card; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover:not(:disabled) { color: $primary; border-color: $primary; } &:disabled { opacity: 0.5; cursor: not-allowed; } }
.page-info { font-size: 14px; color: $text-secondary; }

/* 弹窗 */
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(4px); }
.dialog { width: 100%; max-width: 560px; max-height: 90vh; background: $bg-card; border-radius: 20px; overflow: hidden; box-shadow: 0 24px 48px -12px rgba(0,0,0,0.2); display: flex; flex-direction: column; &.dialog-xl { max-width: 680px; } &.dialog-lg { max-width: 600px; } &.dialog-sm { max-width: 400px; } }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; flex-shrink: 0; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 600; color: $text-primary; }
.dialog-close { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: $text-secondary; background: transparent; border: none; border-radius: 10px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; overflow-y: auto; flex: 1; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; flex-shrink: 0; }

/* 表单分区 */
.form-section { margin-bottom: 28px; &:last-child { margin-bottom: 0; } }
.section-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.section-icon { color: $primary; }
.section-title { font-size: 14px; font-weight: 600; color: $text-primary; }
.section-total { margin-left: auto; font-size: 14px; color: $text-secondary; strong { color: #ea7c3b; font-weight: 600; } }
.required-tag { padding: 2px 8px; font-size: 11px; font-weight: 500; color: #fff; background: $danger; border-radius: 4px; }

/* 输入 */
.form-item { margin-bottom: 12px; }
.form-label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 500; color: $text-primary; }
.form-input { width: 100%; padding: 10px 14px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 10px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 3px rgba($primary, 0.15); } &::placeholder { color: $text-secondary; } }
.city-hint { display: flex; align-items: center; gap: 6px; padding: 10px 14px; background: rgba($primary, 0.1); border-radius: 10px; font-size: 13px; color: $primary; }

/* 关联出差 */
.relation-select { select { cursor: pointer; } }
.trip-info-card { margin-top: 12px; padding: 14px; background: rgba($success, 0.08); border: 1px solid rgba($success, 0.2); border-radius: 12px; }
.trip-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.trip-no { font-size: 14px; font-weight: 600; color: $text-primary; }
.trip-status { padding: 2px 8px; font-size: 12px; border-radius: 4px; &.approved { background: rgba($success, 0.15); color: #2eb385; } }
.trip-detail { display: flex; gap: 16px; font-size: 13px; color: $text-secondary; }

/* 区域选择 */
.region-selector { margin-bottom: 16px; }
.region-label { font-size: 13px; color: $text-secondary; margin-bottom: 8px; }
.region-chips { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.region-chip { display: flex; align-items: center; gap: 6px; padding: 6px 10px; background: rgba($primary, 0.1); border-radius: 20px; font-size: 13px; color: $primary; }
.chip-remove { display: flex; align-items: center; justify-content: center; width: 18px; height: 18px; background: rgba($primary, 0.2); border: none; border-radius: 50%; color: $primary; cursor: pointer; &:hover { background: rgba($primary, 0.3); } }
.add-region-btn { display: flex; align-items: center; gap: 4px; padding: 6px 12px; font-size: 13px; color: $primary; background: rgba($primary, 0.08); border: 1px dashed $primary; border-radius: 20px; cursor: pointer; &:hover { background: rgba($primary, 0.15); } }

/* 时间行 */
.time-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: 12px; }
.duration-item { min-width: 100px; }
.duration-display { display: flex; align-items: center; gap: 4px; padding: 10px 14px; background: rgba($primary, 0.1); border-radius: 10px; }
.duration-value { font-size: 18px; font-weight: 600; color: $primary; }
.duration-unit { font-size: 14px; color: $primary; }

/* 费用明细 */
.expense-items { display: flex; flex-direction: column; gap: 12px; }
.expense-item { padding: 16px; background: $bg-primary; border-radius: 12px; }
.expense-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.item-index { font-size: 14px; font-weight: 600; color: $text-primary; }
.item-remove { display: flex; align-items: center; justify-content: center; width: 24px; height: 24px; color: $danger; background: rgba($danger, 0.1); border: none; border-radius: 6px; cursor: pointer; &:hover { background: rgba($danger, 0.2); } }
.expense-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.add-expense-btn { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 12px; font-size: 14px; color: $primary; background: rgba($primary, 0.08); border: 1px dashed $primary; border-radius: 12px; cursor: pointer; &:hover { background: rgba($primary, 0.15); } }

/* 上传 */
.expense-upload { display: block; }
.upload-tip { margin-top: 8px; font-size: 12px; color: $text-secondary; }
:deep(.el-upload-list) { margin-top: 12px; }
:deep(.el-upload-list__item) { border-radius: 10px; padding: 8px 12px; }
:deep(.el-upload-list__item-name) { font-size: 13px; }

/* 按钮 */
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; gap: 8px; &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: #5a95f7; } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: #f08080; } } }
.loading-spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 详情 */
.detail-card { background: linear-gradient(135deg, rgba($primary, 0.08) 0%, rgba($primary, 0.02) 100%); border-radius: 16px; padding: 20px; margin-bottom: 20px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.detail-no { font-size: 18px; font-weight: 600; color: $text-primary; }
.detail-amount { display: flex; align-items: baseline; gap: 4px; .currency { font-size: 16px; color: #ea7c3b; } .amount { font-size: 32px; font-weight: 700; color: #ea7c3b; } }
.detail-section { background: $bg-primary; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 12px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; } &.highlight { color: $primary; font-weight: 600; } }

.expense-list { display: flex; flex-direction: column; gap: 8px; }
.expense-list-item { display: flex; align-items: center; gap: 12px; padding: 10px; background: $bg-card; border-radius: 8px; }
.type-badge { display: inline-block; padding: 4px 10px; font-size: 12px; font-weight: 500; border-radius: 6px; &.transport { background: rgba($primary, 0.15); color: $primary; } &.meal { background: rgba($success, 0.15); color: #2eb385; } &.accommodation { background: rgba($warning, 0.15); color: #d49b1f; } }
.expense-info { flex: 1; display: flex; flex-direction: column; gap: 2px; .expense-desc { font-size: 13px; color: $text-primary; } .expense-date { font-size: 12px; color: $text-secondary; } }
.expense-amount { font-size: 14px; font-weight: 600; color: #ea7c3b; }

/* 审批流程 */
.approval-flow { margin-top: 20px; }
.flow-title { font-size: 14px; font-weight: 600; color: $text-primary; margin-bottom: 16px;   }
.flow-item { display: flex; gap: 12px; padding-bottom: 16px; position: relative; &:not(:last-child)::before { content: ''; position: absolute; left: 7px; top: 16px; bottom: 0; width: 2px; background: $border; } }
.flow-dot { width: 16px; height: 16px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; &.approved { background: $success; } &.rejected { background: $danger; } &.pending { background: $border; } }
.flow-content { flex: 1; }
.flow-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.flow-user { font-size: 14px; font-weight: 500; color: $text-primary; }
.flow-time { font-size: 12px; color: $text-secondary; }
.flow-badge { padding: 2px 8px; font-size: 12px; font-weight: 500; border-radius: 4px; &.approved { background: rgba($success, 0.15); color: #2eb385; } &.rejected { background: rgba($danger, 0.15); color: #e57373; } }
</style>
