<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { adminUserService } from '@/api/user'
import { formatDate } from '@/composables/useDate'
import type { AdminUser, UserRole, UserStatus, UserStatsResponse } from '@/types'

// 상태
const users = ref<AdminUser[]>([])
const stats = ref<UserStatsResponse | null>(null)
const loading = ref(true)
const error = ref('')

// 페이징
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalCount = ref(0)

// 필터
const searchKeyword = ref('')
const selectedRole = ref<UserRole | ''>('')

// 모달
const showRoleModal = ref(false)
const showStatusModal = ref(false)
const selectedUser = ref<AdminUser | null>(null)
const newRole = ref<UserRole>('ROLE_USER')
const newStatus = ref<UserStatus>('ACTIVE')

// 역할 라벨
const roleLabels: Record<UserRole, string> = {
  ROLE_USER: '일반회원',
  ROLE_COMPANY: '기업회원',
  ROLE_ADMIN: '관리자',
}

// 상태 라벨
const statusLabels: Record<UserStatus, string> = {
  ACTIVE: '활성',
  BLOCKED: '차단',
  DELETED: '탈퇴',
}

// 통계 로드
async function loadStats(): Promise<void> {
  try {
    stats.value = await adminUserService.getStats()
  } catch (e) {
    console.error('통계 로드 실패:', e)
  }
}

// 사용자 목록 로드
async function loadUsers(): Promise<void> {
  loading.value = true
  error.value = ''

  try {
    let response

    if (searchKeyword.value) {
      response = await adminUserService.searchUsers(searchKeyword.value, page.value, size.value)
    } else if (selectedRole.value) {
      response = await adminUserService.getUsersByRole(selectedRole.value, page.value, size.value)
    } else {
      response = await adminUserService.getAllUsers(page.value, size.value)
    }

    users.value = response.users
    totalPages.value = response.totalPages
    totalCount.value = response.totalCount
  } catch (e) {
    error.value = e instanceof Error ? e.message : '사용자 목록을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// 검색
function handleSearch(): void {
  page.value = 0
  loadUsers()
}

// 역할 필터 변경
function handleRoleFilter(role: UserRole | ''): void {
  selectedRole.value = role
  searchKeyword.value = ''
  page.value = 0
  loadUsers()
}

// 페이지 변경
function changePage(newPage: number): void {
  if (newPage >= 0 && newPage < totalPages.value) {
    page.value = newPage
    loadUsers()
  }
}

// 역할 변경 모달 열기
function openRoleModal(user: AdminUser): void {
  selectedUser.value = user
  newRole.value = user.role
  showRoleModal.value = true
}

// 상태 변경 모달 열기
function openStatusModal(user: AdminUser): void {
  selectedUser.value = user
  newStatus.value = user.status
  showStatusModal.value = true
}

// 역할 변경 저장
async function saveRole(): Promise<void> {
  if (!selectedUser.value) return

  try {
    await adminUserService.updateRole(selectedUser.value.userId, { role: newRole.value })
    showRoleModal.value = false
    loadUsers()
    loadStats()
  } catch (e) {
    alert(e instanceof Error ? e.message : '역할 변경에 실패했습니다.')
  }
}

// 상태 변경 저장
async function saveStatus(): Promise<void> {
  if (!selectedUser.value) return

  try {
    await adminUserService.updateStatus(selectedUser.value.userId, { status: newStatus.value })
    showStatusModal.value = false
    loadUsers()
    loadStats()
  } catch (e) {
    alert(e instanceof Error ? e.message : '상태 변경에 실패했습니다.')
  }
}

// 역할 배지 클래스
function getRoleBadgeClass(role: UserRole): string {
  switch (role) {
    case 'ROLE_ADMIN': return 'badge-purple'
    case 'ROLE_COMPANY': return 'badge-blue'
    default: return 'badge-gray'
  }
}

// 상태 배지 클래스
function getStatusBadgeClass(status: UserStatus): string {
  switch (status) {
    case 'ACTIVE': return 'badge-green'
    case 'BLOCKED': return 'badge-red'
    default: return 'badge-gray'
  }
}

onMounted(() => {
  loadStats()
  loadUsers()
})
</script>

<template>
  <div class="space-y-6">
    <!-- 헤더 -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900">사용자 관리</h1>
      <p class="text-gray-500">전체 사용자를 관리하고 권한을 설정하세요.</p>
    </div>

    <!-- 통계 카드 -->
    <div v-if="stats" class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div class="card text-center">
        <p class="text-2xl font-bold text-gray-900">{{ stats.totalUsers }}</p>
        <p class="text-sm text-gray-500">전체 사용자</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-green-600">{{ stats.activeUsers }}</p>
        <p class="text-sm text-gray-500">활성 사용자</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-blue-600">{{ stats.companyCount }}</p>
        <p class="text-sm text-gray-500">기업 회원</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-red-600">{{ stats.blockedUsers }}</p>
        <p class="text-sm text-gray-500">차단 사용자</p>
      </div>
    </div>

    <!-- 필터 및 검색 -->
    <div class="card">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between space-y-4 md:space-y-0">
        <!-- 역할 필터 -->
        <div class="flex space-x-2">
          <button 
            @click="handleRoleFilter('')"
            :class="['btn btn-sm', selectedRole === '' ? 'btn-primary' : 'btn-outline']"
          >
            전체
          </button>
          <button 
            @click="handleRoleFilter('ROLE_USER')"
            :class="['btn btn-sm', selectedRole === 'ROLE_USER' ? 'btn-primary' : 'btn-outline']"
          >
            일반회원
          </button>
          <button 
            @click="handleRoleFilter('ROLE_COMPANY')"
            :class="['btn btn-sm', selectedRole === 'ROLE_COMPANY' ? 'btn-primary' : 'btn-outline']"
          >
            기업회원
          </button>
          <button 
            @click="handleRoleFilter('ROLE_ADMIN')"
            :class="['btn btn-sm', selectedRole === 'ROLE_ADMIN' ? 'btn-primary' : 'btn-outline']"
          >
            관리자
          </button>
        </div>

        <!-- 검색 -->
        <div class="flex space-x-2">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="이메일 또는 닉네임 검색"
            class="input w-64"
            @keyup.enter="handleSearch"
          />
          <button @click="handleSearch" class="btn btn-primary">검색</button>
        </div>
      </div>
    </div>

    <!-- 사용자 목록 -->
    <div class="card">
      <div v-if="loading" class="py-12 text-center text-gray-500">
        로딩 중...
      </div>
      <div v-else-if="error" class="py-12 text-center text-red-500">
        {{ error }}
      </div>
      <div v-else-if="users.length === 0" class="py-12 text-center text-gray-500">
        사용자가 없습니다.
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="text-left text-sm text-gray-500 border-b">
              <th class="pb-3 font-medium">사용자</th>
              <th class="pb-3 font-medium">역할</th>
              <th class="pb-3 font-medium">상태</th>
              <th class="pb-3 font-medium">가입일</th>
              <th class="pb-3 font-medium">마지막 로그인</th>
              <th class="pb-3 font-medium">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr 
              v-for="user in users" 
              :key="user.userId"
              class="border-b border-gray-100 last:border-0"
            >
              <td class="py-4">
                <div class="flex items-center space-x-3">
                  <div class="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                    <span class="text-primary-700 font-medium">
                      {{ user.nickname?.charAt(0) || 'U' }}
                    </span>
                  </div>
                  <div>
                    <p class="font-medium text-gray-900">{{ user.nickname }}</p>
                    <p class="text-sm text-gray-500">{{ user.email }}</p>
                  </div>
                </div>
              </td>
              <td class="py-4">
                <span :class="['badge', getRoleBadgeClass(user.role)]">
                  {{ roleLabels[user.role] }}
                </span>
              </td>
              <td class="py-4">
                <span :class="['badge', getStatusBadgeClass(user.status)]">
                  {{ statusLabels[user.status] }}
                </span>
              </td>
              <td class="py-4 text-sm text-gray-500">
                {{ formatDate(user.createdAt, 'date') }}
              </td>
              <td class="py-4 text-sm text-gray-500">
                {{ user.lastLoginAt ? formatDate(user.lastLoginAt, 'relative') : '-' }}
              </td>
              <td class="py-4">
                <div class="flex space-x-2">
                  <button 
                    @click="openRoleModal(user)"
                    class="text-sm text-primary-600 hover:text-primary-700"
                  >
                    역할 변경
                  </button>
                  <button 
                    @click="openStatusModal(user)"
                    class="text-sm text-gray-600 hover:text-gray-700"
                  >
                    상태 변경
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이징 -->
      <div v-if="totalPages > 1" class="flex justify-center items-center space-x-2 mt-6">
        <button 
          @click="changePage(page - 1)"
          :disabled="page === 0"
          class="btn btn-outline btn-sm"
        >
          이전
        </button>
        <span class="text-sm text-gray-600">
          {{ page + 1 }} / {{ totalPages }}
        </span>
        <button 
          @click="changePage(page + 1)"
          :disabled="page >= totalPages - 1"
          class="btn btn-outline btn-sm"
        >
          다음
        </button>
      </div>
    </div>

    <!-- 역할 변경 모달 -->
    <div v-if="showRoleModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">역할 변경</h3>
        <p class="text-sm text-gray-500 mb-4">
          {{ selectedUser?.nickname }}님의 역할을 변경합니다.
        </p>
        <select v-model="newRole" class="input mb-4">
          <option value="ROLE_USER">일반회원</option>
          <option value="ROLE_COMPANY">기업회원</option>
          <option value="ROLE_ADMIN">관리자</option>
        </select>
        <div class="flex justify-end space-x-2">
          <button @click="showRoleModal = false" class="btn btn-outline">취소</button>
          <button @click="saveRole" class="btn btn-primary">변경</button>
        </div>
      </div>
    </div>

    <!-- 상태 변경 모달 -->
    <div v-if="showStatusModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">상태 변경</h3>
        <p class="text-sm text-gray-500 mb-4">
          {{ selectedUser?.nickname }}님의 상태를 변경합니다.
        </p>
        <select v-model="newStatus" class="input mb-4">
          <option value="ACTIVE">활성</option>
          <option value="BLOCKED">차단</option>
        </select>
        <div class="flex justify-end space-x-2">
          <button @click="showStatusModal = false" class="btn btn-outline">취소</button>
          <button @click="saveStatus" class="btn btn-primary">변경</button>
        </div>
      </div>
    </div>
  </div>
</template>
