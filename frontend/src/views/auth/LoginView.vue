<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = ref({
  email: '',
  password: '',
})
const error = ref('')
const loading = ref(false)

// 리다이렉트 경로
const redirectPath = ref('/')

onMounted(() => {
  // 쿼리 파라미터에서 리다이렉트 경로 추출
  if (route.query.redirect) {
    redirectPath.value = decodeURIComponent(route.query.redirect as string)
  }
})

async function handleSubmit(): Promise<void> {
  if (!form.value.email || !form.value.password) {
    error.value = '이메일과 비밀번호를 입력해주세요.'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await authStore.login(form.value)
    router.push(redirectPath.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '로그인에 실패했습니다.'
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
        <h2 class="mt-6 text-3xl font-bold text-gray-900">로그인</h2>
        <p class="mt-2 text-sm text-gray-600">
          계정이 없으신가요?
          <router-link to="/auth/signup" class="text-primary-600 hover:text-primary-500">
            회원가입
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

          <!-- 비밀번호 -->
          <div>
            <label for="password" class="label">비밀번호</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              class="input"
              placeholder="••••••••"
            />
          </div>
        </div>

        <!-- 로그인 버튼 -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full btn btn-primary py-3"
        >
          <span v-if="loading">로그인 중...</span>
          <span v-else>로그인</span>
        </button>
      </form>
    </div>
  </div>
</template>
