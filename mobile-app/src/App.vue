<script>
	export default {
	  onLaunch() {
	    console.log('思维赋能·教研会议 APP 启动')
	  }
	}
</script>

<style>
/* ============ v5 设计令牌 ============
主色 #00B578  深墨绿 #0F5C46  背景 #F6F7F9  卡片 #FFFFFF
标题 #191919  正文 #4E5969  辅助 #86909C  描边 #F0F0F0
危险 #FA5151  气泡绿 #B4E9CB  链接蓝 #4A6FA5
===================================== */
page {
  background-color: #f6f7f9;
  font-size: 28rpx;
  color: #191919;
  font-family: -apple-system, 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  line-height: 1.5;
}

/* 卡片：24rpx 圆角 + 1rpx 描边，无阴影 */
.card {
  background: #ffffff;
  border: 1rpx solid #f2f3f5;
  border-radius: 24rpx;
  padding: 32rpx;
  margin: 20rpx 24rpx;
}

/* 按压反馈：可点击元素加此类 */
.press { transition: opacity 0.15s, transform 0.15s; }
.press:active { opacity: 0.75; transform: scale(0.98); }

/* 按钮：胶囊形 */
.btn-primary {
  background: linear-gradient(135deg, #00c582 0%, #00a86e 100%);
  color: #fff;
  border-radius: 999rpx;
  padding: 24rpx 0;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}
.btn-primary:active { opacity: 0.85; transform: scale(0.98); }
.btn-outline {
  background: #ffffff;
  color: #00b578;
  border: 2rpx solid #00b578;
  border-radius: 999rpx;
  padding: 22rpx 0;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
}
.btn-outline:active { background: #e2f7ee; transform: scale(0.98); }
.btn-danger-text {
  color: #fa5151;
  text-align: center;
  font-size: 30rpx;
  padding: 28rpx 0;
}

/* 列表行：96rpx 行高，图标+名称+右值+箭头 */
.cell {
  display: flex;
  align-items: center;
  min-height: 96rpx;
  padding: 0 32rpx;
}
.cell-icon {
  width: 40rpx;
  font-size: 34rpx;
  margin-right: 24rpx;
  color: #00b578;
  flex-shrink: 0;
}
.cell-body { flex: 1; }
.cell-name { font-size: 30rpx; color: #191919; display: block; }
.cell-desc { font-size: 24rpx; color: #86909c; margin-top: 2rpx; display: block; }
.cell-value { font-size: 26rpx; color: #86909c; margin-right: 8rpx; }
.cell-arrow { color: #c4c9d2; font-size: 32rpx; }

/* 分组标题 */
.group-title {
  font-size: 24rpx;
  color: #86909c;
  padding: 28rpx 40rpx 12rpx;
}

/* 大数字 */
.big-num { font-size: 56rpx; font-weight: 500; color: #191919; line-height: 1.1; }
.big-label { font-size: 24rpx; color: #86909c; margin-top: 8rpx; }

/* 表单 */
.input {
  background: #f6f7f9;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  padding: 22rpx 28rpx;
  font-size: 30rpx;
  transition: border-color 0.15s, background 0.15s;
}
.input:focus { border-color: #00b578; background: #fff; }
.section-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #86909c;
  margin: 30rpx 0 18rpx;
}
.ph { color: #b8bec8; }

/* 输入字段组：圆角浅底 + 上方标签 */
.field { margin-bottom: 28rpx; }
.field-label { display: block; font-size: 26rpx; font-weight: 600; color: #4e5969; margin-bottom: 16rpx; }
.field .input { border-radius: 18rpx; background: #f7f8fa; }

/* 通用 */
.muted { color: #86909c; font-size: 24rpx; }
.text-body { color: #4e5969; }
.row { display: flex; align-items: center; }
.between { justify-content: space-between; }
.chip {
  display: inline-flex;
  align-items: center;
  font-size: 24rpx;
  padding: 10rpx 26rpx;
  border-radius: 999rpx;
  margin: 0 14rpx 14rpx 0;
}
.chip-blue { background: rgba(74, 111, 165, 0.1); color: #4a6fa5; }
.chip-orange { background: #fdf3e7; color: #ed7b2f; }
.chip-green { background: #e2f7ee; color: #00b578; }
.chip-red { background: #fdeceb; color: #fa5151; }
.chip-gray { background: #f2f3f5; color: #86909c; }

/* 头像：姓氏字 + 学科色 */
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  flex-shrink: 0;
}
.avatar.round { border-radius: 50%; }
.avatar.gray { background: #c8c9cc !important; }

/* 空态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 0;
  gap: 18rpx;
}
.empty-icon {
  width: 150rpx;
  height: 150rpx;
  border-radius: 50%;
  background: #eceef1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64rpx;
}
.empty-text { color: #4e5969; font-size: 28rpx; }
.empty-sub { color: #86909c; font-size: 24rpx; }

/* 骨架屏 */
.skeleton {
  margin: 20rpx 24rpx;
  border-radius: 24rpx;
  background: #fff;
  border: 1rpx solid #f2f3f5;
  padding: 32rpx;
}
.skeleton-line {
  height: 28rpx;
  border-radius: 8rpx;
  background: #f0f1f3;
  margin-bottom: 20rpx;
  position: relative;
  overflow: hidden;
}
/* 骨架微光扫过 */
.skeleton-line::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.7), transparent);
  animation: shimmer 1.4s infinite;
  transform: translateX(-100%);
}
@keyframes shimmer { to { transform: translateX(100%); } }
</style>
