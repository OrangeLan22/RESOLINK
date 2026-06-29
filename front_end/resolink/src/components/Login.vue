<template>
  <div class="login-container">
    <div class="background">
      <span class="title">
        RESOLINK
      </span>
    </div>
    <div class="card">
      <h2>登 录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required>
        </div>
        <button type="submit" class="btn" :disabled="isLoggingIn">{{ isLoggingIn ? '登录中...' : '登录' }}</button>
      </form>
    </div>
    
    <!-- 浮窗提示 -->
    <div v-if="showToast" :class="['toast', toastType]">
      <i :class="toastIcon"></i>
      <span>{{ toastMessage }}</span>
    </div>
  </div>
</template>

<script>
// 引入JSEncrypt库用于RSA加密
import JSEncrypt from 'jsencrypt';
// 引入公共信息store
import { usePublicInfoStore } from '../stores/publicInfo';
// 引入用户信息store
import { useUserInfoStore } from '../stores/userInfo';
// 引入token store
import { useTokenStore } from '../stores/token';

export default {
  name: 'Login',
  data() {
    return {
      username: '',
      password: '',
      publicKey: '',
      clientId: '',
      isLoggingIn: false,
      // 浮窗提示状态
      showToast: false,
      toastMessage: '',
      toastType: 'success', // success, failure, error
      toastIcon: 'icon-success'
    };
  },
  methods: {
    // 获取公钥和客户端ID
    async getPublicKey() {
      try {
        const response = await fetch('http://localhost:2307/api/encryption/public-key');
        const data = await response.json();
        if (data.success) {
          this.publicKey = data.publicKey;
          this.clientId = data.clientId;
        } else {
          throw new Error(data.message);
        }
      } catch (error) {
        throw new Error('网络错误，无法获取公钥');
      }
    },

    // RSA加密函数
    encryptPassword(password, publicKey) {
      const encrypt = new JSEncrypt();
      encrypt.setPublicKey(publicKey);
      return encrypt.encrypt(password);
    },

    // 显示浮窗提示
    displayToast(message, type = 'success') {
      this.toastMessage = message;
      this.toastType = type;
      
      // 设置图标
      switch(type) {
        case 'success':
          this.toastIcon = 'icon-success';
          break;
        case 'failure':
          this.toastIcon = 'icon-failure';
          break;
        case 'error':
          this.toastIcon = 'icon-error';
          break;
        default:
          this.toastIcon = 'icon-success';
      }
      
      this.showToast = true;
      
      // 3秒后自动隐藏
      setTimeout(() => {
        this.showToast = false;
      }, 3000);
    },

    // 登录表单提交事件处理
    async handleLogin() {
      // 验证输入
      if (!this.username || !this.password) {
        this.displayToast('请输入用户名和密码', 'failure');
        return;
      }

      this.isLoggingIn = true;
      
      try {
        // 点击登录时获取公钥
        await this.getPublicKey();
        
        // 加密密码
        const encryptedPassword = this.encryptPassword(this.password, this.publicKey);

        if (!encryptedPassword) {
          this.displayToast('密码加密失败，请重试', 'error');
          return;
        }

        // 构建登录请求数据
        const loginData = {
          username: this.username,
          password: encryptedPassword,
          clientId: this.clientId
        };

        // 发送登录请求
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(loginData)
        });

        const data = await response.json();
        if (data.success) {
          this.displayToast('登录成功，即将跳转至主页面', 'success');
          
          // 将token和用户信息分别存储到不同的store中
          const userInfoStore = useUserInfoStore();
          const tokenStore = useTokenStore();
          
          tokenStore.setToken(data.token);
          // 只传递用户信息给setUserInfo方法，避免传递token
          userInfoStore.setUserInfo({ userInfo: data.userInfo });
          
          // 获取公共信息并保存到Pinia
          const publicInfoStore = usePublicInfoStore();
          try {
            await publicInfoStore.fetchPublicInfo();
            setTimeout(() => {
              this.$parent.checkLoginStatus();
            }, 1500);
          } catch (error) {
            this.displayToast('获取公共信息失败：' + error.message, 'error');
            // 即使获取公共信息失败，仍允许用户登录
            setTimeout(() => {
              this.$parent.checkLoginStatus();
            }, 1500);
          }
        } else {
          this.displayToast('登录失败：' + data.message, 'failure');
          console.error(data.message);
        }
      } catch (error) {
        this.displayToast('登录失败：' + error.message, 'error');
        console.error('登录失败：', error);
        console.error()
      } finally {
        this.isLoggingIn = false;
      }
    }
  }
};
</script>

<style scoped>
* {
  user-select: none;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.background {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 100vw;
  text-align: center;
  z-index: 1;
}

.title {
  font-size: 15vw;
  font-weight: bold;
  color: transparent;
  -webkit-text-fill-color: transparent;
  -webkit-text-stroke-width: 2px;
  -webkit-text-stroke-color: #999;
  width: 100%;
  white-space: nowrap;
  font-style: italic;
}

.login-container {
  overflow: hidden;
  font-family: 'Arial', sans-serif;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 2rem;
  width: 100%;
  height: 100vh;
}

.login-container h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
}

.card {
  display: flex;
  flex-direction: column;
  background-color: rgba(255, 255, 255, 0);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  border-radius: 8px;
  padding: 2rem;
  width: 400px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  z-index: 2;
  }



.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #555;
  font-weight: 500;
}

.form-group input {
  background-color: rgba(255, 255, 255, 0);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-group input:focus {
  outline: none;
  border-color: #3f51b5;
  box-shadow: 0 0 0 2px rgba(63, 81, 181, 0.2);
}

/* 阻止谷歌浏览器自动填充样式 */
.form-group input:-webkit-autofill,
.form-group input:-webkit-autofill:hover,
.form-group input:-webkit-autofill:focus,
.form-group input:-webkit-autofill:active {
  /* 使用与正常输入框相同的背景色 */
  -webkit-background-clip: text;
  -webkit-text-fill-color: #333;
  /* 设置与正常输入框相同的背景色 */
  background-color: rgba(255, 255, 255, 0) !important;
  backdrop-filter: blur(5px) !important;
  -webkit-backdrop-filter: blur(5px) !important;
  /* 清除默认的自动填充背景 */
  box-shadow: 0 0 0 30px rgba(255, 255, 255, 0) inset !important;
  transition: background-color 5000s ease-in-out 0s !important;
}

.btn {
  width: 100%;
  padding: 0.75rem;
  background-color: rgba(63, 81, 181, 0.6);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn:hover {
  background-color:  #3f51b5;
}

.btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* 浮窗提示样式 */
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 24px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
}

.toast.success {
  background-color: rgba(46, 204, 113, 0.9);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.toast.failure {
  background-color: rgba(231, 76, 60, 0.9);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.toast.error {
  background-color: rgba(243, 156, 18, 0.9);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.toast i {
  margin-right: 8px;
  font-size: 16px;
}

/* 动画效果 */
@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 图标样式 */
.icon-success::before {
  content: "✓";
  font-weight: bold;
}

.icon-failure::before {
  content: "✗";
  font-weight: bold;
}

.icon-error::before {
  content: "!";
  font-weight: bold;
}
</style>