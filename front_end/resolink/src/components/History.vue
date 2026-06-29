<template>
  <div class="tab-container scrollbar">
    
    <!-- 无权限提示页面 -->
    <NoPermission v-if="!hasPermission" />
    
    <!-- 正常内容页面 -->
    <div v-else class="tab-main flex">
      <div class="table">
        <div class="top-tools">
          <div class="search-container">
            <input type="text" v-model="searchKeyword" placeholder="通过预约人或工号查找" @blur="handleSearchBlur">
            <button @click="handleSearch">搜索</button>
          </div>
        </div>

        <!-- 表格 -->
        <el-table :data="loading ? Array(pageSize).fill({}) : currentPageData" style="width: 100%" fit max-height="calc(100vh - 242px)">
          <el-table-column prop="name" label="预约人" fixed min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="empId" label="工号" min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.empId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="resName" label="资源" min-width="150">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 120px" />
              </el-skeleton>
              <span v-else>{{ scope.row.resName }}#{{ scope.row.resId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" min-width="150">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 120px" />
              </el-skeleton>
              <span v-else>{{ formatTimestamp(scope.row.startTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="endTime" label="结束时间" min-width="150">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 120px" />
              </el-skeleton>
              <span v-else>{{ formatTimestamp(scope.row.endTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="100">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 80px" />
              </el-skeleton>
              <el-tag v-else :type="getStatusType(scope.row.status, scope.row.startTime, scope.row.endTime)">
                {{ getStatusText(scope.row.status, scope.row.startTime, scope.row.endTime) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="150">
            <template #default="scope">
              <div class="button-container">
<!--                <el-button size="small" :disabled="loading" @click="handleDetail(scope.row)">-->
<!--                  详情-->
<!--                </el-button>-->
                <el-button type="danger" size="small" :disabled="loading" @click="handleDelete(scope.row)" plain>
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="currentPage"
          :page-sizes="[5, 15, 30, 50]"
          :page-size="pageSize"
          :pager-count="5"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :prev-icon="ArrowLeft"
          :next-icon="ArrowRight"
          :page-size-text="'每页显示'"
          :total-text="(total) => `共 ${total} 条数据`"
        />
        <p class="total-count">共 {{ total }} 条数据</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElTable, ElTableColumn, ElButton, ElPagination, ElMessage, ElSkeleton, ElSkeletonItem, ElMessageBox, ElTag, ElLoading } from 'element-plus';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import NoPermission from './Common/NoPermission.vue';

// 分页相关
const currentPage = ref(1);
const pageSize = ref(15);
const total = ref(0);

// 搜索相关
const searchKeyword = ref('');
const searchTimer = ref(null);
const isSearching = ref(false);
const currentSearch = ref('');

// 加载状态
const loading = ref(false);

// 权限状态
const hasPermission = ref(true);

// 错误消息
const errorMessage = ref('');

// 历史记录数据
const historyData = ref([]);

// 当前页数据
const currentPageData = computed(() => {
  return historyData.value;
});

// 格式化时间戳
const formatTimestamp = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp * 1000);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 获取状态文本
const getStatusText = (status, startTime, endTime) => {
  switch (status) {
    case '-1':
      return '已取消';
    case '0':
      // 根据当前时间与预约时间的比较动态判断状态
      const currentTime = Math.floor(Date.now() / 1000); // 当前时间戳（秒）
      
      if (currentTime < startTime) {
        return '未开始';
      } else if (currentTime >= startTime && currentTime <= endTime) {
        return '进行中';
      } else {
        return '已结束';
      }
    default:
      return '未知';
  }
};

// 获取状态标签类型
const getStatusType = (status, startTime, endTime) => {
  switch (status) {
    case '-1':
      return 'info';
    case '0':
      // 根据当前时间与预约时间的比较动态判断标签类型
      const currentTime = Math.floor(Date.now() / 1000); // 当前时间戳（秒）
      
      if (currentTime < startTime) {
        return 'primary'; // 未开始 - 蓝色
      } else if (currentTime >= startTime && currentTime <= endTime) {
        return 'warning'; // 进行中 - 橙色
      } else {
        return 'success'; // 已结束 - 绿色
      }
    default:
      return 'danger';
  }
};

// 获取历史记录数据
const fetchHistoryData = async () => {
  try {
    loading.value = true;
    errorMessage.value = '';
    
    // 获取token并去除引号和前后空格
    let token = localStorage.getItem('token') || '';
    token = token.replace(/^['"]|['"]$/g, '').trim();
    
    // 构建请求体
    const requestBody = {
      token: token,
      data: {
        page: currentPage.value,
        size: pageSize.value
      }
    };
    
    // 如果正在搜索，添加搜索关键词
    if (isSearching.value && currentSearch.value) {
      requestBody.data.search = currentSearch.value;
    }
    
    // 调用API获取历史记录
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/history/list`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });

    // 处理403权限错误
    if (response.status === 403) {
      hasPermission.value = false;
      errorMessage.value = 'API返回403: 没有权限访问历史记录';
    }

    if (!response.ok) {
      errorMessage.value = `API返回${response.status}: 获取历史记录失败`;
    }
    
    const result = await response.json();
    
    if (result.success) {
      historyData.value = result.data || [];
      total.value = result.total || 0;
      errorMessage.value = '数据获取成功';
    } else {
      errorMessage.value = `API返回错误: ${result.message || '获取历史记录失败'}`;
      throw new Error(result.message || '获取历史记录失败');
    }
  } catch (error) {
    console.error(error.message || '获取历史记录失败');
  } finally {
    loading.value = false;
  }
};

// 搜索失焦处理函数
const handleSearchBlur = () => {
  // 清除之前的定时器
  if (searchTimer.value) {
    clearTimeout(searchTimer.value);
  }
  
  // 如果输入框为空且当前处于搜索状态，则清空搜索状态
  if (!searchKeyword.value.trim() && isSearching.value) {
    clearSearch();
  }
};

// 搜索按钮点击处理函数
const handleSearch = () => {
  // 清除之前的定时器
  if (searchTimer.value) {
    clearTimeout(searchTimer.value);
  }
  
  // 更新当前搜索关键词
  currentSearch.value = searchKeyword.value.trim();
  
  // 如果搜索关键词为空，则不执行任何操作
  if (!currentSearch.value) {
    return;
  }
  
  // 重置到第一页
  currentPage.value = 1;
  
  // 更新搜索状态
  isSearching.value = true;
  
  // 获取数据
  fetchHistoryData();
};

// 清空搜索状态函数
const clearSearch = () => {
  currentSearch.value = '';
  isSearching.value = false;
  currentPage.value = 1;
  fetchHistoryData();
};

// 分页事件
const handleSizeChange = (val) => {
  pageSize.value = val;
  currentPage.value = 1; // 切换每页条数时重置到第一页
  fetchHistoryData(); // 获取新数据
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchHistoryData(); // 获取新数据
};

// 详情按钮点击事件
const handleDetail = (row) => {
  ElMessage.info(`查看预约详情: ${row.resName}#${row.resId} - ${row.name}`);
  // 这里可以添加查看详情的逻辑，比如打开详情弹窗
};

// 删除按钮点击事件
const handleDelete = async (row) => {
  try {
    // 显示确认对话框
    await ElMessageBox.confirm(
      `确定要删除预约记录 <strong>${row.resName}#${row.resId}</strong> 吗？此操作不可逆！`,
      '删除确认',
      {
        confirmButtonText: '确定',
        confirmButtonType: "danger",
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    );
    
    // 显示提交loading
    const deleteLoading = ElLoading.service({
      lock: true,
      text: '',
      background: 'rgba(0,0,0,0.3)'
    });
    
    // 获取token并去除引号和前后空格
    let token = localStorage.getItem('token') || '';
    token = token.replace(/^['"]|['"]$/g, '').trim();
    
    // 调用API删除预约记录
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/history/delete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: token,
        data: {
          id: row.id
        }
      })
    });
    
    // 处理403权限错误
    if (response.status === 403) {
      hasPermission.value = false;
      throw new Error('没有权限删除历史记录');
    }
    
    if (!response.ok) {
      throw new Error('删除预约记录失败');
    }
    
    const result = await response.json();
    
    if (result.success) {
      ElMessage.success('删除预约记录成功');
      // 刷新历史记录列表
      fetchHistoryData();
    } else {
      throw new Error(result.message || '删除预约记录失败');
    }
  } catch (error) {
    // 如果是用户取消操作，不显示错误信息
    if (error !== 'cancel' && error.name !== 'CanceledError' && error.message !== 'cancel') {
      ElMessage.error(error.message || '删除预约记录失败');
    }
  }
};

// 组件挂载时获取初始数据
onMounted(() => {
  fetchHistoryData();
});

// 刷新方法
const refresh = () => {
  fetchHistoryData();
};

// 导出刷新方法供父组件调用
defineExpose({
  refresh
});
</script>

<style scoped>
* {
  user-select: none;
}
.flex {
  display: flex;
}

.tab-main {
  max-height: calc(100vh - 155.667px);
  flex-direction: column;
}

.table {
  width: 100%;
  overflow: hidden;
}

.button-container {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  overflow: hidden;
  white-space: nowrap;
}

/* 确保固定列与表格容器右侧完全对齐 */
:deep(.el-table__fixed-right) {
  right: 0 !important;
  width: 175px !important;
}

/* 确保表格主体与固定列对齐 */
:deep(.el-table__body-wrapper) {
  margin-right: 175px;
}

/* 确保表头与固定列对齐 */
:deep(.el-table__header-wrapper) {
  margin-right: 175px;
}

.top-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.search-container {
  border: 1px solid #ccc;
  width: auto;
  height: 30px;
  border-radius: 50px;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  transition: box-shadow 0.1s ease;
}

.search-container:focus-within {
  box-shadow: 0 0 0 1px #3f51b5 inset;
}

.search-container input {
  width: auto;
  min-width: 100px;
  height: 100%;
  background: none;
  border: none;
  outline: none;
  padding: 0 10px;
}

.search-container input::placeholder {
  position: relative;
  top: 1px;
}

.search-container button {
  width: auto;
  height: 100%;
  background: #3f51b5;
  color: white;
  border: none;
  border-radius: 40px;
  padding: 5px 15px;
  margin-left: 5px;
  cursor: pointer;
}

.search-container button:active {
  background: #303f9f;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  width: 100%;
}

.total-count {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

/* 更改已选择页码的背景颜色 */
:deep(.el-pager .is-active) {
  background-color: #3f51b5 !important;
  color: white !important;
}

:deep(.el-pager .number:hover:not(.is-active)) {
  color: #3f51b5 !important;  
}

/* 上下页按钮 */
:deep(.btn-prev),
:deep(.btn-next) {
  border-radius: 50% !important;
}

/* 上下页按钮悬浮时颜色 */
:deep(.btn-prev:hover),
:deep(.btn-next:hover) {
  color: #3f51b5 !important;
}

:deep(.btn-quicknext:hover),
:deep(.btn-quickprev:hover) {
  color: #3f51b5 !important;
}

/* 选择框聚焦时边框颜色 */
:deep(.is-focused) {
  box-shadow: 0 0 0 1px #3f51b5 inset !important;
}
:deep(.is-focus) {
  box-shadow: 0 0 0 1px #3f51b5 inset !important;
}
:deep(.is-first) {
  display: none !important;
}
:deep(.el-table td.el-table__cell div) {
  display: flex;
  align-items: center;
}
:deep(.el-skeleton__paragraph) {
  margin-top: 0 !important;
}

/* 取消状态标签的切换动画 */
:deep(.el-tag) {
  transition: none !important;
  animation: none !important;
}
:deep(.el-tag__content) {
  transition: none !important;
  animation: none !important;
}
</style>