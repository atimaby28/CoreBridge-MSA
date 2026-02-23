<script setup lang="ts">
// 비로그인 사용자를 위한 홈 화면
</script>

<template>
  <div class="space-y-8">
    <!-- 히어로 섹션 -->
    <div class="card bg-gradient-to-r from-primary-600 to-primary-700 text-white">
      <div class="py-8 px-4 text-center">
        <h1 class="text-4xl font-bold mb-4">CoreBridge</h1>
        <p class="text-xl text-primary-100 mb-8">
          채용 관리의 모든 것을 한 곳에서
        </p>
        <div class="flex justify-center space-x-4">
          <router-link to="/auth/signup" class="btn bg-white text-primary-600 hover:bg-gray-100 px-8 py-3">
            시작하기
          </router-link>
          <router-link to="/auth/login" class="btn border border-white text-white hover:bg-primary-500 px-8 py-3">
            로그인
          </router-link>
        </div>
      </div>
    </div>

    <!-- 기능 소개 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="card text-center">
        <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">채용공고 관리</h3>
        <p class="text-gray-500">채용공고 등록부터 마감까지 한 번에 관리하세요</p>
      </div>

      <div class="card text-center">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">지원자 추적</h3>
        <p class="text-gray-500">지원부터 최종 합격까지 모든 단계를 추적하세요</p>
      </div>

      <div class="card text-center">
        <div class="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">면접 일정</h3>
        <p class="text-gray-500">면접 일정을 쉽게 관리하고 알림을 받으세요</p>
      </div>
    </div>

    <!-- CTA -->
    <div class="card text-center py-12">
      <h2 class="text-2xl font-bold text-gray-900 mb-4">지금 바로 시작하세요</h2>
      <p class="text-gray-500 mb-6">무료로 가입하고 채용 관리를 시작해보세요</p>
      <router-link to="/auth/signup" class="btn btn-primary px-8 py-3">
        무료로 시작하기
      </router-link>
    </div>
  </div>
</template>
