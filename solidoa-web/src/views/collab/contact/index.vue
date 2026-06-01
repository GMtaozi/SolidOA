<template>
  <div class="contact-container">
    <el-card class="card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span class="title-icon">⬡</span>
            <span>通讯录</span>
            <span>CONTACT</span>
          </div>
          <div class="search-box">
            <el-input
              v-model="keyword"
              placeholder="搜索姓名、手机号、职位"
              class="input"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button class="btn" @click="handleSearch">
                  <span class="btn-text">搜索</span>
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </template>

      <div class="contact-list">
        <div
          v-for="item in tableData"
          :key="item.id"
          class="contact-card"
        >
          <div class="avatar-wrapper">
            <div class="avatar-glow"></div>
            <div class="avatar" :style="{ background: getAvatarGradient(item.realName) }">
              {{ item.realName?.charAt(0) || '?' }}
            </div>
            <div class="avatar-ring"></div>
          </div>
          <div class="contact-info">
            <div class="contact-name">{{ item.realName }}</div>
            <div class="contact-detail">
              <span class="dept">{{ item.deptName }}</span>
              <span class="position">{{ item.position }}</span>
            </div>
            <div class="contact-meta">
              <span class="meta-item">
                <span class="meta-icon">✉</span>
                {{ item.mobile }}
              </span>
              <span class="meta-item">
                <span class="meta-icon">◎</span>
                {{ item.email }}
              </span>
            </div>
          </div>
          <div class="contact-actions">
            <button class="action-btn call-btn" @click="handleCall(item)">
              <span class="btn-glow"></span>
              <span class="btn-text">拨号</span>
            </button>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          class="pagination"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { systemApi } from '@/api'

const keyword = ref('')
const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})
const loading = ref(false)

const getAvatarGradient = (name) => {
  const colors = [
    'linear-gradient(135deg, #60A5FA 0%, #A78BFA 100%)',
    'linear-gradient(135deg, #A78BFA 0%, #60A5FA 100%)',
    'linear-gradient(135deg, #34D399 0%, #60A5FA 100%)',
    'linear-gradient(135deg, #FBBF24 0%, #FCA5A5 100%)'
  ]
  const index = name ? name.charCodeAt(0) % colors.length : 0
  return colors[index]
}

// 加载通讯录列表
const fetchContacts = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }
    const res = await systemApi.searchContacts(params.keyword || '')
    if (res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
    } else {
      tableData.value = res.records || res.list || []
      pagination.total = res.total || 0
    }
  } catch (error) {
    console.error('获取通讯录失败:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchContacts()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  fetchContacts()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchContacts()
}

const handleCall = (row) => {
  if (row.mobile) {
    window.location.href = `tel:${row.mobile}`
  } else {
    console.warn('该联系人没有手机号')
  }
}

onMounted(() => {
  fetchContacts()
})
</script>

<style scoped lang="scss">
// 柔和舒适风格配色变量
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.contact-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;

  .card {
    background: $bg-card;
    border-radius: 16px;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
    transition: box-shadow 0.3s ease;

    &:hover {
      box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
    }

    :deep(.el-card__header) {
      border-bottom: 1px solid $border-color;
      padding: 16px 20px;
    }

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: $text-primary;

      .title-icon {
        color: $primary;
        font-size: 20px;
      }
    }
  }

  .search-box {
    :deep(.input) {
      .el-input__wrapper {
        background: $bg-card;
        border: 1px solid $border-color;
        border-radius: 12px;
        box-shadow: none;
        padding: 8px 16px;
        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

        &:hover, &:focus {
          border-color: $primary;
          box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.1);
        }
      }

      .el-input__inner {
        color: $text-primary;

        &::placeholder {
          color: $text-secondary;
        }
      }
    }

    :deep(.btn) {
      background: $primary;
      border: 1px solid $primary;
      border-radius: 0 12px 12px 0;
      color: #ffffff;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

      &:hover {
        background: darken($primary, 5%);
        border-color: darken($primary, 5%);
      }
    }
  }

  .contact-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 16px;
  }

  .contact-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: $bg-primary;
    border-radius: 16px;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      background: #fcfaf7;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    .avatar-wrapper {
      position: relative;
      width: 56px;
      height: 56px;

      .avatar-glow {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 60px;
        height: 60px;
        background: radial-gradient(circle, rgba(96, 165, 250, 0.25), transparent 70%);
        border-radius: 50%;
      }

      .avatar {
        position: relative;
        width: 56px;
        height: 56px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        font-weight: 600;
        color: #ffffff;
        background: linear-gradient(135deg, $primary, lighten($primary, 20%));
      }

      .avatar-ring {
        position: absolute;
        top: -3px;
        left: -3px;
        right: -3px;
        bottom: -3px;
        border: 2px solid transparent;
        border-radius: 50%;
        background: linear-gradient(135deg, $primary, lighten($primary, 20%)) border-box;
        -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
        mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
        -webkit-mask-composite: xor;
        mask-composite: exclude;
      }
    }

    .contact-info {
      flex: 1;
      min-width: 0;

      .contact-name {
        font-size: 16px;
        font-weight: 600;
        color: $text-primary;
        margin-bottom: 4px;
      }

      .contact-detail {
        display: flex;
        gap: 8px;
        margin-bottom: 6px;

        .dept, .position {
          font-size: 12px;
          padding: 2px 8px;
          border-radius: 12px;
          background: rgba(96, 165, 250, 0.1);
          color: $primary;
        }

        .position {
          background: rgba(52, 211, 153, 0.1);
          color: $success;
        }
      }

      .contact-meta {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .meta-item {
          font-size: 12px;
          color: $text-secondary;
          display: flex;
          align-items: center;
          gap: 4px;

          .meta-icon {
            color: $primary;
            font-size: 10px;
          }
        }
      }
    }

    .contact-actions {
      .action-btn {
        padding: 8px 16px;
        background: $bg-card;
        border: 1px solid $border-color;
        border-radius: 40px;
        color: $primary;
        font-size: 12px;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

        .btn-glow {
          display: none;
        }

        .btn-text {
          position: relative;
          z-index: 1;
        }

        &:hover {
          background: $primary;
          color: #ffffff;
          border-color: $primary;
          box-shadow: 0 4px 12px rgba(96, 165, 250, 0.25);
        }
      }
    }
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: center;

    :deep(.pagination) {
      .el-pager li {
        background: $bg-card;
        border: 1px solid $border-color;
        color: $text-secondary;
        border-radius: 12px;
        margin: 0 4px;
        min-width: 36px;
        height: 36px;
        line-height: 36px;
        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

        &:hover {
          color: $primary;
          border-color: $primary;
        }

        &.is-active {
          background: $primary;
          border-color: $primary;
          color: #ffffff;
          box-shadow: 0 4px 12px rgba(96, 165, 250, 0.3);
        }
      }

      .el-pagination__total {
        color: $text-secondary;
      }
    }
  }
}
</style>