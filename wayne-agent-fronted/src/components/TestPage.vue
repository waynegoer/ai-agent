<template>
  <div style="padding: 50px; text-align: center; background: #f0f0f0; min-height: 100vh;">
    <h1 style="color: #333; font-size: 2rem;">Test Page</h1>
    <p style="color: #666; font-size: 1.2rem;">This is a simple test page to verify Vue is working.</p>
    <button @click="showMessage"
            style="padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer; margin-top: 20px;">
      Click Me
    </button>
    <p v-if="message" style="color: green; font-weight: bold; margin-top: 20px;">{{ message }}</p>
  </div>
</template>

<script>
import {ref} from 'vue'

export default {
  name: 'TestPage',
  setup() {
    const message = ref('')

    const showMessage = () => {
      message.value = 'Vue is working correctly!'
    }

    return {
      message,
      showMessage
    }
  }
}
</script>



