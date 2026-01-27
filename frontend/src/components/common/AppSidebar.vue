<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

interface MenuItem {
  name: string
  icon: string
  path?: string
  children?: { name: string; path: string }[]
}

const route = useRoute()
const authStore = useAuthStore()

// 역할별 메뉴 구성
const menuItems = computed<MenuItem[]>(() => {
  const items: MenuItem[] = []
  
  // 비로그인 (VISITOR) 메뉴
  if (authStore.isVisitor) {
    items.push(
      { name: '홈', icon: 'home', path: '/home' },
      { name: '로그인', icon: 'login', path: '/auth/login' },
      { name: '회원가입', icon: 'user-plus', path: '/auth/signup' },
    )
    return items
  }

  // 로그인 사용자 - 대시보드
  items.push({ name: '대시보드', icon: 'home', path: '/' })

  // 관리자 메뉴 (ROLE_ADMIN)
  if (authStore.isAdmin) {
    items.push({
      name: '사용자 관리',
      icon: 'users',
      children: [
        { name: '사용자 목록', path: '/admin/users' },
        { name: '사용자 통계', path: '/admin/stats' },
      ],
    })
    items.push({
      name: '시스템',
      icon: 'cog',
      children: [
        { name: '감사 로그', path: '/admin/audits' },
      ],
    })
  }

  // 기업 회원 메뉴 (ROLE_COMPANY)
  if (authStore.isCompany) {
    items.push({
      name: '채용 관리',
      icon: 'briefcase',
      children: [
        { name: '채용공고 목록', path: '/company/jobpostings' },
        { name: '채용공고 등록', path: '/company/jobpostings/new' },
      ],
    })
    items.push({
      name: '지원자 관리',
      icon: 'users',
      children: [
        { name: '지원자 목록', path: '/company/applications' },
        { name: '면접 일정', path: '/company/schedules' },
      ],
    })
    items.push({ name: '🤖 AI 매칭', icon: 'sparkles', path: '/company/ai-matching' })
    items.push({ name: '기업 정보', icon: 'building', path: '/company/profile' })
  }

  // 일반 사용자 메뉴 (ROLE_USER)
  if (authStore.isUser) {
    items.push({ name: '채용공고', icon: 'briefcase', path: '/jobpostings' })
    items.push({
      name: '내 지원',
      icon: 'document',
      children: [
        { name: '지원 현황', path: '/my/applications' },
        { name: '면접 일정', path: '/my/schedules' },
      ],
    })
    items.push({ name: '이력서', icon: 'document-text', path: '/my/resume' })
  }

  // 공통 - 내 정보
  items.push({ name: '내 정보', icon: 'user', path: '/profile' })

  return items
})

function isActive(path: string): boolean {
  if (path === '/' || path === '/home') {
    return route.path === '/' || route.path === '/home'
  }
  return route.path.startsWith(path)
}
</script>

<template>
  <aside class="fixed left-0 top-16 bottom-0 w-64 bg-white border-r border-gray-200 overflow-y-auto">
    <nav class="p-4 space-y-1">
      <template v-for="item in menuItems" :key="item.path || item.name">
        <!-- 단일 메뉴 -->
        <router-link
          v-if="item.path"
          :to="item.path"
          :class="[
            'flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors',
            isActive(item.path) 
              ? 'bg-primary-50 text-primary-700' 
              : 'text-gray-600 hover:bg-gray-100'
          ]"
        >
          <!-- 아이콘들 -->
          <svg v-if="item.icon === 'home'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <svg v-else-if="item.icon === 'login'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
          </svg>
          <svg v-else-if="item.icon === 'user-plus'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
          </svg>
          <svg v-else-if="item.icon === 'users'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <svg v-else-if="item.icon === 'briefcase'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
          <svg v-else-if="item.icon === 'document'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <svg v-else-if="item.icon === 'document-text'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <svg v-else-if="item.icon === 'sparkles'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
          </svg>
          <svg v-else-if="item.icon === 'building'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          <svg v-else-if="item.icon === 'user'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          <span>{{ item.name }}</span>
        </router-link>

        <!-- 서브 메뉴 -->
        <div v-else-if="item.children">
          <div class="flex items-center space-x-3 px-4 py-3 text-gray-400 text-sm font-medium uppercase">
            <svg v-if="item.icon === 'cog'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <svg v-else-if="item.icon === 'users'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <svg v-else-if="item.icon === 'briefcase'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <svg v-else-if="item.icon === 'document'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span>{{ item.name }}</span>
          </div>
          <div class="ml-4 space-y-1">
            <router-link
              v-for="child in item.children"
              :key="child.path"
              :to="child.path"
              :class="[
                'block px-4 py-2 rounded-lg transition-colors',
                isActive(child.path)
                  ? 'bg-primary-50 text-primary-700'
                  : 'text-gray-600 hover:bg-gray-100'
              ]"
            >
              {{ child.name }}
            </router-link>
          </div>
        </div>
      </template>
    </nav>
  </aside>
</template>
