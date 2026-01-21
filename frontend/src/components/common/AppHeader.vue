<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const showUserMenu = ref(false)

async function handleLogout(): Promise<void> {
  await authStore.logout()
  showUserMenu.value = false
  router.push('/auth/login')
}
</script>

<template>
  <header class="fixed top-0 left-0 right-0 h-16 bg-white border-b border-gray-200 z-50">
    <div class="flex items-center justify-between h-full px-6">
      <!-- 로고 -->
      <router-link 
        :to="authStore.isVisitor ? '/home' : '/'" 
        class="flex items-center space-x-2"
      >
        <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
          <span class="text-white font-bold text-lg">C</span>
        </div>
        <span class="font-bold text-xl text-gray-900">CoreBridge</span>
      </router-link>

      <!-- 우측 메뉴 -->
      <div class="flex items-center space-x-4">
        <!-- 비로그인: 로그인/회원가입 버튼 -->
        <template v-if="authStore.isVisitor">
          <router-link 
            to="/auth/login"
            class="px-4 py-2 text-gray-600 hover:text-gray-900"
          >
            로그인
          </router-link>
          <router-link 
            to="/auth/signup"
            class="btn btn-primary"
          >
            회원가입
          </router-link>
        </template>

        <!-- 로그인: 사용자 메뉴 -->
        <template v-else>
          <div class="relative">
            <button 
              @click="showUserMenu = !showUserMenu"
              class="flex items-center space-x-2 p-2 hover:bg-gray-100 rounded-lg"
            >
              <div class="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                <span class="text-primary-700 font-medium text-sm">
                  {{ authStore.user?.nickname?.charAt(0) || 'U' }}
                </span>
              </div>
              <span class="text-gray-700">{{ authStore.user?.nickname || '사용자' }}</span>
              <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            
            <!-- 드롭다운 메뉴 -->
            <div 
              v-if="showUserMenu"
              class="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-100 py-1"
            >
              <div class="px-4 py-2 border-b">
                <p class="text-sm font-medium text-gray-900">{{ authStore.user?.nickname }}</p>
                <p class="text-xs text-gray-500">{{ authStore.user?.email }}</p>
                <p class="text-xs text-primary-600 mt-1">
                  {{ authStore.isAdmin ? '관리자' : authStore.isCompany ? '기업회원' : '일반회원' }}
                </p>
              </div>
              <button 
                @click="handleLogout"
                class="block w-full text-left px-4 py-2 text-red-600 hover:bg-gray-100"
              >
                로그아웃
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </header>
</template>
