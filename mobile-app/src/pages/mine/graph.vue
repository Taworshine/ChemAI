<template>
  <view>
    <view class="card">
      <view class="row" style="gap:16rpx">
        <uni-icons type="map" size="44" color="#4a6fa5" />
        <view>
          <text class="g-title">班级知识图谱</text>
          <text class="muted" style="display:block;margin-top:6rpx">知识点 · 题型 · 错因 的关系网络</text>
        </view>
      </view>
    </view>
    <view class="empty-state" style="padding-top:120rpx">
      <view class="empty-icon"><uni-icons type="map" size="72" color="#c4c9d2" /></view>
      <text style="color:#4e5969">图谱可视化即将开放</text>
      <text class="muted">当前可先在 Web 端查看 Neo4j 图谱，移动端将同步支持</text>
    </view>
  </view>
</template>

<script>
export default {}
</script>

<style scoped>
.g-title { font-size: 32rpx; font-weight: 600; }
</style>
