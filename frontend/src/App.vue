<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/common/AppHeader.vue'
import AppSidebar from '@/components/common/AppSidebar.vue'

const route = useRoute()
const authStore = useAuthStore()

// 인증 페이지 여부 (레이아웃 분기)
const isAuthPage = computed(() => route.path.startsWith('/auth'))
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 인증 페이지 (헤더/사이드바 없음) -->
    <template v-if="isAuthPage">
      <router-view />
    </template>
    
    <!-- 메인 레이아웃 -->
    <template v-else>
      <AppHeader />
      <div class="flex">
        <AppSidebar />
        <main class="flex-1 p-6 ml-64 mt-16">
          <router-view />
        </main>
      </div>
    </template>
  </div>
</template>
