<template>
  <div class="bg-white rounded-xl shadow-lg p-6 space-y-6 max-w-lg w-full">
    <div class="flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900">🔍 스킬 갭 분석</h3>
      <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <div v-if="loading" class="text-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
      <p class="mt-2 text-sm text-gray-500">분석 중...</p>
    </div>

    <template v-else-if="gap">
      <!-- 매치율 -->
      <div class="text-center">
        <div class="text-5xl font-bold" :class="matchRateColor">
          {{ (gap.matchRate * 100).toFixed(0) }}%
        </div>
        <div class="text-sm text-gray-500 mt-1">스킬 매치율</div>
        <div class="text-xs text-gray-400 mt-1">코사인 유사도: {{ (gap.cosineSimilarity * 100).toFixed(1) }}%</div>
      </div>

      <!-- 매칭된 스킬 -->
      <div v-if="gap.matchedSkills.length > 0">
        <h4 class="text-sm font-medium text-green-700 mb-2">✅ 보유 중인 요구 스킬</h4>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="skill in gap.matchedSkills" :key="skill"
            class="px-3 py-1 bg-green-100 text-green-800 text-sm rounded-full"
          >{{ skill }}</span>
        </div>
      </div>

      <!-- 부족한 스킬 -->
      <div v-if="gap.missingSkills.length > 0">
        <h4 class="text-sm font-medium text-red-700 mb-2">❌ 부족한 스킬</h4>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="skill in gap.missingSkills" :key="skill"
            class="px-3 py-1 bg-red-100 text-red-800 text-sm rounded-full"
          >{{ skill }}</span>
        </div>
        <p class="text-xs text-gray-500 mt-2">
          이 스킬들을 보완하면 이 채용공고에 더 적합한 후보자가 될 수 있습니다.
        </p>
      </div>

      <!-- 전체 보유 스킬 -->
      <div>
        <h4 class="text-sm font-medium text-gray-700 mb-2">💼 내 전체 스킬</h4>
        <div class="flex flex-wrap gap-1">
          <span
            v-for="skill in gap.candidateSkills" :key="skill"
            :class="[
              'px-2 py-1 text-xs rounded-full',
              gap.matchedSkills.map(s => s.toLowerCase()).includes(skill.toLowerCase())
                ? 'bg-green-100 text-green-800'
                : 'bg-gray-100 text-gray-600'
            ]"
          >{{ skill }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SkillGapResponse } from '@/types/aiMatching'

const props = defineProps<{
  gap: SkillGapResponse | null
  loading: boolean
}>()

defineEmits<{ (e: 'close'): void }>()

const matchRateColor = computed(() => {
  if (!props.gap) return 'text-gray-400'
  const rate = props.gap.matchRate
  if (rate >= 0.8) return 'text-green-600'
  if (rate >= 0.5) return 'text-yellow-600'
  return 'text-red-600'
})
</script>
