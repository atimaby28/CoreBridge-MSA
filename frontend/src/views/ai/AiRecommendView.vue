<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-4xl mx-auto px-4">
      <!-- 헤더 -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">🎯 AI 채용공고 추천</h1>
        <p class="text-gray-500 mt-1">내 이력서를 기반으로 적합한 채용공고를 AI가 추천해드립니다.</p>
      </div>

      <!-- 이력서 로딩 상태 -->
      <div v-if="loadingResume" class="text-center py-12">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
        <p class="mt-2 text-sm text-gray-500">이력서 불러오는 중...</p>
      </div>

      <!-- 이력서 없음 -->
      <div v-else-if="!resumeContent" class="text-center py-12 bg-white rounded-xl shadow-sm border">
        <svg class="w-16 h-16 text-gray-300 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <p class="mt-4 text-gray-500 text-lg">이력서를 먼저 작성해주세요</p>
        <p class="text-sm text-gray-400 mt-1">이력서가 있어야 AI 추천을 받을 수 있습니다</p>
        <router-link
          to="/my/resume"
          class="inline-block mt-4 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >이력서 작성하러 가기</router-link>
      </div>

      <!-- 메인 컨텐츠 -->
      <template v-else>
        <!-- 추천 버튼 -->
        <div class="bg-white rounded-xl shadow-sm border p-6 mb-6">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="font-semibold text-gray-900">📄 {{ resumeTitle }}</h2>
              <p class="text-sm text-gray-500 mt-1">
                이력서 기반으로 매칭합니다
                <span v-if="resumeSkills.length > 0" class="text-blue-600">
                  · {{ resumeSkills.slice(0, 5).join(', ') }}
                  <span v-if="resumeSkills.length > 5"> 외 {{ resumeSkills.length - 5 }}개</span>
                </span>
              </p>
            </div>
            <button
              @click="handleRecommend"
              :disabled="matching"
              :class="[
                'px-6 py-3 rounded-lg font-medium transition-all flex items-center gap-2',
                !matching
                  ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white hover:from-blue-700 hover:to-purple-700 shadow-lg'
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              ]"
            >
              <svg v-if="matching" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ matching ? 'AI 분석 중...' : '🔍 AI 추천 받기' }}
            </button>
          </div>
        </div>

        <!-- 추천 결과 -->
        <div v-if="matching" class="text-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-purple-600 mx-auto"></div>
          <p class="mt-3 text-gray-500">이력서를 분석하고 적합한 채용공고를 찾고 있습니다...</p>
        </div>

        <div v-else-if="matchedJobs.length > 0" class="space-y-4">
          <h3 class="text-lg font-semibold text-gray-900">
            📋 추천 채용공고
            <span class="text-sm font-normal text-gray-500">({{ matchedJobs.length }}건)</span>
          </h3>

          <div
            v-for="(job, index) in matchedJobs"
            :key="job.jobpostingId"
            class="bg-white rounded-xl shadow-sm border p-5 hover:shadow-md transition-shadow"
          >
            <div class="flex items-start justify-between">
              <div class="flex items-start gap-3">
                <div
                  :class="[
                    'w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 mt-0.5',
                    index === 0 ? 'bg-yellow-100 text-yellow-700' :
                    index === 1 ? 'bg-gray-100 text-gray-700' :
                    index === 2 ? 'bg-orange-100 text-orange-700' :
                    'bg-gray-50 text-gray-500'
                  ]"
                >{{ index + 1 }}</div>
                <div>
                  <router-link
                    :to="`/jobpostings/${job.jobpostingId}`"
                    class="font-medium text-gray-900 hover:text-blue-600 transition-colors"
                  >
                    채용공고 #{{ job.jobpostingId }}
                  </router-link>
                  <p v-if="job.title" class="text-sm text-gray-500 mt-0.5">{{ job.title }}</p>
                </div>
              </div>
              <div class="text-right flex-shrink-0">
                <div class="text-2xl font-bold text-blue-600">{{ (job.score * 100).toFixed(0) }}</div>
                <div class="text-xs text-gray-500">매칭도</div>
              </div>
            </div>

            <!-- 스킬 갭 분석 버튼 -->
            <div class="mt-3 pt-3 border-t">
              <button
                @click="handleSkillGap(job.jobpostingId)"
                class="text-sm text-purple-600 hover:text-purple-800 flex items-center gap-1"
              >
                🔍 스킬 갭 분석
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="searched" class="text-center py-12 bg-white rounded-xl shadow-sm border">
          <p class="text-gray-500">매칭된 채용공고가 없습니다</p>
          <p class="text-sm text-gray-400 mt-1">등록된 채용공고가 있는지 확인해주세요</p>
        </div>
      </template>

      <!-- 스킬 갭 모달 -->
      <div v-if="showSkillGap" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <AiSkillGap :gap="skillGapResult" :loading="analyzingGap" @close="showSkillGap = false" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyResume } from '@/api/resume'
import { matchJobpostings, analyzeSkillGap } from '@/api/aiMatching'
import type { MatchedJobposting, SkillGapResponse } from '@/types/aiMatching'
import AiSkillGap from '@/components/ai/AiSkillGap.vue'

const loadingResume = ref(true)
const resumeContent = ref('')
const resumeTitle = ref('')
const resumeId = ref<number | null>(null)
const resumeSkills = ref<string[]>([])

const matching = ref(false)
const searched = ref(false)
const matchedJobs = ref<MatchedJobposting[]>([])

const showSkillGap = ref(false)
const analyzingGap = ref(false)
const skillGapResult = ref<SkillGapResponse | null>(null)

onMounted(async () => {
  try {
    const resume = await getMyResume()
    resumeContent.value = resume.content || ''
    resumeTitle.value = resume.title || '내 이력서'
    resumeId.value = resume.resumeId
    resumeSkills.value = resume.skills || resume.aiSkills || []
  } catch (e) {
    console.error('이력서 조회 실패:', e)
  } finally {
    loadingResume.value = false
  }
})

async function handleRecommend() {
  if (!resumeContent.value) return
  matching.value = true
  searched.value = false
  matchedJobs.value = []

  try {
    const response = await matchJobpostings({
      resumeText: resumeContent.value,
      topK: 10,
    })
    matchedJobs.value = response.matches
    searched.value = true
  } catch (e) {
    console.error('추천 실패:', e)
    alert('AI 추천에 실패했습니다.')
  } finally {
    matching.value = false
  }
}

async function handleSkillGap(jobpostingId: string) {
  if (!resumeId.value) return
  showSkillGap.value = true
  analyzingGap.value = true
  skillGapResult.value = null

  try {
    skillGapResult.value = await analyzeSkillGap({
      candidateId: String(resumeId.value),
      jobpostingId: jobpostingId,
    })
  } catch (e) {
    console.error('스킬 갭 분석 실패:', e)
    alert('스킬 갭 분석에 실패했습니다.')
    showSkillGap.value = false
  } finally {
    analyzingGap.value = false
  }
}
</script>
