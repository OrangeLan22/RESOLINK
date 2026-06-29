<template>
  <div class="tab-container scrollbar">
    <div class="tab-main flex">
      <NoPermission v-if="!hasPermission" />
      <div v-else>
        <PhysicalResource />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import PhysicalResource from "./ResourceManagement/PhysicalResource.vue";
import NoPermission from "./Common/NoPermission.vue";
import { useResourcesStore } from "../stores/resources";

const resourcesStore = useResourcesStore()

const hasPermission = computed(() => resourcesStore.$state.hasPermission)

onMounted(() => {
  resourcesStore.fetchPhysicalResources()
})
</script>

<style scoped>
* {
  user-select: none;
}
.flex {
  display: flex;
}

.tab-main {

  flex-direction: column;
}
</style>