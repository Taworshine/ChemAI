<template>
  <text class="uni-icons" :style="{ color, fontSize: size + 'rpx' }">
    <svg :width="px" :height="px" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
      <template v-for="(d, i) in paths" :key="i">
        <circle v-if="d.c" :cx="d.c[0]" :cy="d.c[1]" :r="d.c[2]" :fill="d.f ? 'currentColor' : 'none'" :stroke="d.f ? 'none' : 'currentColor'" />
        <path v-else :d="d" />
      </template>
    </svg>
  </text>
</template>

<script>
const I = {
  plus: ['M12 5v14', 'M5 12h14'],
  scan: ['M3 7V4h3', 'M17 4h3v3', 'M21 17v3h-3', 'M7 20H4v-3', 'M3 12h18'],
  right: ['M9 6l6 6-6 6'],
  down: ['M6 9l6 6 6-6'],
  back: ['M15 6l-6 6 6 6'],
  doc: ['M7 3h7l4 4v14H7z', 'M14 3v4h4'],
  list: ['M8.5 6h12', 'M8.5 12h12', 'M8.5 18h12', { c: [4, 6, 0.8], f: 1 }, { c: [4, 12, 0.8], f: 1 }, { c: [4, 18, 0.8], f: 1 }],
  task: ['M5 4h14v16H5z', 'M9 12.5l2 2 4-4.5'],
  star: ['M12 3.5l2.6 5.4 5.9.7-4.4 4.1 1.2 5.8-5.3-3-5.3 3 1.2-5.8L3.5 9.6l5.9-.7z'],
  chart: ['M5 20v-7', 'M12 20V5', 'M19 20v-11', 'M2.5 20h19'],
  map: [{ c: [12, 4, 1.6], f: 1 }, { c: [6, 13, 1.6], f: 1 }, { c: [18, 13, 1.6], f: 1 }, { c: [6, 20, 1.4], f: 1 }, { c: [18, 20, 1.4], f: 1 }, 'M12 5.5v6', 'M7 14l4-2.5', 'M17 14l-4-2.5', 'M6 14.5V19', 'M18 14.5V19'],
  bell: ['M6 9.5a6 6 0 0112 0c0 4.5 2 5.5 2 5.5H4s2-1 2-5.5', 'M10 19.5a2 2 0 004 0'],
  gear: [{ c: [12, 12, 3] }, 'M12 2.5v2.5', 'M12 19v2.5', 'M2.5 12H5', 'M19 12h2.5', 'M5.2 5.2l1.8 1.8', 'M17 17l1.8 1.8', 'M18.8 5.2L17 7', 'M7 17l-1.8 1.8'],
  info: [{ c: [12, 12, 9] }, 'M12 8h.01', 'M12 11.5V16'],
  brush: ['M9 11.5l8-8a2.1 2.1 0 013 3l-8 8', 'M7 14c-1.7 0-3 1.4-3 3.1 0 1.4-1.5 1.6-2 2.1 1.1 1.1 2.5 2 4 2 2.2 0 4-1.8 4-4.1 0-1.7-1.3-3.1-3-3.1z'],
  chat: ['M21 11.5a8 8 0 01-8 8H5.5L3 22V11.5a8 8 0 018-8h2a8 8 0 018 8z'],
  people: [{ c: [9, 8, 3.5] }, 'M3.5 20c0-3 2.5-5.5 5.5-5.5s5.5 2.5 5.5 5.5', 'M16 4.8a3.5 3.5 0 010 6.4', 'M17.5 14.6c2.2.8 3.5 2.8 3.5 5.4'],
  edit: ['M12 20h9', 'M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z'],
  check: ['M5 12.5l5 5 9-10.5'],
  camera: ['M3 8h3.2l2-3h7.6l2 3H21v11H3z', { c: [12, 13, 3.4] }],
  close: ['M6 6l12 12', 'M18 6L6 18'],
  clock: [{ c: [12, 12, 9] }, 'M12 7v5l3.2 3'],
  book: ['M4 5.5A1.5 1.5 0 015.5 4H12v17H5.5A1.5 1.5 0 014 22.5z', 'M20 5.5A1.5 1.5 0 0018.5 4H12v17h6.5A1.5 1.5 0 0020 22.5z'],
  vote: [{ c: [12, 12, 9] }, 'M8.5 12l2.5 2.5 5-5.5'],
  send: ['M21 3L10 14', 'M21 3l-7 18-3-8-8-3z'],
  image: ['M4 4h16v16H4z', { c: [9, 9.5, 1.4], f: 1 }, 'M4 17l5-4 3.5 3L17 11l3.5 3.5'],
  grid: ['M4 4h7v7H4z', 'M13 4h7v7h-7z', 'M4 13h7v7H4z', 'M13 13h7v7h-7z']
}

export default {
  name: 'UniIcons',
  props: {
    type: { type: String, default: '' },
    size: { type: [Number, String], default: 40 },
    color: { type: String, default: '#191919' },
    strokeWidth: { type: [Number, String], default: 1.8 }
  },
  computed: {
    paths() { return I[this.type] || I.list },
    px() { return Math.round(Number(this.size) / 2) },
    sw() { return this.strokeWidth }
  }
}
</script>

<style scoped>
.uni-icons { display: inline-flex; align-items: center; justify-content: center; line-height: 1; }
</style>
