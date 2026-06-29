<template>
  <div class="info">
    <h3>我的信息</h3>
    <table class="info-table">
      <tbody>
        <tr>
          <td class="label">姓名</td>
          <td class="value" id="name">{{userInfo.name}}</td>
        </tr>
        <tr>
          <td class="label">部门/组织</td>
          <td class="value" id="department">{{userInfo.depName || '无部门'}}</td>
        </tr>
        <tr>
          <td class="label">工号</td>
          <td class="value" id="employeeId">{{userInfo.empId}}</td>
        </tr>
        <tr>
          <td class="label">身份</td>
          <td class="value" id="tag">{{userInfo.staName || '无身份'}}</td>
        </tr>
      </tbody>
    </table>
    <div class="buttons">
      <button class="btn" @click="handleAccountSettings">账号设置</button>
<!--      <button class="btn">修改密码</button>-->
      <button class="btn" id="logout-btn" @click="logout()">退出登录</button>
    </div>

    <!-- 账号设置弹窗 -->
    <AccountSettings v-model:visible="accountSettingsVisible" />
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useUserInfoStore } from '../../stores/userInfo';
import { useTokenStore } from '../../stores/token';
import { usePublicInfoStore } from '../../stores/publicInfo';
import AccountSettings from './AccountSettings.vue';

// 获取用户信息store
const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const publicInfoStore = usePublicInfoStore();

// 从store获取用户信息
const userInfo = computed(() => userInfoStore.getUserInfo || {});

// 账号设置弹窗显示状态
const accountSettingsVisible = ref(false);

// 处理账号设置按钮点击
function handleAccountSettings() {
  accountSettingsVisible.value = true;
}



// 退出登录
const logout = () => {
  // 清除所有store的数据
  tokenStore.clearToken();
  userInfoStore.clearUserInfo();
  publicInfoStore.clearPublicInfo();
  
  // 清空所有localStorage数据
  localStorage.clear();
  
  // 刷新页面
  window.location.reload();
}

</script>

<style scoped>
.info {
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 5px;
  transition: box-shadow 0.3s;
}

h3 {
  letter-spacing: 2px;
  margin: 0;
  padding:0 ;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
}

.info-table tr:last-child {
  border-bottom: none;
}

.info-table td {
  padding: 5px 0;
  vertical-align: top;
}

.info-table .label {
  font-weight: 500;
  color: #666;
  width: 80px;
  text-align: left;
  padding-right: 10px;
}

.info-table .value {
  color: #333;
  text-align: left;
}

.buttons {
  display: flex;
  justify-content: center;
}
.btn {
  color: rgba(0, 0, 0, 0.8);
  margin-right: 10px;
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.3s;
  background-color: #f0f0f0;
}
.btn:hover {
  background-color: rgba(63, 81, 181, 0.3);
}
.btn:active {
  background-color: rgba(63, 81, 181, 0.5);
  transform: scale(0.97);
}
.btn[id="logout-btn"] {
  margin-right: 0;
}
.btn:hover[id="logout-btn"] {
  background-color: rgb(255, 77, 79);
  color: #ffffff;
}


</style>