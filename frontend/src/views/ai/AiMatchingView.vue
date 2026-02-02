<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-6xl mx-auto px-4">
      <!-- 헤더 -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">🤖 AI 이력서 매칭</h1>
        <p class="text-gray-500 mt-1">채용공고 내용을 입력하면 적합한 후보자를 AI가 매칭해드립니다.</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 왼쪽: JD 입력 -->
        <div class="space-y-4">
          <div class="bg-white rounded-xl shadow-sm border p-6">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">📋 채용공고 입력</h2>

            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">채용공고 내용</label>
              <textarea
                v-model="jdText"
                rows="10"
                class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent resize-none"
                placeholder="채용공고 내용을 입력하세요...

예시:
- 직무: 백엔드 개발자
- 자격요건: Java, Spring Boot, JPA, MySQL 경험자
- 우대사항: MSA, Kafka, Kubernetes 경험"
              ></textarea>
            </div>

            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">필수 스킬 (선택)</label>
              <input
                v-model="skillsInput"
                type="text"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Java, Spring Boot, MySQL (쉼표로 구분)"
              />
              <p class="text-xs text-gray-500 mt-1">입력하지 않으면 AI가 자동 추출합니다</p>
            </div>

            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">검색 인원</label>
              <select v-model="topK" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent">
                <option :value="5">상위 5명</option>
                <option :value="10">상위 10명</option>
                <option :value="20">상위 20명</option>
              </select>
            </div>

            <button
              @click="handleMatch"
              :disabled="!jdText.trim() || matching"
              :class="[
                'w-full py-3 rounded-lg font-medium transition-all flex items-center justify-center gap-2',
                jdText.trim() && !matching
                  ? 'bg-gradient-to-r from-purple-600 to-blue-600 text-white hover:from-purple-700 hover:to-blue-700 shadow-lg'
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              ]"
            >
              <svg v-if="matching" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ matching ? 'AI 매칭 중...' : '🔍 AI 매칭 시작' }}
            </button>
          </div>

          <div class="bg-blue-50 rounded-xl p-4 border border-blue-100">
            <h4 class="font-medium text-blue-800 mb-2">💡 사용 방법</h4>
            <ul class="text-sm text-blue-700 space-y-1">
              <li>1. 채용공고 내용을 입력합니다</li>
              <li>2. AI 매칭 버튼을 클릭합니다</li>
              <li>3. 매칭된 후보자를 클릭하면 상세 점수를 확인할 수 있습니다</li>
            </ul>
          </div>
        </div>

        <!-- 오른쪽: 결과 -->
        <div class="space-y-4">
          <div class="bg-white rounded-xl shadow-sm border p-6">
            <AiMatchingResult :matches="matches" :loading="matching" @select="handleSelectCandidate" @refresh="handleMatch" />
          </div>

          <!-- 상세 점수 모달 -->
          <div v-if="selectedCandidate" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <AiScoreDetail :score="scoreResult" :loading="scoring" @close="selectedCandidate = null" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { matchCandidates, scoreCandidate } from '@/api/aiMatching'
import type { MatchedCandidate, AiScoreResponse } from '@/types/aiMatching'
import AiMatchingResult from '@/components/ai/AiMatchingResult.vue'
import AiScoreDetail from '@/components/ai/AiScoreDetail.vue'

const jdText = ref('')
const skillsInput = ref('')
const topK = ref(10)
const matching = ref(false)
const scoring = ref(false)
const matches = ref<MatchedCandidate[]>([])
const selectedCandidate = ref<MatchedCandidate | null>(null)
const scoreResult = ref<AiScoreResponse | null>(null)

const requiredSkills = computed(() => {
  if (!skillsInput.value.trim()) return undefined
  return skillsInput.value.split(',').map(s => s.trim()).filter(s => s)
})

async function handleMatch() {
  if (!jdText.value.trim()) return
  matching.value = true
  matches.value = []
  try {
    const response = await matchCandidates({
      jdText: jdText.value,
      requiredSkills: requiredSkills.value,
      topK: topK.value,
    })
    matches.value = response.matches
  } catch (e) {
    console.error('매칭 실패:', e)
    alert('AI 매칭에 실패했습니다.')
  } finally {
    matching.value = false
  }
}

async function handleSelectCandidate(candidate: MatchedCandidate) {
  selectedCandidate.value = candidate
  scoring.value = true
  scoreResult.value = null
  try {
    scoreResult.value = await scoreCandidate({
      candidateId: candidate.candidateId,
      jdText: jdText.value,
      requiredSkills: requiredSkills.value,
    })
  } catch (e) {
    console.error('스코어 계산 실패:', e)
    alert('상세 점수 계산에 실패했습니다.')
    selectedCandidate.value = null
  } finally {
    scoring.value = false
  }
}
</script>
