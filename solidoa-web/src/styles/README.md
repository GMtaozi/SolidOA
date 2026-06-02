# SolidOA Design System — 样式使用规范

> 单一真理源：`src/styles/tokens.scss`
> 修改此文件 = 全站视觉变更（无需改任何业务代码）

## 4 条铁律

### 1. 颜色**必须**用 SCSS 变量或 CSS 变量，**禁止硬编码**

```scss
// ✅ 正确
color: $primary;
background: var(--oa-bg-page);
border-color: $border;

// ❌ 错误
color: #60A5FA;
background: #f7f5f2;
```

### 2. SCSS 变量（`$`）只能在 `<style lang="scss">` 内使用；JS / 内联 style 用 CSS 变量（`var(--*)`）

```vue
<!-- ✅ 正确 -->
<div :style="{ color: 'var(--oa-primary)' }">

<!-- ❌ 错误（$ 变量无法在 JS 中使用） -->
<div :style="{ color: '$primary' }">
```

### 3. 复用样式用 `@mixin`，**禁止**在多个页面复制粘贴同款 SCSS 块

```scss
// ✅ 正确
.btn-save { @include btn-primary; }

// ❌ 错误
.btn-save {
  display: inline-flex; ...
  border: none; ...
  // 重复 30 行
}
```

### 4. 组件 `<style scoped>` 内**不再**重复 `$primary` 等变量定义

vite.config.js 已通过 `additionalData` 自动注入 tokens + mixins 到每个 SCSS 文件，无需重复声明。

## 5 个常用示例

### 示例 1：按钮

```vue
<template>
  <button class="btn-save">保存</button>
  <button class="btn-cancel">取消</button>
</template>

<style lang="scss" scoped>
.btn-save { @include btn-primary; }
.btn-cancel { @include btn-ghost; }
</style>
```

### 示例 2：卡片

```vue
<template>
  <div class="stat-card">
    <div class="stat-label">待审批</div>
    <div class="stat-value">12</div>
  </div>
</template>

<style lang="scss" scoped>
.stat-card {
  @include card-base;
  padding: $gap-xl;
  .stat-label { color: $text-2; font-size: $fz-aux; }
  .stat-value { color: $primary; font-size: $fz-display; font-weight: $fw-semibold; }
}
</style>
```

### 示例 3：状态徽章

```vue
<template>
  <span class="badge-pending">待审批</span>
  <span class="badge-approved">已通过</span>
  <span class="badge-rejected">已驳回</span>
</template>

<style lang="scss" scoped>
.badge-pending { @include status-badge($warning, rgba(251, 191, 36, 0.12)); }
.badge-approved { @include status-badge($success, rgba(52, 211, 153, 0.12)); }
.badge-rejected { @include status-badge($danger, rgba(252, 165, 165, 0.12)); }
</style>
```

### 示例 4：表单输入框

```vue
<template>
  <input class="form-input" placeholder="请输入" />
</template>

<style lang="scss" scoped>
.form-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid $border-input;
  border-radius: $radius-sm;
  background: $bg-input;
  color: $text-1;
  @include focus-glow;
}
</style>
```

### 示例 5：响应式（移动端断点 768px）

```vue
<style lang="scss" scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $gap-xl;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}
</style>
```

## 主色变更流程

1. 修改 `src/styles/tokens.scss` 中的 `$primary`（如改为 `#FF6B6B`）
2. 同时修改 `src/styles/element-overrides.scss` 中的 `--el-color-primary` 为相同值
3. 重启 `npm run dev`
4. 全站主色立即生效

**不要**在 14+ 业务页里硬编码 `#60A5FA`，否则会出现"改了一处、漏了 N 处"的问题。
