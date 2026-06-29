<template>
  <div class="tab-container scrollbar">
    <NoPermission v-if="!hasPermission" />
    <div v-else class="bus-management">
      <h2>班车管理</h2>
      <p>班车管理功能开发中...</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import NoPermission from "./Common/NoPermission.vue";
import { useResourcesStore } from "../stores/resources";

const resourcesStore = useResourcesStore()

const hasPermission = computed(() => resourcesStore.$state.hasPermission)
</script>

<style scoped>
.bus-management {
  padding: 20px;
}
</style>