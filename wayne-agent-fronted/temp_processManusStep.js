// 处理Manus的步骤信息
const processManusStep = (data) => {
    console.log('Processing Manus step:', data)

    // 检查是否是步骤信息
    if (data.startsWith('Step ')) {
        // 提取步骤内容
        const stepMatch = data.match(/^Step \d+: (.+)$/)
        if (stepMatch) {
            const stepContent = stepMatch[1]

            // 如果步骤内容包含工具调用结果，提取有用信息
            if (stepContent.includes('工具') && stepContent.includes('返回的结果')) {
                // 提取工具结果
                const toolResultMatch = stepContent.match(/工具 (.+?) 返回的结果：(.+)/)
                if (toolResultMatch) {
                    const toolName = toolResultMatch[1]
                    const result = toolResultMatch[2]
                    return ` ${toolName}: ${result}`
                }
            }

            // 如果是思考过程，简化显示
            if (stepContent.includes('的思考：')) {
                const thoughtMatch = stepContent.match(/(.+?)的思考：(.+)/)
                if (thoughtMatch) {
                    return ` ${thoughtMatch[2]}`
                }
            }

            // 其他步骤信息，添加图标
            return ` ${stepContent}`
        }
    }

    // 检查是否是执行结束信息
    if (data.includes('执行结束') || data.includes('Terminated')) {
        return ' タスクが完了しました'
    }

    // 检查是否是错误信息
    if (data.includes('执行错误') || data.includes('error')) {
        return ` ${data}`
    }

    // 默认返回原数据
    return data
}
