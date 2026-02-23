<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useScheduleStore } from '@/stores/schedule'
import type { Schedule, ScheduleType, ScheduleStatus } from '@/types/schedule'
import { SCHEDULE_TYPE_LABELS, SCHEDULE_TYPE_ICONS, SCHEDULE_TYPE_COLORS, SCHEDULE_STATUS_LABELS } from '@/types/schedule'
import ScheduleModal from '@/components/schedule/ScheduleModal.vue'
import ScheduleCreateModal from '@/components/schedule/ScheduleCreateModal.vue'

const authStore = useAuthStore()
const scheduleStore = useScheduleStore()

// ============================================
// State
// ============================================
const activeTab = ref<'list' | 'calendar'>('list')
const filterStatus = ref<ScheduleStatus | 'ALL'>('ALL')
const selectedSchedule = ref<Schedule | null>(null)
const showDetailModal = ref(false)
const showCreateModal = ref(false)

// 캘린더 관련
const currentDate = ref(new Date())
const currentMonth = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  return new Date(year, month, 1)
})

// ============================================
// Computed
// ============================================
const isCompany = computed(() => authStore.isCompany)

const filteredSchedules = computed(() => {
  if (filterStatus.value === 'ALL') {
    return scheduleStore.schedules
  }
  return scheduleStore.schedules.filter(s => s.status === filterStatus.value)
})

const calendarDays = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  
  const days: { date: Date; isCurrentMonth: boolean; schedules: Schedule[] }[] = []
  
  // 이전 달의 날짜들
  const firstDayOfWeek = firstDay.getDay()
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const date = new Date(year, month, -i)
    days.push({ date, isCurrentMonth: false, schedules: [] })
  }
  
  // 현재 달의 날짜들
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const date = new Date(year, month, i)
    const dateStr = formatDateStr(date)
    const daySchedules = scheduleStore.schedules.filter(s => {
      const scheduleDate = s.startTime.split('T')[0]
      return scheduleDate === dateStr
    })
    days.push({ date, isCurrentMonth: true, schedules: daySchedules })
  }
  
  // 다음 달의 날짜들 (6주 맞추기)
  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    const date = new Date(year, month + 1, i)
    days.push({ date, isCurrentMonth: false, schedules: [] })
  }
  
  return days
})

const monthLabel = computed(() => {
  return currentDate.value.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long' })
})

// ============================================
// Methods
// ============================================
function formatDateStr(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(dateTimeStr: string): string {
  const date = new Date(dateTimeStr)
  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatTime(dateTimeStr: string): string {
  const date = new Date(dateTimeStr)
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getTypeIcon(type: ScheduleType): string {
  return SCHEDULE_TYPE_ICONS[type] || '📅'
}

function getTypeColor(type: ScheduleType): string {
  return SCHEDULE_TYPE_COLORS[type] || '#6B7280'
}

function getStatusBadgeClass(status: ScheduleStatus): string {
  const classes: Record<ScheduleStatus, string> = {
    SCHEDULED: 'bg-blue-100 text-blue-800',
    IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-gray-100 text-gray-800',
    NO_SHOW: 'bg-red-100 text-red-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

function prevMonth() {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() - 1)
  currentDate.value = date
}

function nextMonth() {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() + 1)
  currentDate.value = date
}

function goToToday() {
  currentDate.value = new Date()
}

function isToday(date: Date): boolean {
  const today = new Date()
  return date.toDateString() === today.toDateString()
}

function openDetail(schedule: Schedule) {
  selectedSchedule.value = schedule
  showDetailModal.value = true
}

function openCreateModal() {
  showCreateModal.value = true
}

async function handleStatusChange(scheduleId: number, status: ScheduleStatus) {
  try {
    await scheduleStore.updateStatus(scheduleId, status)
  } catch (e) {
    console.error('상태 변경 실패:', e)
  }
}

async function handleDelete(scheduleId: number) {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await scheduleStore.deleteSchedule(scheduleId)
    showDetailModal.value = false
  } catch (e) {
    console.error('삭제 실패:', e)
  }
}

async function handleScheduleCreated() {
  showCreateModal.value = false
  // 목록 새로고침
  if (isCompany.value) {
    await scheduleStore.fetchCompanySchedules()
  } else {
    await scheduleStore.fetchMySchedules()
  }
}

// ============================================
// Lifecycle
// ============================================
onMounted(async () => {
  if (isCompany.value) {
    await scheduleStore.fetchCompanySchedules()
  } else {
    await scheduleStore.fetchMySchedules()
  }
})
</script>

<template>
  <div class="max-w-6xl mx-auto p-6">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">일정 관리</h1>
        <p class="text-gray-500 mt-1">
          예정된 일정 {{ scheduleStore.upcomingCount }}건 · 
          완료된 일정 {{ scheduleStore.completedCount }}건
        </p>
      </div>
      <div class="flex items-center gap-3">
        <!-- 탭 전환 -->
        <div class="flex bg-gray-100 rounded-lg p-1">
          <button
            @click="activeTab = 'list'"
            :class="[
              'px-4 py-2 text-sm font-medium rounded-md transition-colors',
              activeTab === 'list'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            ]"
          >
            📋 목록
          </button>
          <button
            @click="activeTab = 'calendar'"
            :class="[
              'px-4 py-2 text-sm font-medium rounded-md transition-colors',
              activeTab === 'calendar'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            ]"
          >
            📅 캘린더
          </button>
        </div>
        
        <!-- 일정 생성 버튼 (기업만) -->
        <button
          v-if="isCompany"
          @click="openCreateModal"
          class="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
        >
          + 일정 추가
        </button>
      </div>
    </div>

    <!-- 로딩 -->
    <div v-if="scheduleStore.loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
    </div>

    <!-- 에러 -->
    <div v-else-if="scheduleStore.error" class="bg-red-50 text-red-600 p-4 rounded-lg">
      {{ scheduleStore.error }}
    </div>

    <!-- 목록 뷰 -->
    <div v-else-if="activeTab === 'list'">
      <!-- 필터 -->
      <div class="flex gap-2 mb-4">
        <button
          v-for="status in ['ALL', 'SCHEDULED', 'COMPLETED', 'CANCELLED'] as const"
          :key="status"
          @click="filterStatus = status"
          :class="[
            'px-3 py-1.5 text-sm rounded-full transition-colors',
            filterStatus === status
              ? 'bg-primary-100 text-primary-700'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          ]"
        >
          {{ status === 'ALL' ? '전체' : SCHEDULE_STATUS_LABELS[status] }}
        </button>
      </div>

      <!-- 일정 없음 -->
      <div
        v-if="filteredSchedules.length === 0"
        class="text-center py-12 text-gray-500"
      >
        <div class="text-4xl mb-4">📅</div>
        <p>등록된 일정이 없습니다.</p>
      </div>

      <!-- 일정 목록 -->
      <div v-else class="space-y-3">
        <div
          v-for="schedule in filteredSchedules"
          :key="schedule.id"
          @click="openDetail(schedule)"
          class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
        >
          <div class="flex items-start gap-4">
            <!-- 타입 아이콘 -->
            <div
              class="w-12 h-12 rounded-lg flex items-center justify-center text-2xl"
              :style="{ backgroundColor: getTypeColor(schedule.type) + '20' }"
            >
              {{ getTypeIcon(schedule.type) }}
            </div>

            <!-- 내용 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h3 class="font-semibold text-gray-900 truncate">
                  {{ schedule.title }}
                </h3>
                <span
                  :class="[
                    'px-2 py-0.5 text-xs font-medium rounded-full',
                    getStatusBadgeClass(schedule.status)
                  ]"
                >
                  {{ SCHEDULE_STATUS_LABELS[schedule.status] }}
                </span>
              </div>
              <p class="text-sm text-gray-500">
                {{ formatDateTime(schedule.startTime) }} ~ {{ formatTime(schedule.endTime) }}
              </p>
              <p v-if="schedule.location" class="text-sm text-gray-500 mt-1">
                📍 {{ schedule.location }}
              </p>
            </div>

            <!-- 상태 변경 (기업만) -->
            <div v-if="isCompany && schedule.status === 'SCHEDULED'" class="flex gap-2">
              <button
                @click.stop="handleStatusChange(schedule.id, 'COMPLETED')"
                class="px-3 py-1 text-sm text-green-600 hover:bg-green-50 rounded"
              >
                완료
              </button>
              <button
                @click.stop="handleStatusChange(schedule.id, 'CANCELLED')"
                class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-50 rounded"
              >
                취소
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 캘린더 뷰 -->
    <div v-else-if="activeTab === 'calendar'" class="bg-white rounded-lg border border-gray-200 p-4">
      <!-- 캘린더 헤더 -->
      <div class="flex items-center justify-between mb-4">
        <button
          @click="prevMonth"
          class="p-2 hover:bg-gray-100 rounded-lg"
        >
          ←
        </button>
        <div class="flex items-center gap-3">
          <h2 class="text-lg font-semibold">{{ monthLabel }}</h2>
          <button
            @click="goToToday"
            class="px-3 py-1 text-sm text-primary-600 hover:bg-primary-50 rounded"
          >
            오늘
          </button>
        </div>
        <button
          @click="nextMonth"
          class="p-2 hover:bg-gray-100 rounded-lg"
        >
          →
        </button>
      </div>

      <!-- 요일 헤더 -->
      <div class="grid grid-cols-7 gap-1 mb-2">
        <div
          v-for="day in ['일', '월', '화', '수', '목', '금', '토']"
          :key="day"
          class="text-center text-sm font-medium text-gray-500 py-2"
        >
          {{ day }}
        </div>
      </div>

      <!-- 날짜 그리드 -->
      <div class="grid grid-cols-7 gap-1">
        <div
          v-for="(day, index) in calendarDays"
          :key="index"
          :class="[
            'min-h-24 p-1 border rounded',
            day.isCurrentMonth ? 'bg-white' : 'bg-gray-50',
            isToday(day.date) ? 'border-primary-500' : 'border-gray-200'
          ]"
        >
          <div
            :class="[
              'text-sm font-medium mb-1',
              day.isCurrentMonth ? 'text-gray-900' : 'text-gray-400',
              isToday(day.date) ? 'text-primary-600' : ''
            ]"
          >
            {{ day.date.getDate() }}
          </div>
          <div class="space-y-1">
            <div
              v-for="schedule in day.schedules.slice(0, 2)"
              :key="schedule.id"
              @click="openDetail(schedule)"
              class="text-xs p-1 rounded truncate cursor-pointer hover:opacity-80"
              :style="{
                backgroundColor: getTypeColor(schedule.type) + '20',
                color: getTypeColor(schedule.type)
              }"
            >
              {{ getTypeIcon(schedule.type) }} {{ schedule.title }}
            </div>
            <div
              v-if="day.schedules.length > 2"
              class="text-xs text-gray-500 text-center"
            >
              +{{ day.schedules.length - 2 }}개 더
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 상세 모달 -->
    <ScheduleModal
      v-if="showDetailModal && selectedSchedule"
      :schedule="selectedSchedule"
      :is-company="isCompany"
      @close="showDetailModal = false"
      @delete="handleDelete"
      @status-change="handleStatusChange"
    />

    <!-- 생성 모달 (기업만) -->
    <ScheduleCreateModal
      v-if="showCreateModal"
      @close="showCreateModal = false"
      @created="handleScheduleCreated"
    />
  </div>
</template>
