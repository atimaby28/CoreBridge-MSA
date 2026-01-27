<template>
  <div class="bg-white rounded-xl shadow-lg p-6 space-y-6">
    <!-- 헤더 -->
    <div class="flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900">
        📊 상세 분석 결과
      </h3>
      <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto"></div>
      <p class="mt-2 text-sm text-gray-500">분석 중...</p>
    </div>

    <!-- 결과 -->
    <template v-else-if="score">
      <!-- 등급 & 총점 -->
      <div class="flex items-center justify-center gap-6">
        <div
          :class="[
            'w-20 h-20 rounded-full flex items-center justify-center text-3xl font-bold border-4',
            gradeColor
          ]"
        >
          {{ score.scoreDetail.grade }}
        </div>
        <div class="text-center">
          <div class="text-4xl font-bold text-gray-900">
            {{ score.scoreDetail.totalScore.toFixed(0) }}
          </div>
          <div class="text-sm text-gray-500">총점 (100점 만점)</div>
        </div>
      </div>

      <!-- 점수 상세 바 -->
      <div class="space-y-4">
        <!-- 스킬 점수 -->
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600">스킬 매칭</span>
            <span class="font-medium">{{ score.scoreDetail.skillScore.toFixed(0) }}/40</span>
          </div>
          <div class="h-3 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-blue-500 rounded-full transition-all"
              :style="{ width: `${(score.scoreDetail.skillScore / 40) * 100}%` }"
            ></div>
          </div>
        </div>

        <!-- 유사도 점수 -->
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600">문서 유사도</span>
            <span class="font-medium">{{ score.scoreDetail.similarityScore.toFixed(0) }}/40</span>
          </div>
          <div class="h-3 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-purple-500 rounded-full transition-all"
              :style="{ width: `${(score.scoreDetail.similarityScore / 40) * 100}%` }"
            ></div>
          </div>
        </div>

        <!-- 보너스 점수 -->
        <div>
          <div class="flex justify-between text-sm mb-1">
            <span class="text-gray-600">보너스</span>
            <span class="font-medium">{{ score.scoreDetail.bonusScore.toFixed(0) }}/20</span>
          </div>
          <div class="h-3 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-green-500 rounded-full transition-all"
              :style="{ width: `${(score.scoreDetail.bonusScore / 20) * 100}%` }"
            ></div>
          </div>
        </div>
      </div>

      <!-- 스킬 비교 -->
      <div class="grid grid-cols-2 gap-4">
        <!-- JD 요구 스킬 -->
        <div>
          <h4 class="text-sm font-medium text-gray-700 mb-2">📋 요구 스킬</h4>
          <div class="flex flex-wrap gap-1">
            <span
              v-for="skill in score.requiredSkills"
              :key="skill"
              :class="[
                'px-2 py-1 text-xs rounded-full',
                score.candidateSkills.includes(skill)
                  ? 'bg-green-100 text-green-800'
                  : 'bg-red-100 text-red-800'
              ]"
            >
              {{ skill }}
              <span v-if="score.candidateSkills.includes(skill)">✓</span>
            </span>
          </div>
        </div>

        <!-- 후보자 스킬 -->
        <div>
          <h4 class="text-sm font-medium text-gray-700 mb-2">👤 보유 스킬</h4>
          <div class="flex flex-wrap gap-1">
            <span
              v-for="skill in score.candidateSkills"
              :key="skill"
              :class="[
                'px-2 py-1 text-xs rounded-full',
                score.requiredSkills.includes(skill)
                  ? 'bg-green-100 text-green-800'
                  : 'bg-blue-100 text-blue-800'
              ]"
            >
              {{ skill }}
            </span>
          </div>
        </div>
      </div>

      <!-- 코사인 유사도 -->
      <div class="text-center text-sm text-gray-500">
        벡터 유사도: {{ (score.cosineSimilarity * 100).toFixed(1) }}%
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiScoreResponse } from '@/types/aiMatching'
import { GradeColors } from '@/types/aiMatching'

const props = defineProps<{
  score: AiScoreResponse | null
  loading: boolean
}>()

defineEmits<{
  (e: 'close'): void
}>()

const gradeColor = computed(() => {
  if (!props.score) return ''
  return GradeColors[props.score.scoreDetail.grade] || ''
})
</script>
