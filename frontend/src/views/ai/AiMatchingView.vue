<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-6xl mx-auto px-4">
      <!-- 헤더 -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">🤖 AI 이력서 매칭</h1>
        <p class="text-gray-500 mt-1">내 채용공고를 선택하면 적합한 후보자를 AI가 매칭해드립니다.</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 왼쪽: 채용공고 선택 -->
        <div class="space-y-4">
          <div class="bg-white rounded-xl shadow-sm border p-6">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">📋 채용공고 선택</h2>

            <!-- 로딩 -->
            <div v-if="loadingJobpostings" class="text-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto"></div>
              <p class="mt-2 text-sm text-gray-500">내 채용공고 불러오는 중...</p>
            </div>

            <!-- 채용공고 없음 -->
            <div v-else-if="myJobpostings.length === 0" class="text-center py-8 text-gray-500">
              <svg class="w-12 h-12 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p class="font-medium">등록된 채용공고가 없습니다</p>
              <p class="text-sm mt-1">채용공고를 먼저 작성해주세요</p>
              <router-link
                to="/jobpostings/create"
                class="inline-block mt-3 px-4 py-2 bg-purple-600 text-white text-sm rounded-lg hover:bg-purple-700 transition-colors"
              >채용공고 작성하러 가기</router-link>
            </div>

            <!-- 채용공고 드롭다운 선택 -->
            <div v-else class="space-y-3">
              <!-- 드롭다운 셀렉터 -->
              <div class="relative" ref="dropdownRef">
                <button
                  @click="showDropdown = !showDropdown"
                  class="w-full px-4 py-3 border-2 rounded-lg text-left flex items-center justify-between transition-all"
                  :class="selectedJobposting ? 'border-purple-500 bg-purple-50' : 'border-gray-200 hover:border-purple-300'"
                >
                  <div class="flex-1 min-w-0">
                    <span v-if="selectedJobposting" class="font-medium text-gray-900">{{ selectedJobposting.title }}</span>
                    <span v-else class="text-gray-400">채용공고를 선택하세요 ({{ myJobpostings.length }}건)</span>
                  </div>
                  <svg
                    class="w-5 h-5 text-gray-400 flex-shrink-0 ml-2 transition-transform duration-200"
                    :class="{ 'rotate-180': showDropdown }"
                    fill="none" stroke="currentColor" viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                <!-- 드롭다운 목록 -->
                <div
                  v-if="showDropdown"
                  class="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto"
                >
                  <div
                    v-for="jp in myJobpostings"
                    :key="jp.jobpostingId"
                    @click="selectJobposting(jp); showDropdown = false"
                    class="px-4 py-3 cursor-pointer transition-colors flex items-center justify-between"
                    :class="[
                      selectedJobposting?.jobpostingId === jp.jobpostingId
                        ? 'bg-purple-50 text-purple-700'
                        : 'hover:bg-gray-50 text-gray-700'
                    ]"
                  >
                    <div class="flex-1 min-w-0">
                      <p class="font-medium truncate">{{ jp.title }}</p>
                      <div v-if="jp.requiredSkills && jp.requiredSkills.length > 0" class="mt-1 flex flex-wrap gap-1">
                        <span
                          v-for="skill in jp.requiredSkills.slice(0, 3)"
                          :key="skill"
                          class="px-1.5 py-0.5 bg-purple-100 text-purple-600 text-xs rounded"
                        >{{ skill }}</span>
                        <span v-if="jp.requiredSkills.length > 3" class="text-xs text-gray-400">+{{ jp.requiredSkills.length - 3 }}</span>
                      </div>
                    </div>
                    <svg
                      v-if="selectedJobposting?.jobpostingId === jp.jobpostingId"
                      class="w-5 h-5 text-purple-600 flex-shrink-0 ml-2"
                      fill="currentColor" viewBox="0 0 20 20"
                    >
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- 선택된 공고 상세 (아코디언 펼침) -->
              <div
                v-if="selectedJobposting"
                class="border-2 border-purple-200 bg-purple-50 rounded-lg p-4 transition-all"
              >
                <div class="flex items-center gap-2 mb-2">
                  <svg class="w-5 h-5 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                  <h3 class="font-semibold text-purple-800">{{ selectedJobposting.title }}</h3>
                </div>
                <p class="text-sm text-gray-600 line-clamp-3">{{ selectedJobposting.content }}</p>
                <div v-if="selectedJobposting.requiredSkills && selectedJobposting.requiredSkills.length > 0" class="mt-3 flex flex-wrap gap-1.5">
                  <span
                    v-for="skill in selectedJobposting.requiredSkills"
                    :key="skill"
                    class="px-2 py-0.5 bg-purple-100 text-purple-700 text-xs rounded-full"
                  >{{ skill }}</span>
                </div>
              </div>
            </div>

            <!-- 추가 옵션 -->
            <div v-if="selectedJobposting" class="mt-4 pt-4 border-t border-gray-100">
              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-2">필수 스킬 (선택, 수정 가능)</label>
                <input
                  v-model="skillsInput"
                  type="text"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  placeholder="Java, Spring Boot, MySQL (쉼표로 구분)"
                />
                <p class="text-xs text-gray-500 mt-1">채용공고의 스킬이 자동으로 채워집니다. 수정 가능합니다.</p>
              </div>

              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-2">검색 인원</label>
                <select v-model="topK" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent">
                  <option :value="5">상위 5명</option>
                  <option :value="10">상위 10명</option>
                  <option :value="20">상위 20명</option>
                </select>
              </div>
            </div>

            <button
              @click="handleMatch"
              :disabled="!selectedJobposting || matching"
              :class="[
                'w-full mt-4 py-3 rounded-lg font-medium transition-all flex items-center justify-center gap-2',
                selectedJobposting && !matching
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
              <li>1. 매칭할 채용공고를 선택합니다</li>
              <li>2. 필수 스킬을 수정하거나 확인합니다</li>
              <li>3. AI 매칭 버튼을 클릭합니다</li>
              <li>4. 매칭된 후보자를 클릭하면 상세 점수를 확인할 수 있습니다</li>
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
            <AiScoreDetail :score="scoreResult" :match-score="selectedCandidate?.score ?? null" :loading="scoring" @close="selectedCandidate = null" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { matchCandidates, scoreCandidate } from '@/api/aiMatching'
import { jobpostingService } from '@/api/jobposting'
import type { MatchedCandidate, AiScoreResponse } from '@/types/aiMatching'
import type { Jobposting } from '@/types/jobposting'
import AiMatchingResult from '@/components/ai/AiMatchingResult.vue'
import AiScoreDetail from '@/components/ai/AiScoreDetail.vue'

const loadingJobpostings = ref(true)
const myJobpostings = ref<Jobposting[]>([])
const selectedJobposting = ref<Jobposting | null>(null)
const skillsInput = ref('')
const topK = ref(10)
const matching = ref(false)
const scoring = ref(false)
const matches = ref<MatchedCandidate[]>([])
const selectedCandidate = ref<MatchedCandidate | null>(null)
const scoreResult = ref<AiScoreResponse | null>(null)

// 드롭다운 상태
const showDropdown = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

const requiredSkills = computed(() => {
  if (!skillsInput.value.trim()) return undefined
  return skillsInput.value.split(',').map(s => s.trim()).filter(s => s)
})

// 드롭다운 외부 클릭 감지
function handleClickOutside(e: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  try {
    const response = await jobpostingService.getMyJobpostings()
    myJobpostings.value = response.jobpostings || []
  } catch (e) {
    console.error('내 채용공고 조회 실패:', e)
  } finally {
    loadingJobpostings.value = false
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 채용공고 선택
function selectJobposting(jp: Jobposting) {
  selectedJobposting.value = jp
  const skills: string[] = []
  if (jp.requiredSkills) skills.push(...jp.requiredSkills)
  if (jp.preferredSkills) skills.push(...jp.preferredSkills)
  skillsInput.value = [...new Set(skills)].join(', ')
}

// JD 텍스트 조합
function buildJdText(): string {
  if (!selectedJobposting.value) return ''
  const jp = selectedJobposting.value
  const parts: string[] = []
  if (jp.title) parts.push(jp.title)
  if (jp.content) parts.push(jp.content)
  if (jp.requiredSkills?.length) parts.push('필수 스킬: ' + jp.requiredSkills.join(', '))
  if (jp.preferredSkills?.length) parts.push('우대 스킬: ' + jp.preferredSkills.join(', '))
  return parts.join('\n')
}

async function handleMatch() {
  if (!selectedJobposting.value) return
  matching.value = true
  matches.value = []
  try {
    const response = await matchCandidates({
      jdText: buildJdText(),
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
      jdText: buildJdText(),
      requiredSkills: requiredSkills.value,
    })
  } catch (e) {
    console.error('스코어 계산 실패:', e)
    alert('상세 점수 계산에 실패했습니다. AI 분석에 시간이 오래 걸릴 수 있습니다. 다시 시도해주세요.')
  } finally {
    scoring.value = false
    if (!scoreResult.value) {
      selectedCandidate.value = null
    }
  }
}
</script>