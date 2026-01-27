<template>
  <div class="space-y-4">
    <!-- 매칭 결과 헤더 -->
    <div class="flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
        🤖 AI 매칭 결과
        <span class="text-sm font-normal text-gray-500">
          ({{ matches.length }}명)
        </span>
      </h3>
      <button
        v-if="!loading"
        @click="$emit('refresh')"
        class="text-sm text-blue-600 hover:text-blue-800 flex items-center gap-1"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        새로고침
      </button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto"></div>
      <p class="mt-2 text-sm text-gray-500">매칭 중...</p>
    </div>

    <!-- 결과 없음 -->
    <div v-else-if="matches.length === 0" class="text-center py-8 bg-gray-50 rounded-lg">
      <svg class="w-12 h-12 text-gray-400 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p class="mt-2 text-gray-500">매칭된 후보자가 없습니다</p>
      <p class="text-sm text-gray-400">이력서가 등록되어 있는지 확인해주세요</p>
    </div>

    <!-- 매칭 결과 리스트 -->
    <div v-else class="space-y-3">
      <div
        v-for="(match, index) in matches"
        :key="match.candidateId"
        class="bg-white border rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
        @click="$emit('select', match)"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <!-- 순위 -->
            <div
              :class="[
                'w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold',
                index === 0 ? 'bg-yellow-100 text-yellow-700' :
                index === 1 ? 'bg-gray-100 text-gray-700' :
                index === 2 ? 'bg-orange-100 text-orange-700' :
                'bg-gray-50 text-gray-500'
              ]"
            >
              {{ index + 1 }}
            </div>

            <!-- 후보자 정보 -->
            <div>
              <div class="font-medium text-gray-900">
                후보자 #{{ match.candidateId }}
              </div>
              <div v-if="match.skills?.length" class="flex flex-wrap gap-1 mt-1">
                <span
                  v-for="skill in match.skills.slice(0, 5)"
                  :key="skill"
                  class="px-2 py-0.5 bg-blue-50 text-blue-700 text-xs rounded-full"
                >
                  {{ skill }}
                </span>
                <span v-if="match.skills.length > 5" class="text-xs text-gray-400">
                  +{{ match.skills.length - 5 }}
                </span>
              </div>
            </div>
          </div>

          <!-- 점수 -->
          <div class="text-right">
            <div class="text-2xl font-bold text-purple-600">
              {{ (match.score * 100).toFixed(0) }}
            </div>
            <div class="text-xs text-gray-500">유사도</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MatchedCandidate } from '@/types/aiMatching'

defineProps<{
  matches: MatchedCandidate[]
  loading: boolean
}>()

defineEmits<{
  (e: 'select', match: MatchedCandidate): void
  (e: 'refresh'): void
}>()
</script>
