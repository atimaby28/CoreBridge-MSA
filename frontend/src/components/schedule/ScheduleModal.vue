<script setup lang="ts">
import { computed } from 'vue'
import type { Schedule, ScheduleType, ScheduleStatus } from '@/types/schedule'
import { SCHEDULE_TYPE_LABELS, SCHEDULE_TYPE_ICONS, SCHEDULE_TYPE_COLORS, SCHEDULE_STATUS_LABELS } from '@/types/schedule'

const props = defineProps<{
  schedule: Schedule
  isCompany: boolean
}>()

const emit = defineEmits<{
  close: []
  delete: [scheduleId: number]
  statusChange: [scheduleId: number, status: ScheduleStatus]
}>()

// ============================================
// Computed
// ============================================
const typeIcon = computed(() => SCHEDULE_TYPE_ICONS[props.schedule.type] || '📅')
const typeLabel = computed(() => SCHEDULE_TYPE_LABELS[props.schedule.type])
const typeColor = computed(() => SCHEDULE_TYPE_COLORS[props.schedule.type] || '#6B7280')
const statusLabel = computed(() => SCHEDULE_STATUS_LABELS[props.schedule.status])

const statusBadgeClass = computed(() => {
  const classes: Record<ScheduleStatus, string> = {
    SCHEDULED: 'bg-blue-100 text-blue-800',
    IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-gray-100 text-gray-800',
    NO_SHOW: 'bg-red-100 text-red-800'
  }
  return classes[props.schedule.status] || 'bg-gray-100 text-gray-800'
})

// ============================================
// Methods
// ============================================
function formatDateTime(dateTimeStr: string): string {
  const date = new Date(dateTimeStr)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
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

function getDuration(): string {
  const start = new Date(props.schedule.startTime)
  const end = new Date(props.schedule.endTime)
  const diffMs = end.getTime() - start.getTime()
  const diffMins = Math.round(diffMs / 60000)
  
  if (diffMins < 60) {
    return `${diffMins}분`
  }
  const hours = Math.floor(diffMins / 60)
  const mins = diffMins % 60
  return mins > 0 ? `${hours}시간 ${mins}분` : `${hours}시간`
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center">
    <!-- 배경 오버레이 -->
    <div 
      class="absolute inset-0 bg-black/50" 
      @click="emit('close')"
    ></div>
    
    <!-- 모달 컨텐츠 -->
    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
      <!-- 헤더 -->
      <div 
        class="p-6 border-b"
        :style="{ borderColor: typeColor + '40' }"
      >
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-3">
            <div
              class="w-12 h-12 rounded-lg flex items-center justify-center text-2xl"
              :style="{ backgroundColor: typeColor + '20' }"
            >
              {{ typeIcon }}
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span 
                  class="px-2 py-0.5 text-xs font-medium rounded"
                  :style="{ backgroundColor: typeColor + '20', color: typeColor }"
                >
                  {{ typeLabel }}
                </span>
                <span
                  :class="[
                    'px-2 py-0.5 text-xs font-medium rounded-full',
                    statusBadgeClass
                  ]"
                >
                  {{ statusLabel }}
                </span>
              </div>
              <h2 class="text-xl font-bold text-gray-900 mt-1">
                {{ schedule.title }}
              </h2>
            </div>
          </div>
          <button
            @click="emit('close')"
            class="p-2 hover:bg-gray-100 rounded-lg text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>
      </div>

      <!-- 내용 -->
      <div class="p-6 space-y-4">
        <!-- 시간 -->
        <div class="flex items-start gap-3">
          <div class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-gray-500">
            🕐
          </div>
          <div>
            <p class="font-medium text-gray-900">
              {{ formatDateTime(schedule.startTime) }}
            </p>
            <p class="text-sm text-gray-500">
              ~ {{ formatTime(schedule.endTime) }} ({{ getDuration() }})
            </p>
          </div>
        </div>

        <!-- 장소 -->
        <div v-if="schedule.location" class="flex items-start gap-3">
          <div class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-gray-500">
            📍
          </div>
          <div>
            <p class="font-medium text-gray-900">장소</p>
            <p class="text-sm text-gray-600">{{ schedule.location }}</p>
          </div>
        </div>

        <!-- 면접관 -->
        <div v-if="schedule.interviewerName" class="flex items-start gap-3">
          <div class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-gray-500">
            👤
          </div>
          <div>
            <p class="font-medium text-gray-900">면접관</p>
            <p class="text-sm text-gray-600">{{ schedule.interviewerName }}</p>
          </div>
        </div>

        <!-- 설명 -->
        <div v-if="schedule.description" class="flex items-start gap-3">
          <div class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-gray-500">
            📝
          </div>
          <div>
            <p class="font-medium text-gray-900">안내사항</p>
            <p class="text-sm text-gray-600 whitespace-pre-wrap">{{ schedule.description }}</p>
          </div>
        </div>

        <!-- 메모 (기업만) -->
        <div v-if="isCompany && schedule.memo" class="flex items-start gap-3">
          <div class="w-8 h-8 bg-yellow-100 rounded-lg flex items-center justify-center text-yellow-600">
            📌
          </div>
          <div>
            <p class="font-medium text-gray-900">내부 메모</p>
            <p class="text-sm text-gray-600 whitespace-pre-wrap">{{ schedule.memo }}</p>
          </div>
        </div>
      </div>

      <!-- 액션 버튼 (기업만) -->
      <div v-if="isCompany" class="p-6 border-t bg-gray-50 rounded-b-xl">
        <div class="flex justify-between">
          <button
            @click="emit('delete', schedule.id)"
            class="px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          >
            삭제
          </button>
          <div class="flex gap-2">
            <button
              v-if="schedule.status === 'SCHEDULED'"
              @click="emit('statusChange', schedule.id, 'CANCELLED')"
              class="px-4 py-2 text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
            >
              취소
            </button>
            <button
              v-if="schedule.status === 'SCHEDULED'"
              @click="emit('statusChange', schedule.id, 'NO_SHOW')"
              class="px-4 py-2 text-orange-600 hover:bg-orange-50 rounded-lg transition-colors"
            >
              불참
            </button>
            <button
              v-if="schedule.status === 'SCHEDULED'"
              @click="emit('statusChange', schedule.id, 'COMPLETED')"
              class="px-4 py-2 bg-green-600 text-white hover:bg-green-700 rounded-lg transition-colors"
            >
              완료
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
