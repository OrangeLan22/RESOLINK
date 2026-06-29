<template>
  <div class="no-permission-container">
    <div class="no-permission-content">
      <el-icon size="64" color="#f56c6c">
        <Lock />
      </el-icon>
      <h2>权限错误</h2>
      <p>您没有权限查看此页面的内容，请联系管理员获取相应权限。</p>
    </div>
  </div>
</template>

<script setup>
import { Lock } from '@element-plus/icons-vue'
</script>

<style scoped>
.no-permission-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  text-align: center;
}

.no-permission-content {
  max-width: 500px;
  padding: 40px;
  border-radius: 8px;
  background-color: #fafafa;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.no-permission-content h2 {
  margin: 20px 0 10px 0;
  color: #f56c6c;
  font-size: 24px;
  font-weight: 600;
}

.no-permission-content p {
  color: #666;
  font-size: 16px;
  line-height: 1.5;
  margin: 0;
}
</style>