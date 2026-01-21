<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  email: '',
  nickname: '',
  password: '',
  passwordConfirm: '',
  role: 'ROLE_USER' as UserRole,
})
const error = ref('')
const loading = ref(false)

async function handleSubmit(): Promise<void> {
  // 유효성 검사
  if (!form.value.email || !form.value.nickname || !form.value.password) {
    error.value = '모든 필드를 입력해주세요.'
    return
  }

  if (form.value.password !== form.value.passwordConfirm) {
    error.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  if (form.value.password.length < 8) {
    error.value = '비밀번호는 8자 이상이어야 합니다.'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await authStore.signup({
      email: form.value.email,
      nickname: form.value.nickname,
      password: form.value.password,
      role: form.value.role,
    })
    
    // 회원가입 성공 후 로그인 페이지로
    router.push('/auth/login?registered=true')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
    <div class="max-w-md w-full space-y-8">
      <!-- 로고 -->
      <div class="text-center">
        <router-link to="/home" class="inline-flex items-center space-x-2">
          <div class="w-12 h-12 bg-primary-600 rounded-xl flex items-center justify-center">
            <span class="text-white font-bold text-2xl">C</span>
          </div>
        </router-link>
        <h2 class="mt-6 text-3xl font-bold text-gray-900">회원가입</h2>
        <p class="mt-2 text-sm text-gray-600">
          이미 계정이 있으신가요?
          <router-link to="/auth/login" class="text-primary-600 hover:text-primary-500">
            로그인
          </router-link>
        </p>
      </div>

      <!-- 폼 -->
      <form @submit.prevent="handleSubmit" class="mt-8 space-y-6">
        <!-- 에러 메시지 -->
        <div v-if="error" class="bg-red-50 text-red-600 p-4 rounded-lg text-sm">
          {{ error }}
        </div>

        <div class="space-y-4">
          <!-- 이메일 -->
          <div>
            <label for="email" class="label">이메일</label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              required
              class="input"
              placeholder="example@email.com"
            />
          </div>

          <!-- 닉네임 -->
          <div>
            <label for="nickname" class="label">닉네임</label>
            <input
              id="nickname"
              v-model="form.nickname"
              type="text"
              required
              class="input"
              placeholder="닉네임을 입력하세요"
            />
          </div>

          <!-- 비밀번호 -->
          <div>
            <label for="password" class="label">비밀번호</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              class="input"
              placeholder="8자 이상 입력하세요"
            />
          </div>

          <!-- 비밀번호 확인 -->
          <div>
            <label for="passwordConfirm" class="label">비밀번호 확인</label>
            <input
              id="passwordConfirm"
              v-model="form.passwordConfirm"
              type="password"
              required
              class="input"
              placeholder="비밀번호를 다시 입력하세요"
            />
          </div>

          <!-- 회원 유형 -->
          <div>
            <label class="label">회원 유형</label>
            <div class="grid grid-cols-2 gap-4">
              <label
                :class="[
                  'flex items-center justify-center p-4 border rounded-lg cursor-pointer transition-colors',
                  form.role === 'ROLE_USER' 
                    ? 'border-primary-500 bg-primary-50 text-primary-700' 
                    : 'border-gray-300 hover:bg-gray-50'
                ]"
              >
                <input
                  v-model="form.role"
                  type="radio"
                  value="ROLE_USER"
                  class="sr-only"
                />
                <div class="text-center">
                  <svg class="w-6 h-6 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  <span class="text-sm font-medium">구직자</span>
                </div>
              </label>
              <label
                :class="[
                  'flex items-center justify-center p-4 border rounded-lg cursor-pointer transition-colors',
                  form.role === 'ROLE_COMPANY' 
                    ? 'border-primary-500 bg-primary-50 text-primary-700' 
                    : 'border-gray-300 hover:bg-gray-50'
                ]"
              >
                <input
                  v-model="form.role"
                  type="radio"
                  value="ROLE_COMPANY"
                  class="sr-only"
                />
                <div class="text-center">
                  <svg class="w-6 h-6 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                  <span class="text-sm font-medium">기업</span>
                </div>
              </label>
            </div>
          </div>
        </div>

        <!-- 가입 버튼 -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full btn btn-primary py-3"
        >
          <span v-if="loading">가입 중...</span>
          <span v-else>회원가입</span>
        </button>
      </form>
    </div>
  </div>
</template>
