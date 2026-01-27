<template>
  <div class="skill-tag-input">
    <label v-if="label" class="block text-sm font-medium text-gray-700 mb-2">
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>
    
    <!-- 태그 목록 + 입력 -->
    <div 
      class="flex flex-wrap gap-2 p-3 border border-gray-300 rounded-lg bg-white min-h-[48px] focus-within:ring-2 focus-within:ring-indigo-500 focus-within:border-transparent"
      @click="focusInput"
    >
      <!-- 태그들 -->
      <span
        v-for="(skill, index) in modelValue"
        :key="index"
        :class="[
          'inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium transition-colors',
          tagColorClass
        ]"
      >
        {{ skill }}
        <button
          type="button"
          @click.stop="removeSkill(index)"
          class="hover:bg-black/10 rounded-full p-0.5 transition-colors"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </span>
      
      <!-- 입력 필드 -->
      <input
        ref="inputRef"
        v-model="inputValue"
        type="text"
        :placeholder="modelValue.length === 0 ? placeholder : ''"
        class="flex-1 min-w-[120px] outline-none text-sm bg-transparent"
        @keydown.enter.prevent="addSkill"
        @keydown.tab.prevent="addSkill"
        @keydown.comma.prevent="addSkill"
        @keydown.backspace="handleBackspace"
      />
    </div>
    
    <!-- 도움말 -->
    <p class="mt-1 text-xs text-gray-500">
      {{ helpText || 'Enter, Tab, 또는 쉼표로 태그를 추가하세요' }}
    </p>
    
    <!-- 추천 스킬 (있을 경우) -->
    <div v-if="suggestions.length > 0" class="mt-2">
      <span class="text-xs text-gray-500 mr-2">추천:</span>
      <button
        v-for="suggestion in filteredSuggestions"
        :key="suggestion"
        type="button"
        @click="addSuggestion(suggestion)"
        class="inline-block mr-1 mb-1 px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded hover:bg-gray-200 transition-colors"
      >
        + {{ suggestion }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  modelValue: string[]
  label?: string
  placeholder?: string
  helpText?: string
  required?: boolean
  suggestions?: string[]
  color?: 'blue' | 'purple' | 'green' | 'orange'
  maxTags?: number
}

const props = withDefaults(defineProps<Props>(), {
  label: '',
  placeholder: '스킬을 입력하세요',
  helpText: '',
  required: false,
  suggestions: () => [],
  color: 'blue',
  maxTags: 20
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const inputValue = ref('')

// 태그 색상 클래스
const tagColorClass = computed(() => {
  const colors = {
    blue: 'bg-blue-100 text-blue-700',
    purple: 'bg-purple-100 text-purple-700',
    green: 'bg-green-100 text-green-700',
    orange: 'bg-orange-100 text-orange-700'
  }
  return colors[props.color]
})

// 아직 추가되지 않은 추천 스킬만 필터링
const filteredSuggestions = computed(() => {
  const lowerValues = props.modelValue.map(v => v.toLowerCase())
  return props.suggestions
    .filter(s => !lowerValues.includes(s.toLowerCase()))
    .slice(0, 8)
})

function focusInput() {
  inputRef.value?.focus()
}

function addSkill() {
  const value = inputValue.value.trim()
  if (!value) return
  
  // 중복 체크
  const lowerValues = props.modelValue.map(v => v.toLowerCase())
  if (lowerValues.includes(value.toLowerCase())) {
    inputValue.value = ''
    return
  }
  
  // 최대 개수 체크
  if (props.modelValue.length >= props.maxTags) {
    return
  }
  
  emit('update:modelValue', [...props.modelValue, value])
  inputValue.value = ''
}

function addSuggestion(skill: string) {
  const lowerValues = props.modelValue.map(v => v.toLowerCase())
  if (lowerValues.includes(skill.toLowerCase())) return
  if (props.modelValue.length >= props.maxTags) return
  
  emit('update:modelValue', [...props.modelValue, skill])
}

function removeSkill(index: number) {
  const newValue = [...props.modelValue]
  newValue.splice(index, 1)
  emit('update:modelValue', newValue)
}

function handleBackspace() {
  // 입력값이 비어있을 때 백스페이스 누르면 마지막 태그 삭제
  if (inputValue.value === '' && props.modelValue.length > 0) {
    removeSkill(props.modelValue.length - 1)
  }
}
</script>
