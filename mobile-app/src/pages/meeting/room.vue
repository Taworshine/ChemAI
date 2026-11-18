<template>
  <view class="room">
    <!-- 顶部：会议信息 + 主持人控制 -->
    <view class="header">
      <view class="row between">
        <text class="title">{{ meeting.title || '加载中…' }}</text>
        <view class="live-pill" :class="{ off: meeting.status !== 1 }">
          <view class="live-dot"></view>{{ statusName }}
        </view>
      </view>
      <view class="row header-sub">
        <text class="no-badge">№ {{ meeting.meetingNo }}</text>
        <text class="sep">·</text>
        <text>{{ meeting.subject }}教研组</text>
        <view class="flex1"></view>
        <text v-if="isHost && meeting.status !== 2" class="lock-btn" @click="toggleLock">
          <uni-icons :type="meeting.locked ? 'close' : 'check'" size="22" :color="meeting.locked ? '#fa5151' : '#00b578'" />
          {{ meeting.locked ? ' 已锁定' : ' 锁定' }}
        </text>
      </view>
      <view v-if="isHost && meeting.status !== 2" class="host-bar">
        <text v-if="meeting.status === 0" class="host-op" @click="control('start')">▶ 开始会议</text>
        <text class="host-op end" @click="control('close')">■ 结束会议</text>
      </view>
    </view>

    <!-- Tab 切换 -->
    <view class="tabbar">
      <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: tab === t.key }" @click="switchTab(t.key)">
        <uni-icons :type="t.icon" size="32" :color="tab === t.key ? '#00b578' : '#86909c'" />
        <text>{{ t.name }}</text>
        <text v-if="t.key === 'anno' && annotations.length" class="tab-badge">{{ annotations.length }}</text>
        <view v-if="t.key === 'chat' && false" class="tab-badge"></view>
      </view>
    </view>

    <!-- ========== 教案页签 ========== -->
    <scroll-view v-show="tab === 'doc'" class="panel" scroll-y>
      <view class="card doc-card">
        <view class="row between">
          <view class="row">
            <view class="doc-icon"><uni-icons type="doc" size="44" color="#4A6FA5" /></view>
            <view>
              <text class="doc-name">{{ docMeta.title || '尚未选择教案' }}</text>
              <text class="doc-status" :class="'p' + (docMeta.parseStatus || 0)">
                {{ ['未解析', '解析中…', '已解析', '解析失败'][docMeta.parseStatus || 0] }}
              </text>
            </view>
          </view>
        </view>
        <view class="doc-actions">
          <view class="doc-action" @click="uploadDoc">
            <view class="da-icon" style="background:#e2f7ee"><uni-icons type="camera" size="40" color="#00B578" /></view>
            <text>上传教案</text>
          </view>
          <view v-if="isHost && docMeta.id" class="doc-action" @click="parseDoc">
            <view class="da-icon" style="background:#fdf3e7"><uni-icons type="gear" size="40" color="#ED7B2F" /></view>
            <text>AI 解析</text>
          </view>
          <view v-if="docMeta.parseStatus === 2" class="doc-action" @click="goReview">
            <view class="da-icon" style="background:#e9f0fd"><uni-icons type="chart" size="40" color="#4A6FA5" /></view>
            <text>量化评审</text>
          </view>
        </view>
      </view>

      <!-- 图片预览 + 批注画布 -->
      <view v-if="docImagePath" class="canvas-wrap">
        <image :src="docImagePath" class="doc-img" mode="widthFix" @load="onImgLoad" />
        <canvas
          id="drawCanvas"
          type="2d"
          class="draw-canvas"
          :style="{ width: canvasW + 'px', height: canvasH + 'px' }"
          @touchstart="drawStart"
          @touchmove="drawMove"
          @touchend="drawEnd"
        ></canvas>
      </view>

      <view v-if="docImagePath" class="draw-tools card">
        <view class="colors">
          <view v-for="c in colors" :key="c" class="color-dot" :style="{ background: c }" :class="{ active: penColor === c }" @click="penColor = c"></view>
        </view>
        <view class="btn-plain draw-toggle" :class="{ drawing }" @click="toggleDraw">
          {{ drawing ? '✔ 提交这段圈画' : '✏️ 开始手绘批注' }}
        </view>
      </view>

      <view class="card">
        <text class="section-label" style="margin-top:0">快速文字批注</text>
        <textarea class="ta" v-model="annoText" placeholder="针对当前教案发表意见…" placeholder-class="ph" />
        <view class="btn-primary small-btn" @click="addTextAnno">提交批注</view>
      </view>
    </scroll-view>

    <!-- ========== 批注页签 ========== -->
    <scroll-view v-show="tab === 'anno'" class="panel" scroll-y>
      <view v-if="annotations.length === 0" class="empty-state">
        <view class="empty-icon">✏️</view>
        <text style="color:#666666">还没有批注</text>
        <text class="muted">去「教案」页签手绘圈画或添加文字批注</text>
      </view>
      <view v-for="a in annotations" :key="a.annotation.id" class="card anno-item" :class="'t-' + a.annotation.type.toLowerCase()">
        <view class="row between">
          <view class="row">
            <view class="avatar anno-avatar">{{ (a.realName || '?').charAt(0) }}</view>
            <view>
              <text class="anno-author">{{ a.realName }}</text>
              <text class="anno-type">{{ typeName(a.annotation.type) }} · 第 {{ a.annotation.page }} 页</text>
            </view>
          </view>
          <view v-if="a.annotation.isResolved" class="resolved-pill">✓ 已解决</view>
        </view>
        <view v-if="a.annotation.content" class="anno-content">{{ a.annotation.content }}</view>
        <view v-if="a.annotation.type === 'DRAW'" class="anno-special"><uni-icons type="brush" size="26" color="#4A6FA5" /> 手绘圈画（在教案页签的画布上可见）</view>
        <view v-if="a.annotation.type === 'VOICE'" class="anno-special"><uni-icons type="chat" size="26" color="#ED7B2F" /> 语音批注 {{ a.annotation.voiceSeconds }}″</view>
        <view class="row anno-ops">
          <text class="op" @click="reply(a)">回复</text>
          <text class="op" @click="toggleResolve(a)">{{ a.annotation.isResolved ? '重新打开' : '标记解决' }}</text>
          <text v-if="a.annotation.userId === myId" class="op danger" @click="removeAnno(a)">删除</text>
        </view>
        <view v-for="r in repliesOf(a.annotation.id)" :key="r.annotation.id" class="reply-line">
          <view class="avatar reply-avatar">{{ (r.realName || '?').charAt(0) }}</view>
          <view class="reply-body">
            <text class="reply-author">{{ r.realName }}</text>
            {{ r.annotation.content }}
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- ========== 讨论页签 ========== -->
    <view v-show="tab === 'chat'" class="chat-panel">
      <scroll-view class="chat-list" scroll-y :scroll-into-view="lastMsgId">
        <view v-for="m in messages" :key="m.message.id" :id="'msg-' + m.message.id" class="msg-row" :class="{ self: m.message.userId === myId }">
          <view v-if="m.message.userId !== myId" class="avatar chat-avatar">{{ (m.realName || '?').charAt(0) }}</view>
          <view class="msg-main">
            <text class="msg-author">{{ m.realName }} · {{ timeOf(m.message.createTime) }}</text>
            <view class="bubble">{{ m.message.content }}</view>
          </view>
          <view v-if="m.message.userId === myId" class="avatar chat-avatar self-avatar">{{ (m.realName || '?').charAt(0) }}</view>
        </view>
      </scroll-view>
      <view class="chat-input">
        <input class="ci" v-model="chatText" placeholder="说点什么…" placeholder-class="ph" confirm-type="send" @confirm="sendChat" />
        <view class="send-btn" @click="sendChat">发送</view>
      </view>
    </view>

    <!-- ========== 投票页签 ========== -->
    <scroll-view v-show="tab === 'vote'" class="panel" scroll-y>
      <view v-if="isHost && meeting.status !== 2" class="btn-plain create-vote" @click="showVoteForm = !showVoteForm">{{ showVoteForm ? '收起' : '＋ 发起投票' }}</view>

      <view v-if="showVoteForm" class="card">
        <input class="input" v-model="voteForm.title" placeholder="投票标题，如：本教案整体是否通过评审？" placeholder-class="ph" />
        <view class="vote-types">
          <view v-for="vt in voteTypes" :key="vt.key" class="vt" :class="{ active: voteForm.voteType === vt.key }" @click="voteForm.voteType = vt.key">{{ vt.name }}</view>
        </view>
        <textarea class="ta" v-model="voteForm.optionsText" placeholder="选项，每行一个" placeholder-class="ph" />
        <view class="row between anon">
          <text>匿名投票</text>
          <switch color="#00B578" :checked="voteForm.anonymous" @change="voteForm.anonymous = $event.detail.value" />
        </view>
        <view class="btn-primary small-btn" @click="createVote">创建投票</view>
      </view>

      <view v-for="v in votes" :key="v.id" class="card vote-item">
        <view class="row between">
          <text class="vote-title">{{ v.title }}</text>
          <view class="vote-status" :class="{ closed: v.status === 1 }">{{ v.status === 1 ? '已结束' : '进行中' }}</view>
        </view>
        <view class="muted vote-meta">{{ voteTypeName(v.voteType) }} · {{ v.anonymous ? '匿名' : '实名' }} · {{ v.status === 1 ? '' : '点击选项投票' }}</view>

        <view v-if="v.status === 0 && !myVotedMap[v.id]">
          <view v-for="opt in parseOptions(v.options)" :key="opt.id" class="vote-opt" :class="{ picked: picked[v.id + opt.id] }" @click="pick(v, opt.id)">
            <view class="opt-radio" :class="{ on: picked[v.id + opt.id] }"></view>
            <text class="opt-text">{{ opt.text }}</text>
            <text v-if="voteForm.voteType === 'SCORE'" class="stars">{{ stars(picked[v.id + opt.id]) }}</text>
          </view>
          <view class="btn-primary small-btn" @click="submitVote(v)">提交投票</view>
        </view>

        <view v-else class="result">
          <view v-for="r in (voteResults[v.id] ? voteResults[v.id].options : [])" :key="r.optionId" class="result-row">
            <text class="r-text">{{ r.text }}</text>
            <view class="bar-wrap">
              <view class="bar" :style="{ width: barWidth(v.id, r) + '%' }"></view>
            </view>
            <text class="r-count">{{ r.count }}票<text v-if="r.avgScore"> {{ r.avgScore.toFixed(1) }}分</text></text>
          </view>
          <view class="muted voters-line">共 {{ voteResults[v.id] ? voteResults[v.id].totalVoters : 0 }} 人参与{{ myVotedMap[v.id] ? ' · 你已投' : '' }}</view>
        </view>
      </view>
    </scroll-view>

    <!-- ========== 成员页签 ========== -->
    <scroll-view v-show="tab === 'member'" class="panel" scroll-y>
      <view v-for="m in members" :key="m.userId" class="card row between member-item">
        <view class="row">
          <view class="avatar" :class="{ gray: !m.online }">{{ (m.realName || '?').charAt(0) }}</view>
          <view>
            <view class="row">
              <text class="member-name">{{ m.realName }}</text>
              <text class="role-pill" :class="{ host: m.role === 'HOST' }">{{ m.role === 'HOST' ? '主持人' : '成员' }}</text>
            </view>
            <view class="row" style="gap:8rpx;margin-top:4rpx">
              <view class="online-dot" :class="{ on: m.online }"></view>
              <text class="muted online-text">{{ m.online ? '在线' : '离线' }}</text>
            </view>
          </view>
        </view>
        <text v-if="isHost && m.role !== 'HOST' && meeting.status !== 2" class="kick" @click="kick(m)">移出</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request, getBaseUrl } from '@/utils/request.js'

export default {
  data() {
    return {
      meetingId: null,
      meeting: {},
      members: [],
      isHost: false,
      myId: null,
      tab: 'doc',
      tabs: [
        { key: 'doc', name: '教案', icon: 'doc' },
        { key: 'anno', name: '圈画', icon: 'edit' },
        { key: 'chat', name: '讨论', icon: 'chat' },
        { key: 'vote', name: '投票', icon: 'vote' },
        { key: 'member', name: '成员', icon: 'people' }
      ],
      docMeta: {},
      docImagePath: '',
      canvasW: 0,
      canvasH: 0,
      drawing: false,
      penColor: '#fa5151',
      colors: ['#fa5151', '#10aeff', '#00b578', '#ff7d00', '#722ed1'],
      currentStroke: [],
      strokes: [],
      ctx: null,
      canvasNode: null,
      annotations: [],
      annoText: '',
      messages: [],
      chatText: '',
      lastMsgId: '',
      lastChatId: null,
      votes: [],
      voteResults: {},
      myVotedMap: {},
      picked: {},
      showVoteForm: false,
      voteForm: { title: '', voteType: 'SINGLE', optionsText: '', anonymous: true, scoreMode: false },
      voteTypes: [
        { key: 'SINGLE', name: '单选' },
        { key: 'MULTIPLE', name: '多选' },
        { key: 'SCORE', name: '评分' }
      ],
      timer: null
    }
  },
  computed: {
    statusName() {
      return ['待开始', '进行中', '已结束', '已取消'][this.meeting.status] || ''
    }
  },
  onLoad(options) {
    this.meetingId = options.id
    this.myId = uni.getStorageSync('userInfo').userId
  },
  onShow() {
    this.refreshAll()
    this.timer = setInterval(() => this.refreshAll(), 3000)
  },
  onHide() { clearInterval(this.timer) },
  onUnload() { clearInterval(this.timer) },
  methods: {
    timeOf(t) {
      return t ? t.replace('T', ' ').substring(11, 16) : ''
    },
    async refreshAll() {
      try {
        const status = await request({ url: '/api/mobile/meeting/' + this.meetingId })
        this.meeting = status.meeting
        this.members = status.members
        this.isHost = status.isHost

        const annos = await request({ url: '/api/mobile/annotation/list?meetingId=' + this.meetingId })
        this.annotations = annos

        const url = '/api/mobile/chat/list?meetingId=' + this.meetingId +
          (this.lastChatId ? '&afterId=' + this.lastChatId : '')
        const msgs = await request({ url })
        if (msgs.length) {
          this.messages = this.messages.concat(msgs)
          this.lastChatId = msgs[msgs.length - 1].message.id
          this.lastMsgId = 'msg-' + msgs[msgs.length - 1].message.id
        }

        const votes = await request({ url: '/api/mobile/vote/list?meetingId=' + this.meetingId })
        this.votes = votes
        for (const v of votes) {
          const r = await request({ url: '/api/mobile/vote/' + v.id + '/result' })
          this.voteResults = { ...this.voteResults, [v.id]: r }
          this.myVotedMap = { ...this.myVotedMap, [v.id]: r.myVoted }
        }

        if (this.meeting.currentDocId && this.meeting.currentDocId !== (this.docMeta.id || 0)) {
          this.loadDoc(this.meeting.currentDocId)
        }
      } catch (e) { /* 网络抖动忽略，下一轮再试 */ }
    },
    switchTab(key) {
      this.tab = key
      if (key === 'doc' && this.docImagePath) this.$nextTick(() => this.replayStrokes())
    },

    // ---------- 教案 ----------
    async loadDoc(docId) {
      try {
        const meta = await request({ url: '/api/mobile/document/' + docId + '/meta' })
        this.docMeta = meta
        if (meta.isImage) {
          const res = await uni.downloadFile({
            url: getBaseUrl() + '/api/mobile/document/' + docId + '/file',
            header: { satoken: uni.getStorageSync('satoken') }
          })
          this.docImagePath = res.tempFilePath
          this.$nextTick(() => this.replayStrokes())
        } else {
          this.docImagePath = ''
        }
      } catch (e) { /* 已提示 */ }
    },
    onImgLoad(e) {
      uni.createSelectorQuery().in(this).select('.doc-img').boundingClientRect(rect => {
        this.canvasW = rect.width
        this.canvasH = rect.height
        this.initCanvas()
      }).exec()
    },
    uploadDoc() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          const file = res.tempFilePaths[0]
          uni.showLoading({ title: '上传中' })
          try {
            const up = await uni.uploadFile({
              url: getBaseUrl() + '/api/document/upload',
              filePath: file,
              name: 'file',
              header: { satoken: uni.getStorageSync('satoken') },
              formData: { docType: 'COURSEWARE' }
            })
            const doc = JSON.parse(up.data).data
            uni.hideLoading()
            if (this.isHost) {
              await request({
                url: '/api/mobile/meeting/' + this.meetingId + '/control',
                method: 'POST',
                data: { action: 'switchDoc', docId: doc.id }
              })
            }
            uni.showToast({ title: '上传成功', icon: 'success' })
            this.refreshAll()
          } catch (e) { uni.hideLoading() }
        }
      })
    },
    async parseDoc() {
      if (!this.docMeta.id) return
      uni.showLoading({ title: 'AI 解析中，请稍候' })
      try {
        await request({ url: '/api/document/parse/' + this.docMeta.id, method: 'POST' })
        uni.hideLoading()
        uni.showToast({ title: '解析完成', icon: 'success' })
        this.refreshAll()
      } catch (e) { uni.hideLoading() }
    },
    goReview() {
      uni.navigateTo({ url: '/pages/review/analyze?docId=' + this.docMeta.id })
    },

    // ---------- 画布批注 ----------
    initCanvas() {
      uni.createSelectorQuery().in(this).select('#drawCanvas')
        .fields({ node: true, size: true }).exec(res => {
          if (!res || !res[0]) return
          this.canvasNode = res[0].node
          this.ctx = this.canvasNode.getContext('2d')
          const dpr = uni.getSystemInfoSync().pixelRatio || 1
          this.canvasNode.width = this.canvasW * dpr
          this.canvasNode.height = this.canvasH * dpr
          this.ctx.scale(dpr, dpr)
          this.replayStrokes()
        })
    },
    toggleDraw() {
      if (!this.drawing) {
        this.drawing = true
        this.strokes = []
        uni.showToast({ title: '开始在教案上圈画', icon: 'none' })
      } else {
        this.drawing = false
        this.submitDraw()
      }
    },
    drawStart(e) {
      if (!this.drawing) return
      const p = this.relativePoint(e)
      this.currentStroke = [p]
      this.ctx.beginPath()
      this.ctx.moveTo(p.px, p.py)
      this.ctx.lineWidth = 4
      this.ctx.strokeStyle = this.penColor
      this.ctx.lineCap = 'round'
      this.ctx.lineJoin = 'round'
    },
    drawMove(e) {
      if (!this.drawing || !this.currentStroke.length) return
      const p = this.relativePoint(e)
      this.currentStroke.push(p)
      this.ctx.lineTo(p.px, p.py)
      this.ctx.stroke()
    },
    drawEnd() {
      if (!this.drawing || !this.currentStroke.length) return
      this.strokes.push({ color: this.penColor, points: this.currentStroke.map(p => ({ x: p.rx, y: p.ry })) })
      this.currentStroke = []
    },
    relativePoint(e) {
      const t = e.touches[0] || e.changedTouches[0]
      const x = t.clientX - e.currentTarget.offsetLeft
      const y = t.clientY - e.currentTarget.offsetTop
      return { px: x, py: y, rx: +(x / this.canvasW).toFixed(4), ry: +(y / this.canvasH).toFixed(4) }
    },
    replayStrokes() {
      if (!this.ctx) return
      this.ctx.clearRect(0, 0, this.canvasW, this.canvasH)
      for (const item of this.annotations) {
        const a = item.annotation
        if (a.type !== 'DRAW' || !a.drawData) continue
        let groups
        try { groups = JSON.parse(a.drawData) } catch (e) { continue }
        this.ctx.strokeStyle = a.color || '#fa5151'
        this.ctx.lineWidth = a.strokeWidth || 4
        this.ctx.lineCap = 'round'
        for (const g of groups) {
          if (!g.length) continue
          this.ctx.beginPath()
          this.ctx.moveTo(g[0].x * this.canvasW, g[0].y * this.canvasH)
          for (const p of g) this.ctx.lineTo(p.x * this.canvasW, p.y * this.canvasH)
          this.ctx.stroke()
        }
      }
    },
    async submitDraw() {
      if (!this.strokes.length || !this.meeting.currentDocId) return
      const groups = this.strokes.map(s => s.points)
      await request({
        url: '/api/mobile/annotation/add',
        method: 'POST',
        data: {
          meetingId: this.meetingId,
          docId: this.meeting.currentDocId,
          type: 'DRAW',
          color: this.penColor,
          strokeWidth: 4,
          drawData: JSON.stringify(groups),
          page: 1
        }
      })
      uni.showToast({ title: '手绘批注已同步给所有人', icon: 'success' })
      this.refreshAll()
    },
    async addTextAnno() {
      if (!this.annoText.trim()) return
      await request({
        url: '/api/mobile/annotation/add',
        method: 'POST',
        data: {
          meetingId: this.meetingId,
          docId: this.meeting.currentDocId,
          type: 'TEXT',
          content: this.annoText.trim(),
          page: 1
        }
      })
      this.annoText = ''
      uni.showToast({ title: '批注已提交', icon: 'success' })
      this.refreshAll()
    },
    reply(item) {
      uni.showModal({
        title: '回复 ' + item.realName,
        editable: true,
        placeholderText: '输入回复内容',
        success: async (res) => {
          if (res.confirm && res.content) {
            await request({
              url: '/api/mobile/annotation/' + item.annotation.id + '/reply',
              method: 'POST',
              data: { content: res.content }
            })
            this.refreshAll()
          }
        }
      })
    },
    async toggleResolve(item) {
      await request({
        url: '/api/mobile/annotation/' + item.annotation.id + '/resolve',
        method: 'POST',
        data: { resolved: item.annotation.isResolved ? 0 : 1 }
      })
      this.refreshAll()
    },
    removeAnno(item) {
      uni.showModal({
        title: '删除该批注？',
        success: async (res) => {
          if (res.confirm) {
            await request({ url: '/api/mobile/annotation/' + item.annotation.id, method: 'DELETE' })
            this.refreshAll()
          }
        }
      })
    },
    repliesOf(id) {
      return this.annotations.filter(a => a.annotation.replyTo === id)
    },

    // ---------- 聊天 ----------
    async sendChat() {
      if (!this.chatText.trim()) return
      await request({
        url: '/api/mobile/chat/send',
        method: 'POST',
        data: { meetingId: this.meetingId, content: this.chatText.trim() }
      })
      this.chatText = ''
      this.refreshAll()
    },

    // ---------- 投票 ----------
    parseOptions(json) {
      try { return JSON.parse(json) } catch (e) { return [] }
    },
    voteTypeName(t) { return { SINGLE: '单选', MULTIPLE: '多选', SCORE: '评分' }[t] || t },
    pick(v, optionId) {
      const key = v.id + optionId
      if (v.voteType === 'SINGLE') {
        const next = { ...this.picked }
        for (const o of this.parseOptions(v.options)) delete next[v.id + o.id]
        next[key] = true
        this.picked = next
      } else if (v.voteType === 'MULTIPLE') {
        const cur = this.picked[key]
        const next = { ...this.picked }
        cur ? delete next[key] : next[key] = true
        this.picked = next
      } else {
        const cur = (this.picked[key] || 0) % 5 + 1
        this.picked = { ...this.picked, [key]: cur }
      }
    },
    stars(n) { return '★'.repeat(n || 0) },
    async submitVote(v) {
      const choices = Object.keys(this.picked)
        .filter(k => k.startsWith(v.id))
        .map(k => ({ optionId: k.slice(String(v.id).length), score: this.picked[k] === true ? null : this.picked[k] }))
        .filter(c => c.score === null || c.score > 0)
      if (!choices.length) {
        uni.showToast({ title: '请先选择', icon: 'none' })
        return
      }
      await request({ url: '/api/mobile/vote/submit', method: 'POST', data: { voteId: v.id, choices } })
      uni.showToast({ title: '投票成功', icon: 'success' })
      this.refreshAll()
    },
    async createVote() {
      const options = this.voteForm.optionsText.split('\n').map(s => s.trim()).filter(Boolean)
      if (!this.voteForm.title.trim() || options.length < 2) {
        uni.showToast({ title: '需要标题和至少 2 个选项', icon: 'none' })
        return
      }
      await request({
        url: '/api/mobile/vote/create',
        method: 'POST',
        data: {
          meetingId: this.meetingId,
          title: this.voteForm.title.trim(),
          voteType: this.voteForm.voteType,
          options: options.map(t => ({ text: t })),
          anonymous: this.voteForm.anonymous ? 1 : 0
        }
      })
      this.showVoteForm = false
      this.voteForm = { title: '', voteType: 'SINGLE', optionsText: '', anonymous: true, scoreMode: false }
      this.refreshAll()
    },
    barWidth(voteId, r) {
      const result = this.voteResults[voteId]
      if (!result) return 0
      const max = Math.max(...result.options.map(o => o.count), 1)
      return Math.round(r.count * 100 / max)
    },

    // ---------- 成员 / 控制 ----------
    async control(action) {
      await request({
        url: '/api/mobile/meeting/' + this.meetingId + '/control',
        method: 'POST',
        data: { action }
      })
      if (action === 'close') {
        uni.showToast({ title: '会议已结束' })
      }
      this.refreshAll()
    },
    async toggleLock() {
      await this.control(this.meeting.locked ? 'unlock' : 'lock')
    },
    kick(m) {
      uni.showModal({
        title: '将 ' + m.realName + ' 移出会议？',
        success: async (res) => {
          if (res.confirm) {
            await request({
              url: '/api/mobile/meeting/' + this.meetingId + '/kick',
              method: 'POST',
              data: { userId: m.userId }
            })
            this.refreshAll()
          }
        }
      })
    },
    typeName(t) { return { DRAW: '手绘', TEXT: '文字', VOICE: '语音' }[t] || t }
  }
}
</script>

<style scoped>
.room { display: flex; flex-direction: column; height: 100vh; }
.flex1 { flex: 1; }

/* 头部：微信绿扁平 */
.header {
  background: #0f5c46;
  color: #fff; padding: 28rpx 30rpx 24rpx;
}
.title { font-size: 34rpx; font-weight: 600; flex: 1; margin-right: 16rpx; line-height: 1.35; }
.live-pill {
  display: flex; align-items: center; gap: 10rpx;
  background: rgba(255, 255, 255, 0.22); border-radius: 999rpx;
  padding: 8rpx 22rpx; font-size: 22rpx; font-weight: 500; white-space: nowrap;
}
.live-pill.off { background: rgba(0, 0, 0, 0.18); }
.live-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #d8ffde; }
.live-pill.off .live-dot { background: #e4e4e4; }
.header-sub { margin-top: 14rpx; font-size: 24rpx; opacity: 0.92; }
.no-badge { font-family: 'Courier New', monospace; font-weight: 600; letter-spacing: 2rpx; }
.sep { margin: 0 14rpx; }
.lock-btn { background: rgba(255, 255, 255, 0.2); padding: 6rpx 20rpx; border-radius: 999rpx; }
.host-bar { display: flex; gap: 20rpx; margin-top: 20rpx; }
.host-op { background: rgba(255, 255, 255, 0.22); padding: 12rpx 30rpx; border-radius: 999rpx; font-size: 24rpx; font-weight: 500; }
.host-op.end { background: rgba(250, 81, 81, 0.9); }

/* 页签栏 */
.tabbar { display: flex; background: #fff; padding: 6rpx 10rpx; border-bottom: 2rpx solid #ececec; }
.tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6rpx;
  font-size: 26rpx; color: #999999; padding: 18rpx 0; position: relative;
}
.tab-emoji { font-size: 28rpx; }
.tab.active { color: #00b578; font-weight: 600; }
.tab.active::after {
  content: ''; position: absolute; bottom: 4rpx; left: 50%; transform: translateX(-50%);
  width: 48rpx; height: 6rpx; border-radius: 3rpx; background: #00b578;
}
.tab-badge {
  position: absolute; top: 2rpx; right: 18rpx;
  background: #fa5151; color: #fff; font-size: 18rpx; font-weight: 600;
  min-width: 30rpx; height: 30rpx; border-radius: 15rpx; padding: 0 8rpx;
  display: flex; align-items: center; justify-content: center;
}
.panel { flex: 1; padding-bottom: 30rpx; }

/* 教案页签 */
.doc-card { padding: 30rpx; }
.doc-icon {
  width: 84rpx; height: 84rpx; border-radius: 16rpx; background: #e2f7ee;
  display: flex; align-items: center; justify-content: center; font-size: 42rpx; margin-right: 22rpx;
}
.doc-name { display: block; font-size: 30rpx; font-weight: 600; max-width: 460rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-status { font-size: 22rpx; }
.doc-status.p0 { color: #999999; }
.doc-status.p1 { color: #ff7d00; }
.doc-status.p2 { color: #00b578; }
.doc-status.p3 { color: #fa5151; }
.doc-actions { display: flex; gap: 16rpx; margin-top: 26rpx; padding-top: 24rpx; border-top: 2rpx solid #f2f2f2; }
.doc-action { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 10rpx; font-size: 24rpx; color: #666666; }
.da-icon {
  width: 88rpx; height: 88rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center; font-size: 40rpx;
}
.canvas-wrap { position: relative; margin: 20rpx 24rpx; border-radius: 12rpx; overflow: hidden; }
.doc-img { width: 100%; display: block; }
.draw-canvas { position: absolute; left: 0; top: 0; }
.draw-tools { display: flex; justify-content: space-between; align-items: center; padding: 22rpx 28rpx; margin-top: 0; }
.colors { display: flex; gap: 16rpx; }
.color-dot { width: 56rpx; height: 56rpx; border-radius: 50%; border: 5rpx solid transparent; box-sizing: border-box; }
.color-dot.active { border-color: #191919; transform: scale(1.1); }
.draw-toggle { padding: 14rpx 32rpx; font-size: 26rpx; }
.draw-toggle.drawing { background: #00b578; color: #fff; }
.ta { background: #f7f7f7; border-radius: 12rpx; padding: 22rpx 28rpx; min-height: 150rpx; width: 100%; box-sizing: border-box; font-size: 28rpx; margin-top: 14rpx; }
.small-btn { margin-top: 20rpx; padding: 18rpx 0; font-size: 28rpx; }
.input { background: #f7f7f7; border-radius: 12rpx; padding: 20rpx 28rpx; font-size: 30rpx; margin-bottom: 18rpx; }

/* 批注页签 */
.anno-item { border-left: 10rpx solid #e0e0e0; }
.anno-item.t-text { border-left-color: #4a6fa5; }
.anno-item.t-draw { border-left-color: #ff7d00; }
.anno-item.t-voice { border-left-color: #722ed1; }
.anno-avatar { width: 64rpx; height: 64rpx; font-size: 26rpx; margin-right: 18rpx; }
.anno-author { display: block; font-size: 28rpx; font-weight: 600; }
.anno-type { font-size: 22rpx; color: #999999; }
.resolved-pill { background: #e2f7ee; color: #00b578; font-size: 22rpx; font-weight: 500; padding: 8rpx 20rpx; border-radius: 10rpx; }
.anno-content { margin: 20rpx 0 6rpx; font-size: 28rpx; line-height: 1.6; color: #191919; }
.anno-special { margin: 20rpx 0 6rpx; font-size: 26rpx; color: #ed7b2f; }
.anno-ops { gap: 40rpx; margin-top: 16rpx; }
.op { color: #4a6fa5; font-size: 26rpx; }
.op.danger { color: #fa5151; }
.reply-line { display: flex; margin-top: 18rpx; background: #f7f7f7; border-radius: 12rpx; padding: 16rpx 20rpx; }
.reply-avatar { width: 48rpx; height: 48rpx; font-size: 22rpx; margin-right: 14rpx; border-radius: 10rpx; }
.reply-body { font-size: 26rpx; line-height: 1.5; flex: 1; }
.reply-author { color: #4a6fa5; font-weight: 600; margin-right: 10rpx; }

/* 讨论页签（微信聊天气泡） */
.chat-panel { flex: 1; display: flex; flex-direction: column; }
.chat-list { flex: 1; padding: 24rpx; box-sizing: border-box; }
.msg-row { display: flex; margin-bottom: 30rpx; }
.chat-avatar { width: 72rpx; height: 72rpx; font-size: 28rpx; margin-right: 18rpx; border-radius: 12rpx; }
.self-avatar { margin-right: 0; margin-left: 18rpx; background: #00b578; }
.msg-main { max-width: 76%; }
.msg-author { font-size: 20rpx; color: #b3b3b3; margin-left: 8rpx; }
.bubble {
  display: inline-block; background: #fff; border-radius: 8rpx;
  padding: 20rpx 28rpx; margin-top: 8rpx; font-size: 28rpx; line-height: 1.55;
}
.msg-row.self { flex-direction: row; justify-content: flex-end; }
.msg-row.self .msg-main { text-align: right; }
.msg-row.self .msg-author { margin-left: 0; margin-right: 8rpx; }
.msg-row.self .bubble { background: #b4e9cb; color: #191919; border-radius: 8rpx; text-align: left; }
.chat-input { display: flex; background: #f7f7f7; padding: 18rpx 24rpx; gap: 18rpx; align-items: center; border-top: 2rpx solid #e5e5e5; }
.ci { flex: 1; background: #fff; border-radius: 12rpx; padding: 16rpx 24rpx; font-size: 28rpx; }
.send-btn {
  background: #00b578; color: #fff;
  padding: 14rpx 36rpx; border-radius: 12rpx; font-size: 28rpx; font-weight: 500;
}
.send-btn:active { background: #009b68; }

/* 投票页签 */
.create-vote { margin: 24rpx; }
.vote-types { display: flex; gap: 18rpx; margin-bottom: 18rpx; }
.vt { flex: 1; text-align: center; padding: 14rpx 0; border-radius: 12rpx; background: #f2f2f2; font-size: 26rpx; color: #666666; }
.vt.active { background: #e2f7ee; color: #00b578; font-weight: 600; }
.anon { margin: 10rpx 0 6rpx; font-size: 28rpx; }
.vote-item .vote-title { font-size: 30rpx; font-weight: 600; flex: 1; margin-right: 16rpx; }
.vote-status { font-size: 22rpx; font-weight: 500; padding: 6rpx 18rpx; border-radius: 10rpx; background: #e2f7ee; color: #00b578; white-space: nowrap; }
.vote-status.closed { background: #f2f2f2; color: #999999; }
.vote-meta { margin: 10rpx 0 4rpx; }
.vote-opt {
  display: flex; align-items: center; background: #f7f7f7;
  border-radius: 12rpx; padding: 22rpx 26rpx; margin-top: 16rpx;
  border: 2rpx solid transparent;
}
.vote-opt.picked { background: #e2f7ee; border-color: #00b578; }
.opt-radio { width: 32rpx; height: 32rpx; border-radius: 50%; border: 4rpx solid #c8c9cc; margin-right: 18rpx; flex-shrink: 0; box-sizing: border-box; }
.opt-radio.on { border-color: #00b578; background: radial-gradient(circle, #00b578 0 45%, transparent 50%); }
.opt-text { flex: 1; font-size: 28rpx; }
.stars { color: #ff7d00; font-size: 26rpx; }
.result-row { display: flex; align-items: center; gap: 16rpx; margin-top: 20rpx; }
.r-text { width: 180rpx; font-size: 26rpx; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-wrap { flex: 1; background: #f0f0f0; border-radius: 8rpx; height: 26rpx; overflow: hidden; }
.bar { background: #00b578; height: 26rpx; border-radius: 8rpx; transition: width 0.4s; }
.r-count { font-size: 24rpx; color: #666666; width: 150rpx; text-align: right; flex-shrink: 0; }
.voters-line { margin-top: 18rpx; }

/* 成员页签 */
.member-item { margin: 16rpx 24rpx; padding: 26rpx 30rpx; }
.member-name { font-size: 30rpx; font-weight: 600; }
.role-pill { font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 8rpx; background: #f2f2f2; color: #999999; margin-left: 14rpx; }
.role-pill.host { background: #fdf3e7; color: #ed7b2f; }
.online-text { font-size: 22rpx; }
.online-dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: #c9cdd4; }
.online-dot.on { background: #00b578; box-shadow: 0 0 0 4rpx rgba(0, 181, 120, 0.15); }
.kick { color: #fa5151; font-size: 26rpx; padding: 10rpx 24rpx; background: #fdeceb; border-radius: 10rpx; }
</style>
