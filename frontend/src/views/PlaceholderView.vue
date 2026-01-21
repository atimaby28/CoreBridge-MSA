<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 경로에 따른 페이지 정보
const pageInfo = computed(() => {
  const path = route.path
  
  const pages: Record<string, { title: string; description: string; icon: string }> = {
    // Admin
    '/admin/stats': {
      title: '사용자 통계',
      description: '사용자 가입 추이, 역할별 분포 등 상세 통계를 확인할 수 있습니다.',
      icon: 'chart',
    },
    '/admin/audits': {
      title: '감사 로그',
      description: '시스템 접근 기록, 주요 활동 로그를 확인할 수 있습니다.',
      icon: 'document',
    },
    '/admin/settings': {
      title: '시스템 설정',
      description: '시스템 환경 설정을 관리할 수 있습니다.',
      icon: 'cog',
    },
    
    // Company
    '/company/jobpostings': {
      title: '채용공고 목록',
      description: '등록한 채용공고를 관리하고 상태를 확인할 수 있습니다.',
      icon: 'briefcase',
    },
    '/company/jobpostings/new': {
      title: '채용공고 등록',
      description: '새로운 채용공고를 작성하고 게시할 수 있습니다.',
      icon: 'plus',
    },
    '/company/applications': {
      title: '지원자 목록',
      description: '채용공고에 지원한 지원자들을 확인하고 관리할 수 있습니다.',
      icon: 'users',
    },
    '/company/schedules': {
      title: '면접 일정',
      description: '면접 일정을 확인하고 관리할 수 있습니다.',
      icon: 'calendar',
    },
    '/company/profile': {
      title: '기업 정보',
      description: '기업 프로필을 수정하고 관리할 수 있습니다.',
      icon: 'building',
    },
    
    // User
    '/jobpostings': {
      title: '채용공고',
      description: '다양한 채용공고를 검색하고 지원할 수 있습니다.',
      icon: 'search',
    },
    '/my/applications': {
      title: '지원 현황',
      description: '내가 지원한 채용공고의 진행 상태를 확인할 수 있습니다.',
      icon: 'document',
    },
    '/my/schedules': {
      title: '면접 일정',
      description: '예정된 면접 일정을 확인할 수 있습니다.',
      icon: 'calendar',
    },
    '/my/resume': {
      title: '이력서 관리',
      description: '이력서를 작성하고 관리할 수 있습니다.',
      icon: 'document-text',
    },
    '/profile': {
      title: '내 정보',
      description: '내 프로필을 확인하고 수정할 수 있습니다.',
      icon: 'user',
    },
  }
  
  return pages[path] || {
    title: '준비 중',
    description: '해당 기능은 현재 개발 중입니다.',
    icon: 'clock',
  }
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ pageInfo.title }}</h1>
      <p class="text-gray-500">{{ pageInfo.description }}</p>
    </div>

    <div class="card">
      <div class="py-16 text-center">
        <!-- 아이콘 -->
        <div class="w-20 h-20 mx-auto mb-6 bg-gray-100 rounded-full flex items-center justify-center">
          <svg v-if="pageInfo.icon === 'chart'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'document'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'cog'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'briefcase'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'plus'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'users'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'calendar'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'building'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'search'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'document-text'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <svg v-else-if="pageInfo.icon === 'user'" class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          <svg v-else class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>

        <h2 class="text-xl font-semibold text-gray-700 mb-2">기능 준비 중</h2>
        <p class="text-gray-500 mb-6">
          해당 기능은 현재 개발 중입니다.<br>
          빠른 시일 내에 서비스할 수 있도록 하겠습니다.
        </p>

        <router-link
          to="/"
          class="inline-flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          대시보드로 돌아가기
        </router-link>
      </div>
    </div>
  </div>
</template>
