/* eslint-env node */
module.exports = {
  root: true,
  env: { browser: true, es2022: true, node: true },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'prettier'
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  ignorePatterns: [
    'dist/**',
    'node_modules/**',
    '*.config.js',
    // TS 文件先关掉，等 Sprint 3.5 配 @typescript-eslint/parser
    'src/api/*.ts',
    'src/api/types.ts',
    'src/components/__tests__/*.ts',
    'src/env.d.ts'
  ],
  rules: {
    'vue/multi-word-component-names': 'off',
    // 旧业务页不强制清理（v-if 顺序、组件名等风格问题留给后续 Sprint）
    'vue/attributes-order': 'warn',
    'vue/html-self-closing': 'off',
    'vue/max-attributes-per-line': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/html-closing-bracket-newline': 'off',
    'no-unused-vars': 'off', // 留给 type-check
    'no-empty': 'warn',
    'no-undef': 'off',
    'no-useless-escape': 'warn'
  }
}
