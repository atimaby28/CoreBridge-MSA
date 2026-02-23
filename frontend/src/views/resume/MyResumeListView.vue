<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <!-- 헤더 -->
    <div class="mb-6 flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">내 이력서</h1>
        <p class="mt-1 text-sm text-gray-500">
          이력서를 작성하고 AI 분석을 통해 개선점을 확인하세요.
        </p>
      </div>
      <div class="flex gap-2">
        <!-- 수정 모드 토글 -->
        <button
          v-if="!isEditing && resume?.content"
          @click="startEditing"
          class="px-4 py-2 rounded-lg font-medium bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          수정
        </button>
        
        <!-- AI 분석 버튼 -->
        <button
          v-if="resume && resume.content && !isEditing"
          @click="handleAnalyze"
          :disabled="isAnalyzing || analyzing"
          :class="[
            'px-4 py-2 rounded-lg font-medium transition-all flex items-center gap-2',
            isAnalyzing || analyzing
              ? 'bg-yellow-100 text-yellow-800 cursor-wait'
              : 'bg-gradient-to-r from-purple-600 to-blue-600 text-white hover:from-purple-700 hover:to-blue-700 shadow-lg hover:shadow-xl'
          ]"
        >
          <svg v-if="isAnalyzing || analyzing" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          {{ isAnalyzing || analyzing ? 'AI 분석 중...' : '🤖 AI 분석' }}
        </button>
      </div>
    </div>

    <!-- 로딩 -->
    <div v-if="loading && !resume" class="text-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">불러오는 중...</p>
    </div>

    <!-- 메인 컨텐츠 -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 왼쪽: 이력서 영역 -->
      <div class="lg:col-span-2 space-y-4">
        <!-- 이력서 카드 -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="p-4 border-b border-gray-100 flex justify-between items-center">
            <div class="flex items-center gap-2">
              <span class="text-lg font-semibold text-gray-900">📝 이력서 내용</span>
              <span v-if="resume" class="text-xs text-gray-400">v{{ resume.currentVersion }}</span>
              <span v-if="isEditing" class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">수정 중</span>
            </div>
            <span v-if="resume" :class="['px-2 py-1 rounded-full text-xs font-medium', getStatusColor(resume.status)]">
              {{ getStatusName(resume.status) }}
            </span>
          </div>
          
          <div class="p-4 space-y-4">
            <!-- 제목 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">제목</label>
              <input
                v-if="isEditing"
                v-model="editForm.title"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                placeholder="이력서 제목을 입력하세요"
              />
              <div v-else class="px-3 py-2 bg-gray-50 rounded-lg text-gray-900">
                {{ resume?.title || '제목 없음' }}
              </div>
            </div>
            
            <!-- 내용 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">내용</label>
              <!-- 수정 모드 -->
              <textarea
                v-if="isEditing"
                v-model="editForm.content"
                rows="16"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm transition-colors"
                placeholder="이력서 내용을 작성하세요...

예시:
# 기본 정보
- 이름: 홍길동
- 연락처: hong@email.com

# 경력
## ABC 회사 (2020.03 ~ 현재)
- 백엔드 개발
- Spring Boot, Kubernetes 활용

# 기술 스택
Java, Spring Boot, PostgreSQL, Redis, Kubernetes"
              ></textarea>
              <!-- 읽기 모드 -->
              <div v-else class="bg-gray-50 rounded-lg p-4 min-h-[400px]">
                <pre v-if="resume?.content" class="whitespace-pre-wrap font-mono text-sm text-gray-800">{{ resume.content }}</pre>
                <div v-else class="text-center text-gray-400 py-16">
                  <svg class="mx-auto h-12 w-12 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <p>이력서 내용이 없습니다.</p>
                  <button
                    @click="startEditing"
                    class="mt-4 text-blue-600 hover:underline"
                  >
                    작성 시작하기
                  </button>
                </div>
              </div>
            </div>

            <!-- 보유 스킬 태그 -->
            <div>
              <template v-if="isEditing">
                <SkillTagInput
                  v-model="editForm.skills"
                  label="보유 스킬"
                  placeholder="보유 스킬을 입력하세요"
                  help-text="Enter로 태그 추가 (AI 매칭에 활용됩니다)"
                  color="blue"
                  :suggestions="SKILL_SUGGESTIONS"
                />
              </template>
              <template v-else-if="resume?.skills && resume.skills.length > 0">
                <label class="block text-sm font-medium text-gray-700 mb-2">보유 스킬</label>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="skill in resume.skills"
                    :key="skill"
                    class="px-3 py-1 bg-blue-100 text-blue-700 text-sm rounded-full font-medium"
                  >
                    {{ skill }}
                  </span>
                </div>
              </template>
            </div>

            <!-- 수정 모드 버튼 -->
            <div v-if="isEditing" class="flex justify-between items-center pt-2 border-t border-gray-100">
              <div class="text-xs text-gray-400">
                수정 후 저장하면 이전 버전이 자동 백업됩니다.
              </div>
              <div class="flex gap-2">
                <button
                  @click="cancelEditing"
                  class="px-4 py-2 rounded-lg font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                >
                  취소
                </button>
                <button
                  @click="handleSave"
                  :disabled="loading || !hasChanges"
                  :class="[
                    'px-4 py-2 rounded-lg font-medium transition-colors',
                    hasChanges
                      ? 'bg-blue-600 text-white hover:bg-blue-700'
                      : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                  ]"
                >
                  {{ loading ? '저장 중...' : '💾 저장' }}
                </button>
              </div>
            </div>

            <!-- 읽기 모드 하단 정보 -->
            <div v-else class="flex justify-between items-center pt-2 border-t border-gray-100">
              <div class="text-xs text-gray-400">
                <span v-if="resume">마지막 수정: {{ formatDate(resume.updatedAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 버전 히스토리 -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="p-4 border-b border-gray-100 flex justify-between items-center">
            <span class="text-lg font-semibold text-gray-900">📜 버전 기록</span>
            <span class="text-xs text-gray-400">{{ versions.length }}개 버전</span>
          </div>
          
          <div v-if="versions.length === 0" class="p-6 text-center text-gray-500">
            <svg class="mx-auto h-10 w-10 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p>저장된 버전이 없습니다.</p>
            <p class="text-sm mt-1">이력서를 수정하고 저장하면 버전이 기록됩니다.</p>
          </div>
          
          <div v-else class="divide-y divide-gray-100 max-h-72 overflow-y-auto">
            <div
              v-for="ver in versions"
              :key="ver.versionId"
              class="p-3 hover:bg-gray-50 flex justify-between items-center transition-colors"
            >
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-sm font-medium">
                  {{ ver.version }}
                </div>
                <div>
                  <span class="font-medium text-gray-900">{{ ver.title || '제목 없음' }}</span>
                  <p class="text-xs text-gray-500">{{ formatDate(ver.createdAt) }}</p>
                  <p v-if="ver.memo" class="text-xs text-gray-400 mt-0.5">{{ ver.memo }}</p>
                </div>
              </div>
              <div class="flex gap-1">
                <button
                  @click="handlePreview(ver)"
                  class="px-3 py-1.5 text-xs text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                >
                  보기
                </button>
                <button
                  @click="handleRestore(ver.version)"
                  class="px-3 py-1.5 text-xs text-blue-600 hover:bg-blue-100 rounded-lg transition-colors font-medium"
                >
                  복원
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽: AI 분석 결과 -->
      <div class="space-y-4">
        <!-- AI 분석 상태 카드 -->
        <div class="bg-gradient-to-br from-purple-50 to-blue-50 rounded-xl shadow-sm border border-purple-100 overflow-hidden">
          <div class="p-4 border-b border-purple-100">
            <span class="text-lg font-semibold text-purple-900">🤖 AI 분석 결과</span>
          </div>
          
          <div class="p-4">
            <!-- 분석 전 상태 -->
            <div v-if="!resume?.aiSummary && !isAnalyzing" class="text-center py-6">
              <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-purple-100 flex items-center justify-center">
                <svg class="w-8 h-8 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
              </div>
              <p class="text-purple-700 font-medium">AI 분석을 시작하세요</p>
              <p class="text-sm text-purple-500 mt-1">
                이력서 내용을 분석하여<br/>요약, 스킬, 경력을 추출합니다.
              </p>
            </div>

            <!-- 분석 중 상태 -->
            <div v-else-if="isAnalyzing" class="text-center py-6">
              <div class="w-16 h-16 mx-auto mb-4">
                <svg class="animate-spin w-full h-full text-purple-500" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </div>
              <p class="text-purple-700 font-medium">AI가 분석 중입니다...</p>
              <p class="text-sm text-purple-500 mt-1">약 1~2분 정도 소요됩니다.</p>
            </div>

            <!-- 분석 완료 상태 -->
            <div v-else class="space-y-4">
              <!-- 요약 -->
              <div>
                <h4 class="text-sm font-medium text-purple-800 mb-2 flex items-center gap-1">
                  📝 AI 요약
                </h4>
                <p class="text-sm text-gray-700 bg-white rounded-lg p-3 border border-purple-100">
                  {{ resume.aiSummary }}
                </p>
              </div>

              <!-- 추출된 스킬 -->
              <div v-if="resume.aiSkills && resume.aiSkills.length > 0">
                <h4 class="text-sm font-medium text-purple-800 mb-2 flex items-center gap-1">
                  🛠 추출된 기술 스택
                </h4>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="skill in resume.aiSkills"
                    :key="skill"
                    class="px-2 py-1 bg-white text-purple-700 text-sm rounded-full border border-purple-200"
                  >
                    {{ skill }}
                  </span>
                </div>
              </div>

              <!-- 분석 일시 -->
              <div class="text-xs text-purple-400 text-right">
                분석 일시: {{ formatDate(resume.analyzedAt) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 도움말 -->
        <div class="bg-blue-50 rounded-xl p-4 border border-blue-100">
          <h4 class="font-medium text-blue-800 mb-2">💡 AI Pipeline 기능</h4>
          <ul class="text-sm text-blue-700 space-y-1">
            <li>• FastAPI + Ollama 기반 이력서 자동 요약</li>
            <li>• 기술 스택 자동 추출 및 태깅</li>
            <li>• JD 매칭 점수 산출 (코사인 유사도)</li>
            <li>• 스킬 갭 분석 (보유/부족 스킬 비교)</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 버전 미리보기 모달 -->
    <div v-if="showPreviewModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-hidden">
        <div class="p-4 border-b flex justify-between items-center">
          <h3 class="font-semibold">
            <span class="text-blue-600">v{{ selectedVersion?.version }}</span> 미리보기
          </h3>
          <button @click="showPreviewModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="p-4 overflow-y-auto max-h-[60vh]">
          <div class="mb-4">
            <span class="text-sm font-medium text-gray-500">제목</span>
            <p class="mt-1 text-gray-900">{{ selectedVersion?.title || '(제목 없음)' }}</p>
          </div>
          <div>
            <span class="text-sm font-medium text-gray-500">내용</span>
            <pre class="mt-2 p-4 bg-gray-50 rounded-lg text-sm whitespace-pre-wrap font-mono text-gray-800 border">{{ selectedVersion?.content || '(내용 없음)' }}</pre>
          </div>
          <div class="mt-4 text-xs text-gray-400">
            저장 일시: {{ formatDate(selectedVersion?.createdAt) }}
          </div>
        </div>
        <div class="p-4 border-t flex justify-end gap-2 bg-gray-50">
          <button
            @click="showPreviewModal = false"
            class="px-4 py-2 text-gray-700 hover:bg-gray-200 rounded-lg transition-colors"
          >
            닫기
          </button>
          <button
            @click="handleRestore(selectedVersion!.version); showPreviewModal = false"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            이 버전으로 복원
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useResumeStore } from '@/stores/resume'
import { storeToRefs } from 'pinia'
import { ResumeStatusNames, ResumeStatusColors, type ResumeStatus, type VersionResponse } from '@/types/resume'
import SkillTagInput from '@/components/common/SkillTagInput.vue'

// 추천 스킬 목록
const SKILL_SUGGESTIONS = [
  'Java', 'Spring Boot', 'Python', 'JavaScript', 'TypeScript',
  'React', 'Vue.js', 'Node.js', 'Go', 'Kotlin',
  'MySQL', 'PostgreSQL', 'MongoDB', 'Redis', 'Kafka',
  'Docker', 'Kubernetes', 'AWS', 'GCP', 'Jenkins'
]

const resumeStore = useResumeStore()
const { resume, versions, loading, analyzing, error } = storeToRefs(resumeStore)

const isAnalyzing = computed(() => resume.value?.status === 'ANALYZING')

// 수정 모드 상태
const isEditing = ref(false)
const showPreviewModal = ref(false)
const selectedVersion = ref<VersionResponse | null>(null)

const editForm = reactive({
  title: '',
  content: '',
  skills: [] as string[],
})

// 변경 사항 있는지 확인
const hasChanges = computed(() => {
  if (!resume.value) return editForm.title.length > 0 || editForm.content.length > 0 || editForm.skills.length > 0
  const titleChanged = editForm.title !== (resume.value.title || '')
  const contentChanged = editForm.content !== (resume.value.content || '')
  const skillsChanged = JSON.stringify(editForm.skills) !== JSON.stringify(resume.value.skills || [])
  return titleChanged || contentChanged || skillsChanged
})

function getStatusColor(status: ResumeStatus): string {
  return ResumeStatusColors[status] || 'bg-gray-100 text-gray-800'
}

function getStatusName(status: ResumeStatus): string {
  return ResumeStatusNames[status] || status
}

function formatDate(dateString?: string): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 수정 모드 시작
function startEditing() {
  editForm.title = resume.value?.title || ''
  editForm.content = resume.value?.content || ''
  editForm.skills = [...(resume.value?.skills || [])]
  isEditing.value = true
}

// 수정 취소
function cancelEditing() {
  isEditing.value = false
  // 폼 초기화
  editForm.title = resume.value?.title || ''
  editForm.content = resume.value?.content || ''
  editForm.skills = [...(resume.value?.skills || [])]
}

async function handleSave() {
  if (!hasChanges.value) return
  
  try {
    await resumeStore.update({
      title: editForm.title,
      content: editForm.content,
      skills: editForm.skills.length > 0 ? editForm.skills : undefined,
    })
    isEditing.value = false
    alert('이력서가 저장되었습니다.')
  } catch (e) {
    alert('저장에 실패했습니다.')
  }
}

async function handleAnalyze() {
  if (!resume.value?.content) {
    alert('이력서 내용을 먼저 작성해주세요.')
    return
  }
  
  try {
    await resumeStore.analyze()
    alert('AI 분석이 요청되었습니다. 완료되면 자동으로 업데이트됩니다.')
    
    // 분석 완료까지 폴링 (최대 2분)
    const maxAttempts = 24  // 5초 * 24 = 120초
    let attempts = 0
    
    const pollAnalysis = setInterval(async () => {
      attempts++
      await resumeStore.fetchResume()
      
      if (resume.value?.status === 'ANALYZED' || attempts >= maxAttempts) {
        clearInterval(pollAnalysis)
        if (resume.value?.status === 'ANALYZED') {
          alert('AI 분석이 완료되었습니다!')
        }
      }
    }, 5000)  // 5초마다 확인
    
  } catch (e) {
    alert('AI 분석 요청에 실패했습니다.')
  }
}

function handlePreview(ver: VersionResponse) {
  selectedVersion.value = ver
  showPreviewModal.value = true
}

async function handleRestore(version: number) {
  if (!confirm(`버전 ${version}으로 복원하시겠습니까?\n현재 내용은 새 버전으로 저장됩니다.`)) return
  
  try {
    await resumeStore.restore(version)
    isEditing.value = false
    alert('복원되었습니다.')
  } catch (e) {
    alert('복원에 실패했습니다.')
  }
}

async function fetchData() {
  try {
    await resumeStore.fetchResume()
    await resumeStore.fetchVersions()
  } catch (e) {
    console.error('데이터 로딩 실패:', e)
  }
}

onMounted(fetchData)
</script>