<template>
  <div class="main">
    <div class="none" v-if="appointments.length === 0">
      <el-empty description="暂无事务"></el-empty>
    </div>
    
    <div class="content" v-else>
      <!-- 资源卡片列表 -->
      <div v-for="appointment in appointments" :key="appointment.id" class="card" :id="appointment.resourceType">
        <div class="status" :class="{ 'ongoing': getStatus(appointment) === '进行中', 'upcoming': getStatus(appointment) === '未开始' }">
          {{ getStatus(appointment) }}
        </div>
        <div class="name">
          <h4>{{ appointment.resourceName }}</h4>
        </div>
        <div class="type">
          {{ appointment.location }}
        </div>
        <div class="buttons">
          <button>资源详情</button>
          <button v-if="getStatus(appointment) === '进行中'" @click="releaseAppointment(appointment.id)">提前释放</button>
          <button v-else @click="cancelAppointment(appointment.id)">取消预约</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';

const appointments = ref([]);

// 获取预约状态
const getStatus = (appointment) => {
  const now = Math.floor(Date.now() / 1000);
  if (now >= appointment.startTime && now <= appointment.endTime) {
    return '进行中';
  } else if (now < appointment.startTime) {
    return '未开始';
  } else {
    return '已结束';
  }
};

// 取消预约
const cancelAppointment = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消预约吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const token = localStorage.getItem('token')?.replace(/"/g, '');
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/appointment/cancel-appointment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token,
        data: {
          id
        }
      })
    });
    
    if (response.ok) {
      // 重新获取预约列表以更新UI
      const updateResponse = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/home/appointments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ token })
      });
      
      if (updateResponse.ok) {
        appointments.value = await updateResponse.json();
        ElMessage.success('取消预约成功');
      }
    } else {
      ElMessage.error('取消预约失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('网络错误:', error);
      ElMessage.error('网络错误，请稍后重试');
    }
  }
};

// 提前释放
const releaseAppointment = async (id) => {
  try {
    await ElMessageBox.confirm('确定要提前释放该资源吗？', '提前释放', {
      confirmButtonText: '确定',
      confirmButtonClass: 'el-button--danger',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const token = localStorage.getItem('token')?.replace(/"/g, '');
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/appointment/release-appointment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token,
        data: {
          id
        }
      })
    });
    
    if (response.ok) {
      // 重新获取预约列表以更新UI
      const updateResponse = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/home/appointments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ token })
      });
      
      if (updateResponse.ok) {
        appointments.value = await updateResponse.json();
        ElMessage.success('提前释放成功');
      }
    } else {
      ElMessage.error('提前释放失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('网络错误:', error);
      ElMessage.error('网络错误，请稍后重试');
    }
  }
};

// 组件挂载时获取预约记录
onMounted(async () => {
  try {
    const token = localStorage.getItem('token')?.replace(/"/g, ''); // 假设token存储在localStorage中，移除可能存在的引号
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/home/appointments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ token })
    });
    
    if (response.ok) {
      appointments.value = await response.json();
    } else {
      console.error('获取预约记录失败');
    }
  } catch (error) {
    console.error('网络错误:', error);
  }
});
</script>

<style scoped>
.none {
  display: flex;
  flex-direction: column;
  align-items: center;
  p {
    margin-top: 20px;
    font-size: 15px;
    color: rgba(51, 51, 51, 0.8);
  }
}

.content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  grid-auto-flow: dense;
}

.card {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  transition: box-shadow 0.3s;
}

.card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.name {
  color: #333;
  margin: 0;
  font-size: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  h4 {
    margin: 0;
  }
}

.status {
  width: fit-content;
  margin-bottom: 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
  padding: 2px 6px;
  background-color: rgba(92, 92, 92, 0.35);
}

.status.ongoing {
  background-color: rgba(76, 175, 80, 0.8);
}

.status.upcoming {
  background-color: rgba(143, 143, 143, 0.8);
}

.type {
  margin-bottom: 15px;
  font-size: 14px;
}

.buttons {
  display: flex;
  justify-content: space-between;
  gap: 5px;
  button {
    flex: 1;
    min-width: 80px;
    padding: 5px 10px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  button:active {
    transform: scale(0.95);
  }

  button:hover {
    background-color: #dadada;
  }
  button:nth-child(2) {
    background-color: rgba(63, 81, 181, 0.8);
    color: #fff;
  }
  button:nth-child(2):hover {
    background-color: rgba(63, 81, 181, 1);
  }
}
</style>