<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userService } from '@/api/user'
import type { User, UserUpdateRequest } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

// 상태
const user = ref<User | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const successMessage = ref('')

// 수정 모드
const isEditing = ref(false)
const editForm = ref<UserUpdateRequest>({
  nickname: '',
})

// 탈퇴 모달
const showDeleteModal = ref(false)
const deleteConfirmText = ref('')

// 내 정보 조회
async function loadProfile(): Promise<void> {
  loading.value = true
  error.value = ''
  
  try {
    user.value = await userService.getMe()
    editForm.value.nickname = user.value.nickname
  } catch (e) {
    error.value = e instanceof Error ? e.message : '프로필을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// 수정 모드 시작
function startEdit(): void {
  if (user.value) {
    editForm.value.nickname = user.value.nickname
  }
  isEditing.value = true
}

// 수정 취소
function cancelEdit(): void {
  isEditing.value = false
  if (user.value) {
    editForm.value.nickname = user.value.nickname
  }
}

// 내 정보 수정
async function saveProfile(): Promise<void> {
  if (!editForm.value.nickname?.trim()) {
    error.value = '닉네임을 입력해주세요.'
    return
  }
  
  saving.value = true
  error.value = ''
  successMessage.value = ''
  
  try {
    user.value = await userService.updateMe(editForm.value)
    isEditing.value = false
    successMessage.value = '프로필이 수정되었습니다.'
    
    // authStore 업데이트
    await authStore.fetchUser()
    
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '프로필 수정에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

// 회원 탈퇴
async function deleteAccount(): Promise<void> {
  if (deleteConfirmText.value !== '회원탈퇴') {
    error.value = '"회원탈퇴"를 정확히 입력해주세요.'
    return
  }
  
  saving.value = true
  error.value = ''
  
  try {
    await userService.deleteMe()
    authStore.clearAuth()
    router.push('/home')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '회원 탈퇴에 실패했습니다.'
    saving.value = false
  }
}

// 역할 라벨
function getRoleLabel(role: string): string {
  const labels: Record<string, string> = {
    ROLE_USER: '일반회원',
    ROLE_COMPANY: '기업회원',
    ROLE_ADMIN: '관리자',
  }
  return labels[role] || role
}

// 상태 라벨
function getStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    ACTIVE: '활성',
    BLOCKED: '차단',
    DELETED: '탈퇴',
  }
  return labels[status] || status
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="space-y-6">
    <!-- 헤더 -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900">내 정보</h1>
      <p class="text-gray-500">계정 정보를 확인하고 수정할 수 있습니다.</p>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="card py-12 text-center text-gray-500">
      로딩 중...
    </div>

    <!-- 에러 -->
    <div v-else-if="error && !user" class="card py-12 text-center text-red-500">
      {{ error }}
    </div>

    <!-- 프로필 카드 -->
    <template v-else-if="user">
      <!-- 알림 메시지 -->
      <div v-if="successMessage" class="bg-green-50 text-green-700 p-4 rounded-lg">
        {{ successMessage }}
      </div>
      <div v-if="error" class="bg-red-50 text-red-600 p-4 rounded-lg">
        {{ error }}
      </div>

      <div class="card">
        <!-- 프로필 헤더 -->
        <div class="flex items-center space-x-6 pb-6 border-b border-gray-200">
          <div class="w-20 h-20 bg-primary-100 rounded-full flex items-center justify-center">
            <span class="text-primary-700 font-bold text-3xl">
              {{ user.nickname?.charAt(0) || 'U' }}
            </span>
          </div>
          <div>
            <h2 class="text-xl font-semibold text-gray-900">{{ user.nickname }}</h2>
            <p class="text-gray-500">{{ user.email }}</p>
            <div class="flex items-center space-x-2 mt-2">
              <span class="badge badge-blue">{{ getRoleLabel(user.role) }}</span>
              <span v-if="!authStore.isUser" class="badge badge-green">{{ getStatusLabel(user.status) }}</span>
            </div>
          </div>
        </div>

        <!-- 정보 섹션 -->
        <div class="py-6 space-y-6">
          <!-- 닉네임 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">닉네임</label>
            <div v-if="isEditing">
              <input
                v-model="editForm.nickname"
                type="text"
                class="input w-full max-w-md"
                placeholder="닉네임을 입력하세요"
              />
            </div>
            <p v-else class="text-gray-900">{{ user.nickname }}</p>
          </div>

          <!-- 이메일 (수정 불가) -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">이메일</label>
            <p class="text-gray-900">{{ user.email }}</p>
            <p v-if="isEditing" class="text-sm text-gray-500 mt-1">이메일은 변경할 수 없습니다.</p>
          </div>

          <!-- 가입일 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">가입일</label>
            <p class="text-gray-900">
              {{ new Date(user.createdAt).toLocaleDateString('ko-KR', { 
                year: 'numeric', month: 'long', day: 'numeric' 
              }) }}
            </p>
          </div>
        </div>

        <!-- 버튼 -->
        <div class="flex justify-between items-center pt-6 border-t border-gray-200">
          <div>
            <button
              v-if="!isEditing"
              @click="startEdit"
              class="btn btn-primary"
            >
              정보 수정
            </button>
            <div v-else class="flex space-x-2">
              <button
                @click="cancelEdit"
                class="btn btn-outline"
                :disabled="saving"
              >
                취소
              </button>
              <button
                @click="saveProfile"
                class="btn btn-primary"
                :disabled="saving"
              >
                {{ saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </div>
          
          <button
            v-if="!isEditing"
            @click="showDeleteModal = true"
            class="text-red-600 hover:text-red-700 text-sm"
          >
            회원 탈퇴
          </button>
        </div>
      </div>

      <!-- 추가 정보 카드 -->
      <div class="card">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">계정 정보</h3>
        <div class="space-y-4">
          <div class="flex justify-between py-3 border-b border-gray-100">
            <span class="text-gray-500">회원 유형</span>
            <span class="text-gray-900">{{ getRoleLabel(user.role) }}</span>
          </div>
          <div v-if="!authStore.isUser" class="flex justify-between py-3 border-b border-gray-100">
            <span class="text-gray-500">계정 상태</span>
            <span class="text-gray-900">{{ getStatusLabel(user.status) }}</span>
          </div>
          <div class="flex justify-between py-3">
            <span class="text-gray-500">User ID</span>
            <span class="text-gray-500 font-mono text-sm">{{ user.userId }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 회원 탈퇴 모달 -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl p-6 w-full max-w-md mx-4">
        <h3 class="text-lg font-semibold text-gray-900 mb-2">회원 탈퇴</h3>
        <p class="text-gray-600 mb-4">
          정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없으며, 모든 데이터가 삭제됩니다.
        </p>
        
        <div class="bg-red-50 p-4 rounded-lg mb-4">
          <p class="text-sm text-red-700">
            탈퇴를 확인하려면 아래에 <strong>"회원탈퇴"</strong>를 입력하세요.
          </p>
        </div>
        
        <input
          v-model="deleteConfirmText"
          type="text"
          class="input w-full mb-4"
          placeholder="회원탈퇴"
        />
        
        <div class="flex justify-end space-x-2">
          <button 
            @click="showDeleteModal = false; deleteConfirmText = ''"
            class="btn btn-outline"
          >
            취소
          </button>
          <button 
            @click="deleteAccount"
            class="btn bg-red-600 text-white hover:bg-red-700"
            :disabled="saving || deleteConfirmText !== '회원탈퇴'"
          >
            {{ saving ? '처리 중...' : '탈퇴하기' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
