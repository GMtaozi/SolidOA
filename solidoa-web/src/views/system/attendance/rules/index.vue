<template>
  <div class="rules-container">
    <el-card class="rules-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">考勤规则配置</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn primary" type="primary" @click="handleSave" :loading="saving">
            <el-icon><Select /></el-icon>
            保存配置
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="rules-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="加班规则" name="overtime">
          <div class="rule-section">
            <div class="section-title">加班规则配置</div>
            <el-form :model="rulesData.overtime" label-width="140px" class="rule-form">
              <el-form-item label="加班必须审批">
                <el-switch v-model="rulesData.overtime.approvalRequired" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="允许平日加班">
                <el-switch v-model="rulesData.overtime.allowWeekday" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="允许周末加班">
                <el-switch v-model="rulesData.overtime.allowWeekend" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="允许节假日加班">
                <el-switch v-model="rulesData.overtime.allowHoliday" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="最小加班时长">
                <el-input-number v-model="rulesData.overtime.minOvertimeMinutes" :min="0" :max="480" />
                <span class="form-tip">分钟，超过此时间才计入加班</span>
              </el-form-item>
              <el-form-item label="加班折算方式">
                <el-radio-group v-model="rulesData.overtime.conversionType">
                  <el-radio label="1:1">1:1 等时调休</el-radio>
                  <el-radio label="1:1.5">1:1.5 加班工资</el-radio>
                  <el-radio label="1:2">1:2 节假日双倍</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="补卡规则" name="repair">
          <div class="rule-section">
            <div class="section-title">补卡规则配置</div>
            <el-form :model="rulesData.repair" label-width="140px" class="rule-form">
              <el-form-item label="允许补卡">
                <el-switch v-model="rulesData.repair.allowRepair" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="补卡需要审批">
                <el-switch v-model="rulesData.repair.approvalRequired" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="每月补卡次数上限">
                <el-input-number v-model="rulesData.repair.monthlyLimit" :min="0" :max="31" />
                <span class="form-tip">0表示不限制</span>
              </el-form-item>
              <el-form-item label="补卡时限">
                <el-input-number v-model="rulesData.repair.timeLimitDays" :min="0" :max="30" />
                <span class="form-tip">天后可补卡，0表示不限制</span>
              </el-form-item>
              <el-form-item label="补卡申请范围">
                <el-select v-model="rulesData.repair.scopeType" class="cyber-select">
                  <el-option label="只能补当天" value="SAME_DAY" />
                  <el-option label="可补3天内" value="WITHIN_3_DAYS" />
                  <el-option label="可补7天内" value="WITHIN_7_DAYS" />
                  <el-option label="可补30天内" value="WITHIN_30_DAYS" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="外勤规则" name="outdoor">
          <div class="rule-section">
            <div class="section-title">外勤规则配置</div>
            <el-form :model="rulesData.outdoor" label-width="140px" class="rule-form">
              <el-form-item label="允许外勤打卡">
                <el-switch v-model="rulesData.outdoor.allowOutdoor" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="外勤需要审批">
                <el-switch v-model="rulesData.outdoor.approvalRequired" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="允许范围">
                <el-input-number v-model="rulesData.outdoor.rangeLimit" :min="0" :max="10000" />
                <span class="form-tip">米，超出此范围无法外勤打卡</span>
              </el-form-item>
              <el-form-item label="需要拍照上传">
                <el-switch v-model="rulesData.outdoor.requirePhoto" active-color="#34D399" />
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="迟到早退" name="late">
          <div class="rule-section">
            <div class="section-title">迟到早退规则配置</div>
            <el-form :model="rulesData.late" label-width="140px" class="rule-form">
              <el-form-item label="启用迟到规则">
                <el-switch v-model="rulesData.late.enableLateRule" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="迟到计入异常">
                <el-switch v-model="rulesData.late.countAsAnomaly" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="迟到扣款方式">
                <el-radio-group v-model="rulesData.late.deductionType">
                  <el-radio label="NONE">不扣款</el-radio>
                  <el-radio label="FIXED">固定金额</el-radio>
                  <el-radio label="PERCENT">按比例</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="rulesData.late.deductionType === 'FIXED'" label="每次扣款金额">
                <el-input-number v-model="rulesData.late.deductionAmount" :min="0" :precision="2" />
                <span class="form-tip">元</span>
              </el-form-item>
              <el-form-item v-if="rulesData.late.deductionType === 'PERCENT'" label="扣款比例">
                <el-input-number v-model="rulesData.late.deductionPercent" :min="0" :max="100" />
                <span class="form-tip">%</span>
              </el-form-item>
              <el-form-item label="启用早退规则">
                <el-switch v-model="rulesData.late.enableEarlyLeaveRule" active-color="#34D399" />
              </el-form-item>
              <el-form-item label="早退计入异常">
                <el-switch v-model="rulesData.late.earlyLeaveCountAsAnomaly" active-color="#34D399" />
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="高级设置" name="advanced">
          <div class="rule-section">
            <div class="section-title">高级规则配置（JSON）</div>
            <div class="json-editor-wrapper">
              <el-input
                v-model="jsonText"
                type="textarea"
                :rows="20"
                placeholder="请输入JSON格式的规则配置"
                class="json-textarea"
                @blur="validateJson"
              />
              <div v-if="jsonError" class="json-error">
                <el-icon><Warning /></el-icon>
                {{ jsonError }}
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { hrApi } from '@/api/hr'
import { ElMessage } from 'element-plus'
import { Select, Warning } from '@element-plus/icons-vue'

const activeTab = ref('overtime')
const saving = ref(false)
const jsonError = ref('')
const jsonText = ref('')

const rulesData = reactive({
  overtime: {
    approvalRequired: true,
    allowWeekday: true,
    allowWeekend: true,
    allowHoliday: true,
    minOvertimeMinutes: 30,
    conversionType: '1:1'
  },
  repair: {
    allowRepair: true,
    approvalRequired: true,
    monthlyLimit: 3,
    timeLimitDays: 7,
    scopeType: 'WITHIN_7_DAYS'
  },
  outdoor: {
    allowOutdoor: true,
    approvalRequired: false,
    rangeLimit: 5000,
    requirePhoto: false
  },
  late: {
    enableLateRule: true,
    countAsAnomaly: true,
    deductionType: 'NONE',
    deductionAmount: 0,
    deductionPercent: 0,
    enableEarlyLeaveRule: true,
    earlyLeaveCountAsAnomaly: true
  }
})

const loadData = async () => {
  try {
    const res = await hrApi.getRules()
    const data = res.data || {}
    // 合并已有规则
    if (data.overtime) Object.assign(rulesData.overtime, data.overtime)
    if (data.repair) Object.assign(rulesData.repair, data.repair)
    if (data.outdoor) Object.assign(rulesData.outdoor, data.outdoor)
    if (data.late) Object.assign(rulesData.late, data.late)
    jsonText.value = JSON.stringify(data, null, 2)
  } catch (error) {
    console.error('加载规则配置失败', error)
    jsonText.value = JSON.stringify(rulesData, null, 2)
  }
}

const handleTabChange = (tabName) => {
  if (tabName === 'advanced') {
    jsonText.value = JSON.stringify(rulesData, null, 2)
  }
}

const validateJson = () => {
  try {
    JSON.parse(jsonText.value)
    jsonError.value = ''
    return true
  } catch (e) {
    jsonError.value = 'JSON格式错误: ' + e.message
    return false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    let submitData
    if (activeTab.value === 'advanced') {
      if (!validateJson()) {
        ElMessage.error('请修正JSON格式错误')
        return
      }
      submitData = JSON.parse(jsonText.value)
    } else {
      // 保存所有规则配置，避免覆盖其他规则
      submitData = {
        overtime: { ...rulesData.overtime },
        repair: { ...rulesData.repair },
        outdoor: { ...rulesData.outdoor },
        late: { ...rulesData.late }
      }
      // 替换当前tab的值为表单值
      submitData[activeTab.value] = { ...rulesData[activeTab.value] }
    }
    await hrApi.updateRules(submitData)
    ElMessage.success('保存成功')
    loadData()
  } catch (error) {
    console.error('保存规则配置失败', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 监听tab变化，同步数据到JSON
watch(activeTab, (newTab) => {
  if (newTab !== 'advanced') {
    jsonText.value = JSON.stringify(rulesData, null, 2)
  }
})

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.rules-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.rules-card {
  background: $bg-card;
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);

  :deep(.el-card__header) {
    border-bottom: 1px solid $border-color;
    padding: 20px 24px;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title-wrapper {
    display: flex;
    align-items: center;
    gap: 16px;

    .title-text {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
    }

    .title-line {
      width: 40px;
      height: 3px;
      background: linear-gradient(90deg, $primary, transparent);
      border-radius: 2px;
    }
  }
}

.rules-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }

  :deep(.el-tabs__item) {
    font-weight: 500;
    color: $text-secondary;

    &.is-active {
      color: $primary;
    }

    &:hover {
      color: $primary;
    }
  }

  :deep(.el-tabs__nav-wrap::after) {
    background-color: $border-color;
  }

  :deep(.el-tabs__active-bar) {
    background-color: $primary;
  }
}

.rule-section {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-color;
  }
}

.rule-form {
  max-width: 600px;

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-form-item__label) {
    color: $text-secondary;
    font-weight: 500;
  }

  :deep(.el-input-number) {
    .el-input__wrapper {
      border-radius: 12px;
      padding: 0 12px;
    }
  }
}

.cyber-select {
  width: 200px;
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: $text-secondary;
}

.json-editor-wrapper {
  position: relative;
}

.json-textarea {
  :deep(.el-textarea__inner) {
    font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
    font-size: 13px;
    line-height: 1.6;
    border-radius: 12px;
    padding: 16px;
    resize: none;
    background: #fafafa;
  }
}

.json-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(#FBBF24, 0.1);
  border-radius: 8px;
  color: #FBBF24;
  font-size: 13px;

  :deep(.el-icon) {
    font-size: 16px;
  }
}

.cyber-btn {
  background: $bg-card;
  border: 1px solid $border-color;
  color: $text-primary;
  font-weight: 500;
  border-radius: 12px;
  padding: 10px 20px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.primary {
    background: $primary;
    border-color: $primary;
    color: #ffffff;

    &:hover {
      background: #5a95f7;
      border-color: #5a95f7;
    }
  }
}
</style>