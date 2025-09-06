<template>
  <div class="home-page">
    <div class="container">
      <h1 class="title">AI アプリケーション</h1>
      <p class="subtitle">お好みのAIアプリケーションを選択してください</p>
      <p style="color: white; text-align: center; margin: 20px 0;">HomePage component loaded successfully!</p>

      <div class="app-grid">
        <div class="app-card" @click="navigateToLoveApp">
          <div class="app-icon">💕</div>
          <h2 class="app-title">AI 恋愛マスター</h2>
          <p class="app-description">恋愛に関する悩みや質問に答えるAIアシスタント</p>
          <div class="app-button">開始する</div>
        </div>

        <div class="app-card" @click="navigateToManusApp">
          <div class="app-icon">🤖</div>
          <h2 class="app-title">AI スーパーエージェント</h2>
          <p class="app-description">様々なタスクをサポートする高機能AIアシスタント</p>
          <div class="app-button">開始する</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomePage',
  methods: {
    navigateToLoveApp() {
      this.$router.push('/love-app')
    },
    navigateToManusApp() {
      this.$router.push('/manus-app')
    }
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.container {
  max-width: 800px;
  width: 100%;
  text-align: center;
}

.title {
  font-size: 3rem;
  color: white;
  margin-bottom: 1rem;
  font-weight: 300;
}

.subtitle {
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 3rem;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  margin-top: 2rem;
}

.app-card {
  background: white;
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.app-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.app-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.app-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.app-title {
  font-size: 1.5rem;
  color: #333;
  margin-bottom: 1rem;
  font-weight: 600;
}

.app-description {
  color: #666;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.app-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 24px;
  border-radius: 25px;
  font-weight: 600;
  display: inline-block;
  transition: all 0.3s ease;
}

.app-card:hover .app-button {
  transform: scale(1.05);
}

@media (max-width: 768px) {
  .title {
    font-size: 2rem;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
