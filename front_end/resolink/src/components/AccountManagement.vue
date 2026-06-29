<!--suppress ALL -->
<template>
  <div class="tab-container scrollbar">

    <!-- 无权限提示页面 -->
    <NoPermission v-if="!hasPermission" />
    
    <!-- 正常内容页面 -->
    <div v-else class="tab-main flex">
      <div class="table">

        <div class="top-tools">
          <div class="search-container">
            <input type="text" v-model="searchKeyword" placeholder="通过姓名或工号查找" @blur="handleSearchBlur">
            <button @click="handleSearch">搜索</button>
          </div>
          <div class="button-container">
<!--            <button class="new-account-btn">权限查询</button>-->
            <button class="new-account-btn" @click="handleCreateAccount">创建账号</button>
          </div>
        </div>

        <!-- 表格 -->
        <el-table :data="loading ? Array(pageSize).fill({}) : currentPageData" style="width: 100%" fit max-height="calc(100vh - 242px)">
          <el-table-column prop="name" label="姓名" fixed min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="emp_id" label="工号" min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.emp_id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="useracc" label="账号" min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.useracc }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="dep_name" label="部门" min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.dep_name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sta_name" label="身份" min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.sta_name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="auth_tag" label="权限" min-width="120">
            <template #default="scope">
              <el-skeleton v-if="loading" animated :rows="1" :throttle="200" >
                <el-skeleton-item variant="text" style="width: 100px" />
              </el-skeleton>
              <span v-else>{{ scope.row.auth_tag }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="200">
            <template #default="scope">
              <div class="button-container">
                <el-button size="small" :disabled="loading" @click="handleEdit(scope.row)">
                  编辑
                </el-button>
                <el-button size="small" :disabled="loading" @click="handleResetPassword(scope.row)">
                  重置
                </el-button>
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

      <!-- 弹窗容器 -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        :width="dialogWidth"
        :show-close="true"
        :close-on-click-modal="false"
        style="max-width: 500px;"
      >
        <!-- 弹窗内容区域，动态渲染不同组件 -->
        <div class="dialog-content">
          <component 
            :is="currentComponent" 
            v-bind="currentComponent === EditAccount ? { editData } : {}" 
            @success="handleDialogSuccess" 
            @cancel="closeDialog" 
          />
        </div>
      </el-dialog>
      
      <!-- 重置密码成功弹窗 -->
      <SuccessDialog 
        :visible="resetPasswordDialogVisible"
        :useracc="resetPasswordData.useracc"
        :password="resetPasswordData.password"
        :message="`用户 <strong>${resetPasswordData.useracc}</strong> 的新密码为`"
        @close="closeResetPasswordDialog"
      />
    </div>
  </div>
</template>


<script setup>
import { ref, computed, shallowRef, onMounted, watch } from 'vue';
import { ElTable, ElTableColumn, ElButton, ElPagination, ElMessage, ElSkeleton, ElSkeletonItem, ElMessageBox, ElLoading } from 'element-plus';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import CreateAccount from './AccountManagement/CreateAccount.vue';
import EditAccount from './AccountManagement/EditAccount.vue';
import SuccessDialog from './Common/SuccessDialog.vue';
import NoPermission from './Common/NoPermission.vue';
import { usePublicInfoStore } from '../stores/publicInfo';
import { useAccountsStore } from '../stores/accounts';

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()
// 获取账户store实例
const accountsStore = useAccountsStore()

// 分页相关
const currentPage = ref(1);
const pageSize = ref(15);

// 搜索相关
const searchKeyword = ref(''); // 搜索关键词
const searchTimer = ref(null); // 搜索定时器
const isSearching = ref(false); // 是否正在搜索
const currentSearch = ref(''); // 当前搜索关键词（仅在点击搜索按钮时更新）

// 重置密码相关
const resetPasswordDialogVisible = ref(false);
const resetPasswordData = ref({ useracc: '', password: '' });

// 编辑相关
const isEditMode = ref(false);
const editData = ref(null);

// 从store获取数据
const accounts = computed(() => accountsStore.accounts);
const total = computed(() => accountsStore.total);
const loading = computed(() => accountsStore.loading);
const error = computed(() => accountsStore.error);
const hasPermission = computed(() => accountsStore.$state.hasPermission);

// 当前页数据直接从store获取，因为分页由后端处理
const currentPageData = computed(() => accounts.value);

// 获取账户数据的函数
const fetchAccounts = async () => {
  try {
    if (isSearching.value && currentSearch.value) {
      // 直接实现搜索API调用
      accountsStore.loading = true
      
      // 获取token并去除引号和前后空格
      let token = localStorage.getItem('token') || ''
      token = token.replace(/^['"]|['"]$/g, '').trim()
      
      // 调用API搜索账户
      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/account-mgm/search-accounts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8'
        },
        body: JSON.stringify({
          token: token,
          data: {
            search: currentSearch.value,
            size: pageSize.value,
            page: currentPage.value
          }
        })
      })

      // 处理403权限错误
      if (response.status === 403) {
        accountsStore.$state.hasPermission = false
        throw new Error('没有权限访问账号信息')
      }
      
      if (!response.ok) {
        throw new Error('搜索账号失败')
      }
      
      const data = await response.json()
      if (data.success) {
        accountsStore.accounts = data.data || []
        accountsStore.total = data.total || 0
        accountsStore.totalPages = data.totalPages || 0
        // 成功获取数据，设置为有权限
        accountsStore.$state.hasPermission = true
      } else {
        throw new Error(data.message || '搜索账号失败')
      }
    } else {
      // 调用原始API
      await accountsStore.fetchAccounts(currentPage.value, pageSize.value);
    }
  } catch (err) {
    ElMessage.error(err.message);
  } finally {
    accountsStore.loading = false
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
  fetchAccounts();
};

// 清空搜索状态函数
const clearSearch = () => {
  currentSearch.value = '';
  isSearching.value = false;
  currentPage.value = 1;
  fetchAccounts();
};

// 分页事件
const handleSizeChange = (val) => {
  pageSize.value = val;
  currentPage.value = 1; // 切换每页条数时重置到第一页
  fetchAccounts(); // 获取新数据
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchAccounts(); // 获取新数据
};

// 组件挂载时获取初始数据
onMounted(() => {
  fetchAccounts();
});

// 弹窗相关
const dialogVisible = ref(false);
const dialogTitle = ref('');
const dialogWidth = ref('50%');
const currentComponent = shallowRef(null);
let componentUnmountTimer = null; // 保存组件卸载的定时器ID

// 打开弹窗方法
const openDialog = async (title, component, width = '50%') => {
  dialogTitle.value = title;
  dialogWidth.value = width;
  
  // 清除任何未执行的组件卸载定时器（解决快速关闭打开的问题）
  if (componentUnmountTimer) {
    clearTimeout(componentUnmountTimer);
    componentUnmountTimer = null;
  }
  
  // 如果是创建账号或编辑账号组件，先获取必要的数据
  if (component === CreateAccount || component === EditAccount) {
    // 显示全局loading
    const loading = ElLoading.service({
      lock: true,
      text: '',
      background: 'rgba(0,0,0,0.3)'
    });
    
    try {
      // 先获取最新的部门和权限信息
      await Promise.all([
        publicInfoStore.fetchDepartmentsWithStatuses(),
        publicInfoStore.fetchAuthorities()
      ]);
      
      // 数据获取成功后，渲染组件
      currentComponent.value = component;
      dialogVisible.value = true;
    } catch (error) {
      ElMessage.error('获取数据失败，请重试');
      // 数据获取失败，不显示组件
      currentComponent.value = null;
      dialogVisible.value = false;
    } finally {
      // 关闭loading
      loading.close();
    }
  } else {
    // 其他组件直接显示
    currentComponent.value = component;
    dialogVisible.value = true;
  }
};

// 关闭弹窗方法
const closeDialog = () => {
  // 关闭弹窗
  dialogVisible.value = false;
};

// 监听弹窗可见性变化
watch(() => dialogVisible.value, (newVisible) => {
  if (!newVisible) {
    // 清除任何未执行的定时器
    if (componentUnmountTimer) {
      clearTimeout(componentUnmountTimer);
    }
    
    // 延迟卸载组件，确保动画完整
    componentUnmountTimer = setTimeout(() => {
      // 重置编辑模式和数据
      isEditMode.value = false;
      editData.value = null;
      
      // 卸载组件
      currentComponent.value = null;
      componentUnmountTimer = null; // 清除定时器引用
    }, 300); // 与弹窗动画时间一致
  }
});

// 处理弹窗成功事件
const handleDialogSuccess = (updatedData) => {
  // 检查是否为无修改情况
  if (updatedData && updatedData.noChanges) {
    // 直接关闭弹窗，不进行任何数据操作
    closeDialog();
    return;
  }
  
  if (updatedData && updatedData.length > 0) {
    // 如果有更新后的数据，直接在本地更新，避免重新请求API
    const updatedAccount = updatedData[0];
    const index = accountsStore.accounts.findIndex(acc => acc.id === updatedAccount.id);
    if (index !== -1) {
      // 更新本地账号列表中的对应数据
      accountsStore.accounts[index] = {
        ...accountsStore.accounts[index],
        ...updatedAccount
      };
    }
  } else {
    // 没有更新数据或不是编辑操作，重新获取完整列表
    fetchAccounts();
  }
  // 关闭弹窗
  closeDialog();
};

// 创建账号按钮点击事件
const handleCreateAccount = () => {
  // 设置为创建模式
  isEditMode.value = false
  editData.value = null
  // 打开弹窗
  openDialog('创建账号', CreateAccount);
};

// 操作方法
const handleDetail = (row) => {
  console.log('查看详细信息:', row);
  // 这里可以添加查看详细信息的逻辑
};

// 编辑账户处理函数
const handleEdit = (row) => {
  // 设置编辑模式
  isEditMode.value = true
  // 传递编辑数据
  editData.value = row
  // 打开弹窗
  openDialog('编辑账号', EditAccount)
};

const handlePermission = (row) => {
  console.log('权限管理:', row);
  // 这里可以添加权限管理的逻辑
};

// 关闭重置密码成功弹窗
const closeResetPasswordDialog = () => {
  resetPasswordDialogVisible.value = false
};

// 重置密码处理函数
const handleResetPassword = async (row) => {
  let resetLoading = null;
  try {
    // 显示确认对话框
    await ElMessageBox.confirm(
      `确定要重置账号 <strong>${row.useracc}</strong> 的密码吗？`,
      '重置密码确认',
      {
        confirmButtonText: '确定',
        confirmButtonType: "danger",
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    );
    
    // 显示提交loading
    resetLoading = ElLoading.service({
      lock: true,
      text: '',
      background: 'rgba(0,0,0,0.3)'
    });
    
    // 获取token并去除引号和前后空格
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    // 调用API重置密码
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/account-mgm/reset-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
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
      throw new Error('权限错误')
    }
    
    if (!response.ok) {
      throw new Error('重置密码失败')
    }
    
    const result = await response.json()
    
    if (result.success) {
      // 设置成功数据并显示弹窗
      resetPasswordData.value = result.data
      resetPasswordDialogVisible.value = true
      // 刷新账号列表
      fetchAccounts()
    } else {
      ElMessage.error(result.message || '重置密码失败')
    }
  } catch (error) {
    // 如果是用户取消操作，不显示错误信息
    if (error !== 'cancel' && error.name !== 'CanceledError' && error.message !== 'cancel') {
      ElMessage.error(error.message || '重置密码失败')
    }
  } finally {
    // 关闭loading
    if (resetLoading) {
      resetLoading.close()
    }
  }
};

const handleDelete = async (row) => {
  try {
    // 显示确认对话框
    await ElMessageBox.confirm(
      `确定要删除账号 <strong>${row.useracc}</strong> 吗？此操作不可逆！`,
      '删除确认',
      {
        confirmButtonText: '确定',
        confirmButtonType: "danger",
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    );
    
    // 获取token并去除引号和前后空格
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    // 调用API删除账号
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/account-mgm/delete-account`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
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
      throw new Error('权限错误')
    }
    
    if (!response.ok) {
      throw new Error('删除账号失败')
    }
    
    const result = await response.json()
    
    if (result.success) {
      ElMessage.success('删除账号成功')
      // 刷新账号列表
      fetchAccounts()
    } else {
      ElMessage.error(result.message || '删除账号失败')
    }
  } catch (error) {
    // 如果是用户取消操作，不显示错误信息
    if (error !== 'cancel' && error.name !== 'CanceledError' && error.message !== 'cancel') {
      ElMessage.error(error.message || '删除账号失败')
    }
  }
};
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

.new-account-btn {
  padding: 5px 10px;
  background-color: #3f51b5;
  height: 100%;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 10px;
}
.new-account-btn:active {
  transform: scale(0.95);
}

/* 确保固定列与表格容器右侧完全对齐 */
:deep(.el-table__fixed-right) {
  right: 0 !important;
  width: 325px !important;
}

/* 确保表格主体与固定列对齐 */
:deep(.el-table__body-wrapper) {
  margin-right: 325px;
}

/* 确保表头与固定列对齐 */
:deep(.el-table__header-wrapper) {
  margin-right: 325px;
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

.dialog-content {
  display: flex;
  justify-items: center;
  justify-content: center;
}

:deep(.cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.el-table td.el-table__cell div) {
  display: flex;
  align-items: center;
}

:deep(.is-first) {
  display: none !important;
}

:deep(.el-skeleton__paragraph) {
  margin-top: 0 !important;
}
</style>