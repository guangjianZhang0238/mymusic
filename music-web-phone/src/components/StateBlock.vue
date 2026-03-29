<script setup lang="ts">
defineProps<{
  loading?: boolean
  error?: string
  empty?: boolean
  emptyText?: string
}>()
</script>

<template>
  <el-skeleton v-if="loading" :rows="4" animated />
  <el-result v-else-if="error" icon="error" title="加载失败" :sub-title="error" />
  <el-empty v-else-if="empty" :description="emptyText || '暂无数据'" />
  <slot v-else />
</template>
