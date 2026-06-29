<template>
  <el-dialog
    v-model="dialogVisible"
    title="空间预约"
    width="700px"
    :show-close="true"
    :close-on-click-modal="false"
  >
    <div v-if="props.resource" class="title">
      <span>{{ props.resource.name }}</span>
      <span style="font-size: 18px; font-weight: normal;">
        #{{ props.resource.id }}
      </span>
    </div>
    <el-calendar 
      v-model="calendarDate"
    >
      <template #header>
        <div class="calendar-header">
          <span>{{ calendarDate.getFullYear() }}年{{ calendarDate.getMonth() + 1 }}月</span>
          <el-button-group>
            <el-button
                @click="prevMonth"
                size="small"
                :disabled="isCurrentMonth"
            >上个月</el-button>
            <el-button @click="today" size="small">今天</el-button>
            <el-button @click="nextMonth" size="small">下个月</el-button>
          </el-button-group>
        </div>
      </template>
      <template #date-cell="{ data }">
        <div
          style="padding: 8px"
          @click="handleDateClick(new Date(data.date))"
        >
          <div
            :class="getDateCellClass(data)"
          >
            {{ data.day.split('-')[2] }}
          </div>
        </div>
      </template>
    </el-calendar>
    
    <div class="time-selection" v-if="!isLoading">
      <div class="time-selection-header">
        <h3>选择预约时间</h3>
        <el-switch
          v-model="isCrossDay"
          active-text="跨日"
          inactive-text="单日"
          style="margin-left: 10px"
          @click="handleCrossDayChange(isCrossDay)"
        />
      </div>

      
      <!-- 时间轴滑块 -->
      <div class="time-slider-container">
        <!-- 上午时间轴 -->
        <div class="time-section">
          <div class="slider-wrapper">
            <div class="time-markers">
              <span v-for="hour in 12" :key="hour" class="time-marker">
                {{ (hour - 1).toString().padStart(2, '0') }}
              </span>
              <span class="time-marker" style="color: transparent;">00</span>
            </div>
            <div 
              class="time-track" 
              @mouseup="handleTrackClick($event, 'morning')"
              ref="morningTrack"
            >
              <!-- 不可预约时间段 -->
              <div 
                v-for="(item, index) in morningUnavailableTimes" 
                :key="index"
                class="unavailable-segment"
                :style="{
                  left: `${((new Date(item.start_time * 1000).getHours() * 60 + new Date(item.start_time * 1000).getMinutes()) / (12 * 60)) * 100}%`,
                  width: `${((item.end_time - item.start_time) / (12 * 3600)) * 100}%`
                }"
              ></div>
              
              <!-- 选中时间段 -->
              <div 
                class="selected-segment"
                :style="{
                  left: `${getSelectedSegmentLeft('morning')}%`,
                  width: `${getSelectedSegmentWidth('morning')}%`
                }"
                v-if="isTimeInSection('morning')"
              ></div>
            </div>
          </div>
        </div>
        
        <!-- 下午时间轴 -->
        <div class="time-section">
          <div class="slider-wrapper">

            <div 
              class="time-track" 
              @mouseup="handleTrackClick($event, 'afternoon')"
              ref="afternoonTrack"
            >
              <!-- 不可预约时间段 -->
              <div 
                v-for="(item, index) in afternoonUnavailableTimes" 
                :key="index"
                class="unavailable-segment"
                :style="{
                  left: `${(((new Date(item.start_time * 1000).getHours() - 12) * 60 + new Date(item.start_time * 1000).getMinutes()) / (12 * 60)) * 100}%`,
                  width: `${((item.end_time - item.start_time) / (12 * 3600)) * 100}%`
                }"
                :title="`${formatTime(item.start_time)} - ${formatTime(item.end_time)}`"
              ></div>
              
              <!-- 选中时间段 -->
              <div 
                class="selected-segment"
                :style="{
                  left: `${getSelectedSegmentLeft('afternoon')}%`,
                  width: `${getSelectedSegmentWidth('afternoon')}%`
                }"
                v-if="isTimeInSection('afternoon')"
              ></div>
            </div>
            <div class="time-markers">
              <span v-for="hour in 12" :key="hour" class="time-marker">
                {{ (hour + 11).toString().padStart(2, '0') }}
              </span>
              <span class="time-marker" style="color: transparent;">00</span>
            </div>
          </div>
        </div>
        
        <!-- 全局滑块（动态显示在对应的时间轴上） -->
        <div 
          class="time-handle"
          :style="getGlobalHandleStyle('slider1')"
          @mousedown="startDrag($event, 'slider1')"
          v-if="slider1Time"
        >
          <div class="handle-label">{{ getSliderLabel('slider1') }}</div>
        </div>
        
        <div 
          class="time-handle"
          :style="getGlobalHandleStyle('slider2')"
          @mousedown="startDrag($event, 'slider2')"
          v-if="slider2Time"
        >
          <div class="handle-label">{{ getSliderLabel('slider2') }}</div>
        </div>
      </div>
      
      <!-- 日期选择器（跨日模式） -->
       <div class="date-pickers" v-if="isCrossDay">
         <el-form :model="crossDayForm">
           <div class="date-picker-row">
             <div class="date-group date-picker-item">
               <el-date-picker
                 v-model="crossDayForm.startDate"
                 type="date"
                 format="YYYY-MM-DD"
                 placeholder="选择开始日期"
                 style="width: 100%"
                 :class="{ 'is-error': dateError && dateError.includes('结束日期必须晚于开始日期') }"
               />
               <el-button 
                 class="now-date-btn" 
                 size="small"
                 @click="() => { crossDayForm.startDate = calendarDate; }"
               >当前选择日期</el-button>
             </div>
             <div class="time-picker-divider">—</div>
             <div class="date-group date-picker-item">
               <el-date-picker
                 v-model="crossDayForm.endDate"
                 type="date"
                 format="YYYY-MM-DD"
                 placeholder="选择结束日期"
                 style="width: 100%"
                 :class="{ 'is-error': dateError && dateError.includes('结束日期必须晚于开始日期') }"
               />
               <el-button 
                 class="now-date-btn" 
                 size="small"
                 @click="() => { crossDayForm.endDate = calendarDate; }"
               >当前选择日期</el-button>
             </div>
           </div>
           <div v-if="dateError" class="date-error" style="color: #f56c6c; font-size: 12px; margin-top: 8px; text-align: center;">{{ dateError }}</div>
           <div v-if="dateRangeError" class="date-range-error" style="color: #f56c6c; font-size: 12px; margin-top: 8px; text-align: center;">{{ dateRangeError }}</div>
         </el-form>
       </div>
       
       <!-- 时间选择器 -->
       <div class="time-pickers">
         <el-form :model="formData">
           <div class="time-picker-row">
             <el-form-item class="time-picker-item">
               <el-time-picker
                 v-model="formData.startTime"
                 format="HH:mm"
                 placeholder="选择开始时间"
                 style="width: 100%"
                 @change="handleTimePickerChange"
               />
             </el-form-item>
             <div class="time-picker-divider">—</div>
             <el-form-item class="time-picker-item">
               <el-time-picker
                 v-model="formData.endTime"
                 format="HH:mm"
                 placeholder="选择结束时间"
                 style="width: 100%"
                 @change="handleTimePickerChange"
               />
             </el-form-item>
           </div>
           <div v-if="timeRangeError" class="time-error" style="color: #f56c6c; font-size: 12px; margin-top: 8px; text-align: center;">{{ timeRangeError }}</div>
         </el-form>
       </div>
    </div>
    <div v-else class="loading">
      <span>获取详细时间...</span>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleSubmit"
        >提交预约</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElDialog, ElForm, ElFormItem, ElInput, ElDatePicker, ElButton, ElMessage } from 'element-plus'

// 初始化日期选择器为当前日期
const calendarDate = ref(new Date())
// 跨日开关
const isCrossDay = ref(false)

// 上个月按钮
const prevMonth = () => {
  const newDate = new Date(calendarDate.value)
  newDate.setMonth(newDate.getMonth() - 1)
  calendarDate.value = newDate
  // 获取当前选中日期的不可预约时间段
  fetchUnavailableTimesForSingleDate(newDate)
      .then(times => {
        unavailableTimes.value = times;
      });
}

// 检查是否为当前月份
const isCurrentMonth = computed(() => {
  const today = new Date()
  const currentDate = new Date(calendarDate.value)
  return today.getFullYear() === currentDate.getFullYear() && today.getMonth() === currentDate.getMonth()
})

// 今天按钮
const today = () => {
  const todayDate = new Date()
  calendarDate.value = todayDate
  // 获取当前选中日期的不可预约时间段
  fetchUnavailableTimesForSingleDate(todayDate)
      .then(times => {
        unavailableTimes.value = times;
      });
}

// 下个月按钮
const nextMonth = () => {
  const newDate = new Date(calendarDate.value)
  newDate.setMonth(newDate.getMonth() + 1)
  calendarDate.value = newDate
  // 获取当前选中日期的不可预约时间段
  fetchUnavailableTimesForSingleDate(newDate)
      .then(times => {
        unavailableTimes.value = times;
      });
}

// 处理跨日模式切换
const handleCrossDayChange = (value) => {
  if (value) { // 从非跨日模式切换到跨日模式
    // 更新当前日期不可预约时间段
    fetchUnavailableTimesForSingleDate(calendarDate.value)
        .then(times => {
          unavailableTimes.value = times;
        });
    // 确保结束日期至少比开始日期晚一天
    const startDate = new Date(crossDayForm.value.startDate)
    const endDate = new Date(crossDayForm.value.endDate)
    if (startDate.getTime() === endDate.getTime()) {
      // 如果开始和结束日期相同，将结束日期设置为下一天
      const nextDay = new Date(startDate)
      nextDay.setDate(nextDay.getDate() + 1)
      crossDayForm.value.endDate = nextDay
    }

  } else { // 从跨日模式切换到非跨日模式
    // 将startTime和endTime的日期部分修改为当前选择的日期
    if (formData.value.startTime) {
      const start = new Date(formData.value.startTime)
      const currentDate = new Date(calendarDate.value)
      start.setFullYear(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate())
      formData.value.startTime = start
    }
    if (formData.value.endTime) {
      const end = new Date(formData.value.endTime)
      const currentDate = new Date(calendarDate.value)
      end.setFullYear(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate())
      formData.value.endTime = end
    }
    
    // 判断时间顺序
    const start = formData.value.startTime
    const end = formData.value.endTime
    if (start && end && start > end) {
      // 如果开始时间晚于结束时间，交换它们
      const temp = formData.value.startTime
      formData.value.startTime = formData.value.endTime
      formData.value.endTime = temp
      slider1Time.value = new Date(formData.value.startTime)
      slider2Time.value = new Date(formData.value.endTime)
    }

    // 切换到非跨日模式时，获取当前选中日期的不可预约时间段
    fetchUnavailableTimesForSingleDate(calendarDate.value)
        .then(times => {
          unavailableTimes.value = times;
        });
  }
}

// 检查特定日期是否有不可预约时间段冲突
const hasUnavailableTimeConflict = (date) => {
  const dateStr = date.toISOString().split('T')[0] // YYYY-MM-DD格式
  const unavailableTimesForDate = allUnavailableTimes.value[dateStr] || []
  
  // 如果该日期没有不可预约时间段，则无冲突
  if (!unavailableTimesForDate.length) {
    return false
  }
  
  // 检查是否有不可预约时间段
  return unavailableTimesForDate.length > 0
}

// 获取日期单元格的CSS类
const getDateCellClass = (data) => {
  const date = new Date(data.date)
  date.setHours(0, 0, 0, 0) // 清除时间部分以便比较
  
  if (!isCrossDay.value) {
    // 非跨日模式，只高亮当前选中的日期
    const selectedDate = new Date(calendarDate.value)
    selectedDate.setHours(0, 0, 0, 0)
    if (date.getTime() === selectedDate.getTime()) {
      return ['date-cell', 'current-selected']
    }
  } else if (isCrossDay.value && crossDayForm.value.startDate && crossDayForm.value.endDate) {
    // 跨日模式，高亮日期范围
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    // 检查当前日期是否在范围内
    const isInRange = date.getTime() >= startDate.getTime() && date.getTime() <= endDate.getTime()
    const isStart = date.getTime() === startDate.getTime()
    const isEnd = date.getTime() === endDate.getTime()
    
    const classes = ['date-cell']
    if (isInRange) classes.push('in-range')
    if (isStart) classes.push('range-start')
    if (isEnd) classes.push('range-end')
    
    // 检查该日期是否有不可预约时间段冲突
    if (hasUnavailableTimeConflict(date)) {
      classes.push('conflict')
    }
    
    return classes
  }
  
  return ['date-cell']
}

// 处理日期点击事件
const handleDateClick = (date) => {
  // 判断所选日期是否为今日之前的
  const selectedDate = new Date(date)
  selectedDate.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (selectedDate < today) {
    ElMessage.warning('不能选择今日之前的日期')
    // 设置为今天的日期，并确保日历重新渲染
    calendarDate.value = new Date(today)
    // 强制触发日历更新
    setTimeout(() => {
      calendarDate.value = new Date(today)
    }, 0)
    return
  }
  // 更新选中的日期
  calendarDate.value = date
  // 获取当前选中日期的不可预约时间段
  fetchUnavailableTimesForSingleDate(date)
    .then(times => {
      unavailableTimes.value = times;
    });
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp * 1000)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 检查是否与不可预约时间段重叠
const isTimeOverlapWithUnavailable = ref(false)

const validateTime = () => {
  timeRangeError.value = ''
  isTimeOverlapWithUnavailable.value = false
  
  if (!formData.value.startTime || !formData.value.endTime) {
    return
  }
  

  
  const startDate = new Date(calendarDate.value)
  let startHours = 0
  let startMinutes = 0
  let endHours = 0
  let endMinutes = 0
  
  // 处理startTime
  if (formData.value.startTime instanceof Date) {
    startHours = formData.value.startTime.getHours()
    startMinutes = formData.value.startTime.getMinutes()
  } else if (typeof formData.value.startTime === 'string') {
    const [hours, minutes] = formData.value.startTime.split(':').map(Number)
    startHours = hours
    startMinutes = minutes
  }
  
  // 处理endTime
  if (formData.value.endTime instanceof Date) {
    endHours = formData.value.endTime.getHours()
    endMinutes = formData.value.endTime.getMinutes()
  } else if (typeof formData.value.endTime === 'string') {
    const [hours, minutes] = formData.value.endTime.split(':').map(Number)
    endHours = hours
    endMinutes = minutes
  }
  
  startDate.setHours(startHours, startMinutes, 0, 0)
  const startTimestamp = Math.floor(startDate.getTime() / 1000)
  
  const endDate = new Date(calendarDate.value)
  endDate.setHours(endHours, endMinutes, 0, 0)
  const endTimestamp = Math.floor(endDate.getTime() / 1000)
  
  // 检查所选时间是否与不可预约的时间段冲突
  let hasConflict = false
  for (const item of unavailableTimes.value) {
    if (startTimestamp < item.end_time && endTimestamp > item.start_time) {
      hasConflict = true
      break
    }
  }
  
  // 如果有冲突，显示提示信息
  if (hasConflict) {
    timeRangeError.value = ''
  }
}

const unavailableTimes = ref([])
const allUnavailableTimes = ref({}) // 存储跨日模式下所有日期的不可预约时间段
const isLoading = ref(false)

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  resource: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:visible', 'success', 'cancel'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const formData = ref({
  resourceName: '',
  startTime: null,
  endTime: null
})

// 基于滑块时间自动计算开始和结束时间
const updateFormDataFromSliders = () => {
  if (!slider1Time.value || !slider2Time.value) return
  
  if (isCrossDay.value) {
    // 跨日模式下，根据当前日期类型更新对应的时间
    const selectedDate = new Date(calendarDate.value)
    selectedDate.setHours(0, 0, 0, 0)
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    if (startDate.getTime() === endDate.getTime()) {
      // 开始和结束日期相同，按固定映射设置表单时间
      formData.value.startTime = slider1Time.value
      formData.value.endTime = slider2Time.value
    } else if (selectedDate.getTime() === startDate.getTime()) {
      // 当前选择的是开始日期，更新开始时间
      formData.value.startTime = slider1Time.value
    } else if (selectedDate.getTime() === endDate.getTime()) {
      // 当前选择的是结束日期，更新结束时间
      formData.value.endTime = slider2Time.value
    }
  } else {
    // 非跨日模式，自动确定哪个时间是开始，哪个是结束
    if (slider1Time.value.getTime() < slider2Time.value.getTime()) {
      formData.value.startTime = slider1Time.value
      formData.value.endTime = slider2Time.value
    } else {
      formData.value.startTime = slider2Time.value
      formData.value.endTime = slider1Time.value
    }
  }
}

const timeRangeError = ref('')
const dateRangeError = ref('') // 跨日模式下日期范围的错误信息

// 时间滑块相关变量
const isDragging = ref(false)
const dragType = ref('') // 'slider1' 或 'slider2'


// 跨日日期表单
const crossDayForm = ref({
  startDate: new Date(),
  endDate: (() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow;
  })()
})

// 日期选择错误提示
const dateError = ref('')

// 滑块时间（不区分开始结束）
const slider1Time = ref(new Date(new Date().setHours(0, 0, 0, 0))) // 默认0点
const slider2Time = ref(new Date(new Date().setHours(0, 0, 0, 0))) // 默认0点

// 计算上午和下午的不可预约时间段
const morningUnavailableTimes = computed(() => {
  return unavailableTimes.value.filter(item => {
    const startHour = new Date(item.start_time * 1000).getHours()
    return startHour < 12
  })
})

const afternoonUnavailableTimes = computed(() => {
  return unavailableTimes.value.filter(item => {
    const startHour = new Date(item.start_time * 1000).getHours()
    return startHour >= 12
  })
})

// 时间轴滑块功能函数

// 检查当前日期是否是开始日期
const isStartDate = computed(() => {
  if (!crossDayForm.value.startDate) return false
  const selectedDate = new Date(calendarDate.value)
  selectedDate.setHours(0, 0, 0, 0)
  const startDate = new Date(crossDayForm.value.startDate)
  startDate.setHours(0, 0, 0, 0)
  const endDate = new Date(crossDayForm.value.endDate)
  endDate.setHours(0, 0, 0, 0)
  const isSameDay = startDate.getTime() === endDate.getTime()
  return selectedDate.getTime() === startDate.getTime()
})

// 检查当前日期是否是结束日期
const isEndDate = computed(() => {
  if (!crossDayForm.value.endDate) return false
  const selectedDate = new Date(calendarDate.value)
  selectedDate.setHours(0, 0, 0, 0)
  const startDate = new Date(crossDayForm.value.startDate)
  startDate.setHours(0, 0, 0, 0)
  const endDate = new Date(crossDayForm.value.endDate)
  endDate.setHours(0, 0, 0, 0)
  const isSameDay = startDate.getTime() === endDate.getTime()
  return selectedDate.getTime() === endDate.getTime()
})

// 检查当前日期是否在开始和结束日期之间
const isMiddleDate = computed(() => {
  if (!crossDayForm.value.startDate || !crossDayForm.value.endDate) return false
  const selectedDate = new Date(calendarDate.value)
  selectedDate.setHours(0, 0, 0, 0)
  const startDate = new Date(crossDayForm.value.startDate)
  startDate.setHours(0, 0, 0, 0)
  const endDate = new Date(crossDayForm.value.endDate)
  endDate.setHours(0, 0, 0, 0)
  return selectedDate.getTime() > startDate.getTime() && selectedDate.getTime() < endDate.getTime()
})



// 检查时间是否在当前时间段内
const isTimeInSection = (section) => {
  if (!isCrossDay.value) {
    if (!slider1Time.value || !slider2Time.value) return false
    
    const startHour = Math.min(slider1Time.value.getHours(), slider2Time.value.getHours())
    const endHour = Math.max(slider1Time.value.getHours(), slider2Time.value.getHours())
    
    if (section === 'morning') {
      return startHour < 12 || endHour < 12
    } else {
      return startHour >= 12 || endHour >= 12
    }
  } else {
    // 检查当前选择的日期是否在起始日期范围内
    const selectedDate = new Date(calendarDate.value)
    selectedDate.setHours(0, 0, 0, 0)
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    // 检查日期是否在范围内
    const isDateInRange = selectedDate.getTime() >= startDate.getTime() && selectedDate.getTime() <= endDate.getTime()
    
    // 跨日模式下，只有当日期在范围内且是开始、结束或中间日期时，才显示选中区间
    if (!isDateInRange || !(isStartDate.value || isEndDate.value || isMiddleDate.value)) {
      return false
    }
    

    
    // 检查开始和结束日期是否相同
    const isSameDay = startDate.getTime() === endDate.getTime()
    
    if (isSameDay) {
      // 开始和结束日期相同，按照单日模式处理
      if (!formData.value.startTime || !formData.value.endTime) return false
      const startTime = formData.value.startTime
      const endTime = formData.value.endTime
      const startHour = startTime.getHours()
      const endHour = endTime.getHours()
      
      if (section === 'morning') {
        return startHour < 12 || endHour < 12
      } else {
        return startHour >= 12 || endHour >= 12
      }
    }
    
    // 开始日期的特殊处理
    if (isStartDate.value) {
      if (!formData.value.startTime) return false
      const startHour = formData.value.startTime.getHours()
      if (section === 'morning' && startHour >= 12) {
        return false // 开始时间在下午，上午不显示
      } else if (section === 'afternoon' && startHour < 12) {
        return true // 开始时间在上午，下午整个时间段都显示
      }
    }
    
    // 结束日期的特殊处理
    if (isEndDate.value) {
      if (!formData.value.endTime) return false
      const endHour = formData.value.endTime.getHours()
      if (section === 'morning' && endHour < 12) {
        return true // 结束时间在上午，上午整个时间段都显示
      } else if (section === 'afternoon' && endHour < 12) {
        return false // 结束时间在上午，下午不显示
      }
    }
    
    return true
  }
}

// 获取滑块标签
const getSliderLabel = (type) => {
  return ''
}

// 获取全局滑块样式
const getGlobalHandleStyle = (type) => {
  // 确保使用正确的时间顺序
  const time = type === 'slider1' ? formData.value.startTime : formData.value.endTime
  if (!time) return {}
  
  // 跨日模式下，根据当前日期决定是否显示滑块
  if (isCrossDay.value) {
    // 检查当前选择的日期是否在起始日期范围内
    const selectedDate = new Date(calendarDate.value)
    selectedDate.setHours(0, 0, 0, 0)
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    // 检查日期是否在范围内
    const isDateInRange = selectedDate.getTime() >= startDate.getTime() && selectedDate.getTime() <= endDate.getTime()
    
    // 如果日期不在范围内，不显示滑块
    if (!isDateInRange) {
      return { display: 'none' }
    }
    
    // 检查开始和结束日期是否相同
    const isSameDay = startDate.getTime() === endDate.getTime()
    
    if (isSameDay) {
      // 开始和结束日期相同，显示两个滑块
    } else if (isStartDate.value && type === 'slider2') {
      // 开始日期不显示结束滑块
      return { display: 'none' }
    } else if (isEndDate.value && type === 'slider1') {
      // 结束日期不显示开始滑块
      return { display: 'none' }
    } else if (isMiddleDate.value) {
      // 中间日期不显示任何滑块
      return { display: 'none' }
    }
  }
  
  const hour = time.getHours()
  const minute = time.getMinutes()
  
  // 确定在哪个时间轴
  const isMorning = hour < 12
  
  // 计算在当前时间轴内的位置
  const sectionPosition = isMorning ? 
    ((hour * 60 + minute) / (12 * 60)) * 100 : 
    (((hour - 12) * 60 + minute) / (12 * 60)) * 100
  
  // 计算相对于容器的位置
  const topPosition = isMorning ? 20 : 55 // 上午在第一个轨道，下午在第二个轨道
  
  return {
    left: `${sectionPosition}%`,
    top: `${topPosition}px`,
    position: 'absolute',
    zIndex: 10
  }
}

// 获取选中时间段在指定时间轴上的显示范围
const getSelectedSegmentLeft = (section) => {
  if (!formData.value.startTime || !formData.value.endTime) return 0
  
  // 确保使用正确的时间顺序
  const startTime = formData.value.startTime
  const endTime = formData.value.endTime
  
  if (!isCrossDay.value) {
    const startHour = startTime.getHours()
    const endHour = endTime.getHours()
    
    if (section === 'morning') {
      // 上午时间段：只显示0-12点的部分
      if (startHour >= 12) return 0 // 如果开始时间在下午，上午不显示
      
      const startMinutes = startHour * 60 + startTime.getMinutes()
      return (startMinutes / (12 * 60)) * 100
    } else {
      // 下午时间段：只显示12-24点的部分
      if (endHour < 12) return 0 // 如果结束时间在上午，下午不显示
      
      const startMinutes = Math.max(0, (startHour - 12) * 60 + startTime.getMinutes())
      return (startMinutes / (12 * 60)) * 100
    }
  } else {
    // 跨日模式下
    // 检查开始和结束日期是否相同
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    const isSameDay = startDate.getTime() === endDate.getTime()
    
    if (isSameDay) {
      // 开始和结束日期相同，按照单日模式处理
      const startHour = startTime.getHours()
      const endHour = endTime.getHours()
      
      if (section === 'morning') {
        // 上午时间段：只显示0-12点的部分
        if (startHour >= 12) return 0 // 如果开始时间在下午，上午不显示
        
        const startMinutes = startHour * 60 + startTime.getMinutes()
        return (startMinutes / (12 * 60)) * 100
      } else {
        // 下午时间段：只显示12-24点的部分
        if (endHour < 12) return 0 // 如果结束时间在上午，下午不显示
        
        const startMinutes = Math.max(0, (startHour - 12) * 60 + startTime.getMinutes())
        return (startMinutes / (12 * 60)) * 100
      }
    } else if (isStartDate.value) {
      // 开始日期：从开始时间到当天结束
      if (section === 'morning') {
        const startHour = startTime.getHours()
        if (startHour >= 12) return 0
        const startMinutes = startHour * 60 + startTime.getMinutes()
        return (startMinutes / (12 * 60)) * 100
      } else {
        // 下午时间段：从开始时间开始
        const startHour = startTime.getHours()
        if (startHour < 12) return 0 // 开始时间在上午，下午从0点开始
        const startMinutes = (startHour - 12) * 60 + startTime.getMinutes()
        return (startMinutes / (12 * 60)) * 100
      }
    } else if (isEndDate.value) {
      // 结束日期：从当天开始到结束时间
      return 0 // 从0点开始
    } else if (isMiddleDate.value) {
      // 中间日期：整个时间段都选中
      return 0 // 从0点开始
    }
    return 0
  }
}

// 获取选中时间段在指定时间轴上的宽度
const getSelectedSegmentWidth = (section) => {
  if (!formData.value.startTime || !formData.value.endTime) return 0
  
  // 确保使用正确的时间顺序
  const startTime = formData.value.startTime
  const endTime = formData.value.endTime
  
  if (!isCrossDay.value) {
    
    const startHour = startTime.getHours()
    const endHour = endTime.getHours()
    
    if (section === 'morning') {
      // 上午时间段：从开始时间到12点，或者到结束时间（如果结束时间在上午）
      if (startHour >= 12) return 0 // 开始时间在下午，上午不显示
      
      const startMinutes = startHour * 60 + startTime.getMinutes()
      const endMinutes = Math.min(12 * 60, endHour * 60 + endTime.getMinutes())
      
      return Math.max(0, ((endMinutes - startMinutes) / (12 * 60)) * 100)
    } else {
      // 下午时间段：从12点到结束时间，或者从开始时间（如果开始时间在下午）
      if (endHour < 12) return 0 // 结束时间在上午，下午不显示
      
      const startMinutes = Math.max(0, (startHour - 12) * 60 + startTime.getMinutes())
      const endMinutes = (endHour - 12) * 60 + endTime.getMinutes()
      
      return Math.max(0, ((endMinutes - startMinutes) / (12 * 60)) * 100)
    }
  } else {
    // 跨日模式下
    // 检查开始和结束日期是否相同
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    const isSameDay = startDate.getTime() === endDate.getTime()
    
    if (isSameDay) {
      // 开始和结束日期相同，按照单日模式处理
      const startHour = startTime.getHours()
      const endHour = endTime.getHours()
      
      if (section === 'morning') {
        // 上午时间段：从开始时间到12点，或者到结束时间（如果结束时间在上午）
        if (startHour >= 12) return 0 // 开始时间在下午，上午不显示
        
        const startMinutes = startHour * 60 + startTime.getMinutes()
        const endMinutes = Math.min(12 * 60, endHour * 60 + endTime.getMinutes())
        
        return Math.max(0, ((endMinutes - startMinutes) / (12 * 60)) * 100)
      } else {
        // 下午时间段：从12点到结束时间，或者从开始时间（如果开始时间在下午）
        if (endHour < 12) return 0 // 结束时间在上午，下午不显示
        
        const startMinutes = Math.max(0, (startHour - 12) * 60 + startTime.getMinutes())
        const endMinutes = (endHour - 12) * 60 + endTime.getMinutes()
        
        return Math.max(0, ((endMinutes - startMinutes) / (12 * 60)) * 100)
      }
    } else if (isStartDate.value) {
      // 开始日期：从开始时间到当天结束
      if (section === 'morning') {
        const startHour = startTime.getHours()
        if (startHour >= 12) return 0
        const startMinutes = startHour * 60 + startTime.getMinutes()
        const endMinutes = 12 * 60 // 上午结束时间
        return ((endMinutes - startMinutes) / (12 * 60)) * 100
      } else {
        // 下午时间段：从开始时间到当天结束
        const startHour = startTime.getHours()
        if (startHour < 12) return 100 // 开始时间在上午，下午整个时间段都选中
        const startMinutes = (startHour - 12) * 60 + startTime.getMinutes()
        const endMinutes = 12 * 60 // 下午结束时间
        return ((endMinutes - startMinutes) / (12 * 60)) * 100
      }
    } else if (isEndDate.value) {
      // 结束日期：从当天开始到结束时间
      if (section === 'morning') {
        const endHour = endTime.getHours()
        if (endHour >= 12) return 100 // 上午整个时间段都选中
        const endMinutes = endHour * 60 + endTime.getMinutes()
        return (endMinutes / (12 * 60)) * 100
      } else {
        const endHour = endTime.getHours()
        if (endHour < 12) return 0 // 结束时间在上午，下午不显示
        const endMinutes = (endHour - 12) * 60 + endTime.getMinutes()
        return (endMinutes / (12 * 60)) * 100
      }
    } else if (isMiddleDate.value) {
      // 中间日期：整个时间段都选中
      return 100 // 100%宽度
    }
    return 0
  }
}

// 格式化显示时间
const formatSelectedTime = (time) => {
  if (!time) return '未选择'
  if (typeof time === 'string') return time
  return `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}`
}

// 处理轨道点击
const handleTrackClick = (event, section) => {
  // 跨日模式下，检查当前选择的日期是否在起始日期范围内
  if (isCrossDay.value) {
    const selectedDate = new Date(calendarDate.value)
    selectedDate.setHours(0, 0, 0, 0)
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    // 检查日期是否在范围内
    const isDateInRange = selectedDate.getTime() >= startDate.getTime() && selectedDate.getTime() <= endDate.getTime()
    
    // 如果日期不在范围内，不允许操作
    if (!isDateInRange) {
      return
    }
    
    // 检查是否是中间日期
    if (isMiddleDate.value) {
      // 中间日期不允许操作
      return
    }
  }
  
  const track = event.currentTarget
  const rect = track.getBoundingClientRect()
  const clickX = event.clientX - rect.left
  const percentage = (clickX / rect.width) * 100

  // 计算对应的时间
  let hour, minute
  if (section === 'morning') {
    const totalMinutes = (percentage / 100) * (12 * 60)
    hour = Math.floor(totalMinutes / 60)
    minute = Math.floor(totalMinutes % 60)
  } else {
    const totalMinutes = (percentage / 100) * (12 * 60)
    hour = Math.floor(totalMinutes / 60) + 12
    minute = Math.floor(totalMinutes % 60)
  }
  
  // 创建时间对象
  const newTime = new Date(calendarDate.value)
  newTime.setHours(hour, minute, 0, 0)
  
  // 确定更新哪个滑块
  if (isCrossDay.value) {
    // 检查开始和结束日期是否相同
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    const isSameDay = startDate.getTime() === endDate.getTime()
    
    if (isSameDay) {
      // 开始和结束日期相同，按非跨日模式处理
      const diff1 = Math.abs(newTime.getTime() - slider1Time.value.getTime())
      const diff2 = Math.abs(newTime.getTime() - slider2Time.value.getTime())
      
      if (diff1 < diff2) {
        slider1Time.value = newTime
      } else {
        slider2Time.value = newTime
      }
    } else {
      // 跨日模式下，根据当前日期类型决定调整哪个滑块
      if (isStartDate.value) {
        // 开始日期，只调整开始滑块
        slider1Time.value = newTime
      } else if (isEndDate.value) {
        // 结束日期，只调整结束滑块
        slider2Time.value = newTime
      } else {
        // 中间日期，不允许调整
        return
      }
    }
  } else {
    // 非跨日模式，根据距离更近的滑块调整
    const diff1 = Math.abs(newTime.getTime() - slider1Time.value.getTime())
    const diff2 = Math.abs(newTime.getTime() - slider2Time.value.getTime())
    
    if (diff1 < diff2) {
      slider1Time.value = newTime
    } else {
      slider2Time.value = newTime
    }
  }
  
  // 更新表单数据
  updateFormDataFromSliders()
  validateTime()
}

// 开始拖动滑块
const startDrag = (event, type) => {
  // 跨日模式下，检查当前选择的日期是否在起始日期范围内
  if (isCrossDay.value) {
    const selectedDate = new Date(calendarDate.value)
    selectedDate.setHours(0, 0, 0, 0)
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    // 检查日期是否在范围内
    const isDateInRange = selectedDate.getTime() >= startDate.getTime() && selectedDate.getTime() <= endDate.getTime()
    
    // 如果日期不在范围内，不允许操作
    if (!isDateInRange) {
      return
    }
    
    // 检查是否是中间日期
    if (isMiddleDate.value) {
      // 中间日期不允许操作
      return
    }
    
    // 跨日模式下，根据当前日期类型限制可拖动的滑块
    const isSameDay = startDate.getTime() === endDate.getTime()
    
    if (!isSameDay) {
      if (isStartDate.value && type === 'slider2') {
        // 开始日期只能拖动开始滑块
        return
      } else if (isEndDate.value && type === 'slider1') {
        // 结束日期只能拖动结束滑块
        return
      }
    } else {
      // 开始和结束日期相同，允许拖动任意滑块（如同非跨日模式）
    }
  }
  
  event.stopPropagation()
  isDragging.value = true
  dragType.value = type
  
  // 添加全局事件监听
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  
  event.preventDefault()
}

// 拖动过程 - 实时跟随光标
const onDrag = (event) => {
  if (!isDragging.value) return
  
    // 跨日模式下，检查当前选择的日期是否在起始日期范围内
    if (isCrossDay.value) {
      const selectedDate = new Date(calendarDate.value)
      selectedDate.setHours(0, 0, 0, 0)
      const startDate = new Date(crossDayForm.value.startDate)
      startDate.setHours(0, 0, 0, 0)
      const endDate = new Date(crossDayForm.value.endDate)
      endDate.setHours(0, 0, 0, 0)

      // 检查日期是否在范围内
      const isDateInRange = selectedDate.getTime() >= startDate.getTime() && selectedDate.getTime() <= endDate.getTime()

      // 如果日期不在范围内，停止拖动
      if (!isDateInRange) {
        endDrag()
        return
      }

      // 检查是否是中间日期
      if (isMiddleDate.value) {
        // 中间日期不允许操作，停止拖动
        endDrag()
        return
      }

      // 跨日模式下，根据当前日期类型限制可拖动的滑块
      const isSameDay = startDate.getTime() === endDate.getTime()

      if (!isSameDay) {
        if (isStartDate.value && dragType.value === 'slider2') {
          // 开始日期只能拖动开始滑块
          endDrag()
          return
        } else if (isEndDate.value && dragType.value === 'slider1') {
          // 结束日期只能拖动结束滑块
          endDrag()
          return
        }
      } else {
        // 开始和结束日期相同，允许拖动任意滑块（如同非跨日模式）
      }
    }
  
  // 获取两个时间轴的位置信息
  const morningTrack = document.querySelector('.time-section:nth-child(1) .time-track')
  const afternoonTrack = document.querySelector('.time-section:nth-child(2) .time-track')
  
  if (!morningTrack || !afternoonTrack) return
  
  const morningRect = morningTrack.getBoundingClientRect()
  const afternoonRect = afternoonTrack.getBoundingClientRect()
  
  // 计算光标到两个时间轴的距离（考虑X轴和Y轴）
  const morningCenterY = (morningRect.top + morningRect.bottom) / 2
  const afternoonCenterY = (afternoonRect.top + afternoonRect.bottom) / 2
  
  // 计算光标到上午时间轴的距离（考虑X轴边界）
  let distanceToMorning = Math.abs(event.clientY - morningCenterY)
  const morningX = Math.max(morningRect.left, Math.min(morningRect.right, event.clientX))
  distanceToMorning += Math.abs(event.clientX - morningX) * 0.1 // X轴距离权重较小
  
  // 计算光标到下午时间轴的距离（考虑X轴边界）
  let distanceToAfternoon = Math.abs(event.clientY - afternoonCenterY)
  const afternoonX = Math.max(afternoonRect.left, Math.min(afternoonRect.right, event.clientX))
  distanceToAfternoon += Math.abs(event.clientX - afternoonX) * 0.1 // X轴距离权重较小
  
  // 根据距离确定在哪个时间轴上
  let track, section, percentage
  
  if (distanceToMorning < distanceToAfternoon) {
    // 靠近上午时间轴
    track = morningTrack
    section = 'morning'
    const clickX = Math.max(0, Math.min(morningRect.width, morningX - morningRect.left))
    percentage = (clickX / morningRect.width) * 100
    // 确保百分比不超过100%
    percentage = Math.min(99.99, percentage)
  } else {
    // 靠近下午时间轴
    track = afternoonTrack
    section = 'afternoon'
    const clickX = Math.max(0, Math.min(afternoonRect.width, afternoonX - afternoonRect.left))
    percentage = (clickX / afternoonRect.width) * 100
    // 确保百分比不超过100%
    percentage = Math.min(99.99, percentage)
  }
  
  // 计算对应的时间
  let hour, minute
  if (section === 'morning') {
    const totalMinutes = (percentage / 100) * (12 * 60)
    hour = Math.floor(totalMinutes / 60)
    minute = Math.floor(totalMinutes % 60)
  } else {
    const totalMinutes = (percentage / 100) * (12 * 60)
    hour = Math.floor(totalMinutes / 60) + 12
    minute = Math.floor(totalMinutes % 60)
  }
  
  // 创建时间对象
  const newTime = new Date(calendarDate.value)
  newTime.setHours(hour, minute, 0, 0)
  
  // 实时更新滑块时间
  if (dragType.value === 'slider1') {
    slider1Time.value = newTime
  } else {
    slider2Time.value = newTime
  }
  
  // 在跨日模式下当开始和结束日期相同时，采用类似非跨日模式的逻辑
  if (isCrossDay.value && 
      crossDayForm.value.startDate && crossDayForm.value.endDate &&
      new Date(crossDayForm.value.startDate).setHours(0, 0, 0, 0) === new Date(crossDayForm.value.endDate).setHours(0, 0, 0, 0)) {
    
    // 类似非跨日模式，根据时间的早晚自动分配开始和结束时间
    if (slider1Time.value.getTime() < slider2Time.value.getTime()) {
      formData.value.startTime = slider1Time.value
      formData.value.endTime = slider2Time.value
    } else {
      formData.value.startTime = slider2Time.value
      formData.value.endTime = slider1Time.value
    }
  } else {
    // 非跨日模式或日期不同时，正常更新表单数据
    updateFormDataFromSliders();
  }
  
  validateTime()
}

// 结束拖动
const endDrag = () => {
  isDragging.value = false
  dragType.value = ''
  
  // 移除事件监听
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
}

// 时间选择器变化处理
const handleTimePickerChange = () => {
  if (!formData.value.startTime || !formData.value.endTime) return
  
  // 确保结束时间在开始时间之后
  // 非跨日模式，或跨日模式下开始和结束日期为同一天时，检查时间顺序
  if (!isCrossDay.value || 
      (isCrossDay.value && crossDayForm.value.startDate && crossDayForm.value.endDate && 
       new Date(crossDayForm.value.startDate).setHours(0, 0, 0, 0) === new Date(crossDayForm.value.endDate).setHours(0, 0, 0, 0))) {
    
    // 比较时间部分，不包含日期
    const startHours = formData.value.startTime.getHours();
    const startMinutes = formData.value.startTime.getMinutes();
    const endHours = formData.value.endTime.getHours();
    const endMinutes = formData.value.endTime.getMinutes();
    
    // 将时间转换为分钟数进行比较
    const startTimeInMinutes = startHours * 60 + startMinutes;
    const endTimeInMinutes = endHours * 60 + endMinutes;
    
    if (startTimeInMinutes > endTimeInMinutes) {
      const temp = formData.value.startTime
      formData.value.startTime = formData.value.endTime
      formData.value.endTime = temp
    }
  } else {
    // 跨日模式下，不同日期之间的时间比较
    if (formData.value.startTime > formData.value.endTime) {
      const temp = formData.value.startTime
      formData.value.startTime = formData.value.endTime
      formData.value.endTime = temp
    }
  }
  
  // 更新滑块时间
  slider1Time.value = formData.value.startTime
  slider2Time.value = formData.value.endTime
  
  // 验证时间
  validateTime()
  
  // 跨日模式下，检查所选时间范围内是否包含不可预约时间段
  if (isCrossDay.value && crossDayForm.value.startDate && crossDayForm.value.endDate) {
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(formData.value.startTime.getHours(), formData.value.startTime.getMinutes(), 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(formData.value.endTime.getHours(), formData.value.endTime.getMinutes(), 0, 0)
    
    const startTimestamp = Math.floor(startDate.getTime() / 1000)
    const endTimestamp = Math.floor(endDate.getTime() / 1000)
    
    // 检查所选时间范围内是否包含不可预约时间段
    let hasConflict = false
    for (const dateStr in allUnavailableTimes.value) {
      const times = allUnavailableTimes.value[dateStr]
      for (const time of times) {
        if (time.start_time < endTimestamp && time.end_time > startTimestamp) {
          hasConflict = true
          break
        }
      }
      if (hasConflict) break
    }
    
    if (hasConflict) {
      timeRangeError.value = '选择范围内存在不可选择时间段'
    } else {
      timeRangeError.value = ''
    }
  }




// 日期选择器变化处理（用于日历点击和其他非日期选择器触发的情况）
const handleDateChange = () => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  // 重置错误提示
  dateError.value = ''
  
  // 验证开始日期
  if (crossDayForm.value.startDate) {
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    
    if (startDate < today) {
      crossDayForm.value.startDate = new Date()
      ElMessage.warning('不可选择之前的日期')
    }
  }
  
  // 验证结束日期
  if (crossDayForm.value.endDate) {
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    if (endDate < today) {
      crossDayForm.value.endDate = new Date()
      ElMessage.warning('不可选择之前的日期')
    }
  }
  
  // 检查结束日期是否小于开始日期，或是否为同一天
  if (crossDayForm.value.startDate && crossDayForm.value.endDate) {
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    if (endDate < startDate) {
      dateError.value = '结束日期必须晚于开始日期'
    } else if (startDate.getTime() === endDate.getTime()) {
      // 开始和结束日期相同，自动将结束日期设置为下一天
      const newEndDate = new Date(startDate)
      newEndDate.setDate(newEndDate.getDate() + 1)
      crossDayForm.value.endDate = newEndDate
      ElMessage.info('已自动将结束日期调整为下一天')
    }
  }
  
  // 验证时间
  validateTime()
}

// 处理日期选择器变化（专门用于获取整个日期范围的不可预约时间段）
const handleDateRangeChange = async () => {
  // 先调用原始的处理逻辑
  handleDateChange()
  
  // 在跨日模式下，获取整个日期范围的不可预约时间段
  if (isCrossDay.value && crossDayForm.value.startDate && crossDayForm.value.endDate) {
    // 获取整个日期范围的不可预约时间段
    const unavailableTimesMap = await fetchUnavailableTimesForRange(crossDayForm.value.startDate, crossDayForm.value.endDate)
    allUnavailableTimes.value = unavailableTimesMap
    
    // 检查整个日期范围内是否有不可预约时间段
    let hasConflict = false
    for (const dateStr in unavailableTimesMap) {
      if (unavailableTimesMap[dateStr].length > 0) {
        hasConflict = true
        break
      }
    }
    
    // 如果有冲突，显示文字提示信息
    if (hasConflict) {
      dateRangeError.value = '所选区间内存在不可预约时间段'
    } else {
      dateRangeError.value = '' // 清除之前的错误信息
    }
  }
}
}

// 监听表单时间变化，更新滑块
watch(() => formData.value.startTime, (newStartTime) => {
  if (newStartTime && !isDragging.value) {
    slider1Time.value = newStartTime
  }
})

watch(() => formData.value.endTime, (newEndTime) => {
  if (newEndTime && !isDragging.value) {
    slider2Time.value = newEndTime
  }
})



// 获取单个日期的不可预约时间段
const fetchUnavailableTimesForSingleDate = async (date) => {
  try {
    let token = localStorage.getItem('token')
    // 去除可能的多余引号
    if (token) {
      token = token.replace(/^"|"$/g, '')
    }
    
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
    
    // 计算当天零点的时间戳
    const dayStart = new Date(date)
    dayStart.setHours(0, 0, 0, 0)
    const dayTimestamp = Math.floor(dayStart.getTime() / 1000)
    
    const response = await fetch(`${apiBaseUrl}/api/appointment/unavailable-times`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token,
        data: {
          type: 'space',
          id: props.resource?.id || 0,
          day: dayTimestamp
        }
      })
    })

    const result = await response.json()
    if (result.success) {
      return result.data || []
    } else {
      console.error(`获取 ${date.toDateString()} 不可预约时间失败:`, result.message)
      return []
    }
  } catch (error) {
    console.error(`获取 ${date.toDateString()} 不可预约时间失败:`, error)
    return []
  }
}

// 获取日期范围内所有日期的不可预约时间段
const fetchUnavailableTimesForRange = async (startDate, endDate) => {
  isLoading.value = true
  try {
    // 计算日期范围内的每一天
    const dateRange = []
    const current = new Date(startDate)
    current.setHours(0, 0, 0, 0)
    
    const end = new Date(endDate)
    end.setHours(0, 0, 0, 0)
    
    while (current.getTime() <= end.getTime()) {
      dateRange.push(new Date(current))
      current.setDate(current.getDate() + 1)
    }
    
    // 并行获取所有日期的不可预约时间段
    const promises = dateRange.map(date => fetchUnavailableTimesForSingleDate(date))
    const results = await Promise.all(promises)
    
    // 将结果存储在映射中，键为日期字符串，值为不可预约时间段数组
    const unavailableTimesMap = {}
    dateRange.forEach((date, index) => {
      const dateStr = date.toISOString().split('T')[0] // YYYY-MM-DD格式
      unavailableTimesMap[dateStr] = results[index]
    })
    
    return unavailableTimesMap
  } catch (error) {
    console.error('获取日期范围内的不可预约时间失败:', error)
    return {}
  } finally {
    isLoading.value = false
  }
}


// 监听resource变化，更新表单数据
watch(() => props.resource, (newResource) => {
  if (newResource) {
    formData.value.resourceName = newResource.name || ''
  }
}, { immediate: true })

// 监听弹窗打开状态，重置数据并获取不可预约时间
watch(dialogVisible, (newVisible) => {
  if (newVisible) {
    // 重置表单数据
    formData.value = {
      resourceName: props.resource?.name || '',
      startTime: new Date(new Date().setHours(0, 0, 0, 0)),
      endTime: new Date(new Date().setHours(0, 0, 0, 0)),
      purpose: ''
    }

    // 重置不可预约时间
    allUnavailableTimes.value = {}

    // 重置跨日相关
    isCrossDay.value = false
    crossDayForm.value = {
      startDate: new Date(),
      endDate: (() => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        return tomorrow;
      })()
    }
    
    // 重置滑块时间
    slider1Time.value = new Date(new Date().setHours(0, 0, 0, 0))
    slider2Time.value = new Date(new Date().setHours(0, 0, 0, 0))
    
    // 重置错误状态
    timeRangeError.value = ''
    isTimeOverlapWithUnavailable.value = false
    
    // 重置日期为今天
    const today = new Date()
    calendarDate.value = today
    
    // 获取当前日期的不可预约时间
    fetchUnavailableTimesForSingleDate(today)
      .then(times => {
        if (dialogVisible.value) {
          unavailableTimes.value = times;
        }
      });
  }
})

// 监听跨日状态下的开始日期和结束日期
watch([() => crossDayForm.value.startDate, () => crossDayForm.value.endDate], ([newStartDate, newEndDate]) => {
  if (isCrossDay.value && newStartDate && newEndDate) {

    const startDate = new Date(newStartDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(newEndDate)
    endDate.setHours(0, 0, 0, 0)
    
    // 检查开始日期是否大于结束日期
    if (startDate > endDate) {
      dateError.value = '开始日期不能大于结束日期'
      return
    } else {
      dateError.value = ''
    }
    
    // 检查开始和结束日期是否为同一天
    if (startDate.getTime() === endDate.getTime()) {
      // 自动将结束日期向后移动一天
      const nextDay = new Date(startDate)
      nextDay.setDate(nextDay.getDate() + 1)
      crossDayForm.value.endDate = nextDay
      // 提示用户
      ElMessage.warning('所选日期小于最小间隔')
    }
    
    // 当开始日期小于结束日期时，获取所选日期内所有不可预约时间段
    if (startDate < endDate) {
      fetchUnavailableTimesForRange(startDate, endDate)
        .then(unavailableTimesMap => {
          allUnavailableTimes.value = unavailableTimesMap;
          // 时间轴数据只在点击时通过handleDateClick更新，这里只更新allUnavailableTimes
        });
    }
  }
}, { deep: true })



const handleCancel = () => {
  emit('cancel')
  dialogVisible.value = false
}

const handleSubmit = async () => {
  // 表单验证
  if (!formData.value.startTime) {
    ElMessage.error('请选择开始时间')
    return
  }
  if (!formData.value.endTime) {
    ElMessage.error('请选择结束时间')
    return
  }
  
  // 跨日模式下，检查结束日期是否小于开始日期
  if (isCrossDay.value && crossDayForm.value.startDate && crossDayForm.value.endDate) {
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(0, 0, 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(0, 0, 0, 0)
    
    if (endDate < startDate) {
      ElMessage.error('结束日期必须晚于开始日期')
      return
    }
  }
  
  // 构建预约时间
  const startDate = new Date(isCrossDay.value ? crossDayForm.value.startDate : calendarDate.value)
  let startHours = 0
  let startMinutes = 0
  let endHours = 0
  let endMinutes = 0
  
  // 处理startTime
  if (formData.value.startTime instanceof Date) {
    startHours = formData.value.startTime.getHours()
    startMinutes = formData.value.startTime.getMinutes()
  } else if (typeof formData.value.startTime === 'string') {
    const [hours, minutes] = formData.value.startTime.split(':').map(Number)
    startHours = hours
    startMinutes = minutes
  }
  
  // 处理endTime
  if (formData.value.endTime instanceof Date) {
    endHours = formData.value.endTime.getHours()
    endMinutes = formData.value.endTime.getMinutes()
  } else if (typeof formData.value.endTime === 'string') {
    const [hours, minutes] = formData.value.endTime.split(':').map(Number)
    endHours = hours
    endMinutes = minutes
  }
  
  startDate.setHours(startHours, startMinutes, 0, 0)
  
  const endDate = new Date(isCrossDay.value ? crossDayForm.value.endDate : calendarDate.value)
  endDate.setHours(endHours, endMinutes, 0, 0)

  // 验证时间是否在不可预约的时间段内
  validateTime()
  if (timeRangeError.value) {
    ElMessage.error('请选择有效的预约时间')
    return
  }
  
  // 跨日模式下，检查所选时间范围内是否包含不可预约时间段
  if (isCrossDay.value && crossDayForm.value.startDate && crossDayForm.value.endDate) {
    const startDate = new Date(crossDayForm.value.startDate)
    startDate.setHours(formData.value.startTime.getHours(), formData.value.startTime.getMinutes(), 0, 0)
    const endDate = new Date(crossDayForm.value.endDate)
    endDate.setHours(formData.value.endTime.getHours(), formData.value.endTime.getMinutes(), 0, 0)
    
    const startTimestamp = Math.floor(startDate.getTime() / 1000)
    const endTimestamp = Math.floor(endDate.getTime() / 1000)
    
    // 检查所选时间范围内是否包含不可预约时间段
    let hasConflict = false
    for (const dateStr in allUnavailableTimes.value) {
      const times = allUnavailableTimes.value[dateStr]
      for (const time of times) {
        if (time.start_time < endTimestamp && time.end_time > startTimestamp) {
          hasConflict = true
          break
        }
      }
      if (hasConflict) break
    }
    
    if (hasConflict) {
      ElMessage.error('选择范围内存在不可选择时间段')
      return
    }
  }
  
  // 检查结束时间是否晚于开始时间
  if (startDate >= endDate) {
    ElMessage.error('结束时间必须晚于开始时间')
    return
  }

  // 提交预约到后端
  try {
    // 获取token
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    // 获取用户信息
    const userInfoStr = localStorage.getItem('userInfo') || '{}'
    const userInfo = JSON.parse(userInfoStr).userInfo || {}
    
    // 构建请求数据
    const requestData = {
      data: {
        emp_id: userInfo.empId || userInfo.id || '',
        name: userInfo.name || userInfo.userName || '',
        res_id: props.resource.id,
        res_name: props.resource.name,
        type: 'space',
        appointment_date: startDate.toISOString().split('T')[0], // 格式化为YYYY-MM-DD
        start_time: String(Math.floor(startDate.getTime() / 1000)), // 转换为字符串格式的时间戳
        end_time: String(Math.floor(endDate.getTime() / 1000)) // 转换为字符串格式的时间戳
      }
    }
    
    // 发送请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/appointment/create-appointment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8'
      },
      body: JSON.stringify({
        token: token,
        data: requestData.data
      })
    })
    
    // 处理响应
    if (response.status === 403) {
      ElMessage.error('没有权限进行预约')
      return
    }
    
    if (!response.ok) {
      throw new Error('预约请求失败')
    }
    
    const result = await response.json()
    if (result.success) {
      ElMessage.success('预约成功')
      // 调用桌面通知
      import('../..//utils/notification.js').then(({ requestNotificationPermission, sendNotification }) => {
        requestNotificationPermission().then(granted => {
          if (granted) {
            sendNotification('资源预约成功', {
              body: `你已成功预约${result.data.resName}`
            })
          }
        })
      })
      // 发送通知事件
      window.dispatchEvent(new CustomEvent('appointmentSuccess', {
        detail: {
          title: '资源预约成功',
          content: `你已成功预约${result.data.resName}`,
          time: new Date().toLocaleString()
        }
      }))
      emit('success')
      dialogVisible.value = false
    } else {
      ElMessage.error(result.message || '预约失败')
    }
  } catch (error) {
    console.error('预约提交错误:', error)
    ElMessage.error('预约失败，请稍后重试')
  }
}
</script>

<style scoped>
.title {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 10px;
  font-size: 24px;
  font-weight: bold;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  span {
    font-size: 15px;
  }
}

:deep(.el-calendar-table td.is-today) {
  color: #3f51b5;
  font-weight: bold;
}

:deep(.el-calendar-table td.disabled) {
  color: #999;
  cursor: not-allowed;
}

:deep(.el-calendar-table td.disabled:hover) {
  background-color: transparent !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.time-selection {
  margin: 20px 20px;
}

.time-selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.time-selection h3 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
}

.time-selection h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.time-slider-container {
  margin: 20px 0 0;
  position: relative;
  min-height: 120px;
}

.time-section {
  margin-bottom: 25px;
}

.slider-wrapper {
  position: relative;
}

.time-markers {
  display: flex;
  position: relative;
  width: calc(100% + 12px);
  left: -6px;
  right: -6px;
  justify-content: space-between;
  margin-top: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
}

.time-marker {
  color: #666666;
}

.time-track {
  position: relative;
  height: 10px;
  background: linear-gradient(to right, #e8f4fd, #f0f0f0);
  cursor: pointer;
  overflow: hidden;
}

.unavailable-segment {
  position: absolute;
  height: 100%;
  background-color: #f56c6c;
  border: 1px solid #f56c6c;
  z-index: 10;
}

.time-handle {
  position: absolute;
  top: -5px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #3f51b5;
  cursor: pointer;
  z-index: 10;
  transform: translateX(-50%);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 10px;
  font-weight: bold;
  transition: transform 0.2s ease;
}

.time-handle:hover {
  background-color: #3f51b5;
  transform: translateX(-50%) scale(1.1);
}

.handle-label {
  line-height: 1;
  text-align: center;
}

.selected-segment {
  position: absolute;
  height: 100%;
  background-color: rgba(63, 81, 181, 1);
  z-index: 2;
}

.time-pickers {
  justify-items: center;
  margin-top: 10px;
  padding: 0 100px;
}

.time-picker-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.time-picker-item {
  flex: 1;
  margin-bottom: 0;
}

.time-picker-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

.time-picker-item :deep(.el-time-picker) {
  flex: 1;
}

.time-picker-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  color: #909399;
  font-size: 16px;
  font-weight: bold;
}

/* 日期选择器样式 */
.date-pickers {
  justify-items: center;
  margin-bottom: 10px;
  padding: 0 100px;
}

.date-group {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.now-date-btn {
  margin-top: 10px;
  width: fit-content;
}

.date-picker-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.date-picker-item {
  flex: 1;
  margin-bottom: 0;
}

.date-picker-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

.date-picker-item :deep(.el-date-picker) {
  flex: 1;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
}

.loading .is-loading {
  margin-right: 10px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

:deep(.el-calendar__body) {
  padding: 0 20px 12px;
}
:deep(.el-calendar-table .el-calendar-day) {
  height: auto;
  padding: 0;
}

/* 自定义日历头样式 */
:deep(.el-calendar__header) {
  padding: 10px 0;
  margin: 0 20px;
  display: flex;
  flex-direction: column;
}
:deep(.el-switch__label.is-active) {
  color:#3f51b5;
}

/* 自定义日期单元格样式 */
.date-cell {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.date-cell.in-range {
  background-color: rgba(63, 81, 181, 0.5) !important;
  color: #ffffff;
}

.date-cell.range-start,
.date-cell.range-end {
  background-color: #3f51b5 !important;
  color: white !important;
}

.date-cell.conflict {
  background-color: rgba(245, 108, 108, 0.8) !important; /* 浅红色背景 */
}

/*
.date-cell.conflict.range-start,
.date-cell.conflict.range-end {
  background-color: #f56c6c !important;
  color: white !important;
}
*/

.date-cell.conflict.range-start,
.date-cell.conflict.range-end {
  background-color: #3f51b5 !important;
  color: white !important;

}

:deep(.el-calendar-table td.is-selected) {
  background-color: #3f51b5;
  color: #ffffff;
}
:deep(.el-calendar-day:hover):not(.is-selected) {
  background-color: rgba(63, 81, 181, 0.2);
  }
:deep(.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset;
}
</style>