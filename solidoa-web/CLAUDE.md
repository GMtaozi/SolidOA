# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SolidOA is a lightweight office automation system for small-to-medium enterprises, featuring approval workflows, collaboration tools, and financial management. The frontend is a Vue 3 SPA that communicates with a Java backend via REST API.

**Tech Stack:** Vue 3 + Vite + Element Plus + Pinia + Axios + SCSS

## Development Commands

```bash
# Install dependencies
npm install

# Start development server (port 3000, proxies /api to localhost:8080)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

**API Proxy:** Vite dev server proxies `/api/*` requests to `http://localhost:8080` (the Java backend). Ensure the backend is running before testing API calls.

## Architecture

### Directory Structure
```
src/
├── api/           # Axios API modules organized by domain
├── composables/   # Vue composables (useTime, etc.)
├── router/        # Vue Router configuration
├── stores/        # Pinia state management
├── styles/        # Global styles
└── views/         # Page components
    ├── approval/   # 审批管理
    ├── attendance/ # 考勤打卡
    ├── collab/     # 协作办公 (通讯录、消息、日程)
    ├── dashboard/   # 仪表盘
    ├── finance/     # 财务管理 (报销)
    ├── home/        # 首页
    ├── layout/      # 主布局 (侧边栏 + 头部)
    ├── login/       # 登录页
    ├── system/      # 系统管理 (用户、部门、角色)
    └── workflow/    # 工作流 (请假、用印、采购、抄送)
```

### Key Patterns
- **Views:** Each feature module has its own folder under `views/`
- **Dialogs:** Custom dialogs use `.dialog-overlay` + `.dialog` classes, not Element Plus `el-dialog`
- **API Modules:** `src/api/` contains `system.js` (系统+协作), `workflow.js` (审批流), `hr.js` (考勤+财务), `file.js` (文件)
- **Routing:** All routes are lazy-loaded in `src/router/index.js`

### Design System

The project has a documented design system in `PRODUCT.md` and `DESIGN.md` at the workspace root. Key design tokens:

| Token | Value | Usage |
|--------|-------|-------|
| Primary | `#60A5FA` | Main accent color (cloud blue) |
| Success | `#34D399` | Approve/pass states (mint green) |
| Warning | `#FBBF24` | Pending states (honey yellow) |
| Danger | `#FCA5A5` | Reject/error states (peach red) |
| BG Primary | `#f7f5f2` | Page background |
| Text Primary | `#3B3B3B` | Main text |
| Border | `#F0EDE9` | Dividers, borders |

**Standard Component Specs (from DESIGN.md):**
- Input/Textarea: `border-radius: 8px`, `padding: 10px 16px`, focus glow `0 0 0 2px rgba(96,165,250,0.2)`
- Buttons: `border-radius: 12px`, `padding: 10px 20px`
- Cards: `border-radius: 16px`, shadow on hover
- Dialogs: `border-radius: 16px`, `max-width: 480px`

## API Integration

Backend runs on `http://localhost:8080` (gateway). API structure:
- `POST /api/v1/auth/login` - Authentication (system-service)
- `GET/POST /api/v1/system/*` - System management + 协作 (system-service:8081)
- `GET/POST /api/v1/workflow/*` - Workflow operations (workflow-service:8082)
- `GET/POST /api/v1/hr/*` - HR: 考勤 + 财务 (hr-service:8085)
- `GET/POST /api/v1/file/*` - File operations (file-service:8086)
- `GET/POST /api/v1/dingtalk/*` - DingTalk integration (dingtalk-service:8087)

## Icon Usage

Use Element Plus icons only. Available icons: `HomeFilled`, `DataLine`, `DocumentChecked`, `Money`, `TrendCharts`, `Calendar`, `Check`, `Memo`, `User`, `OfficeBuilding`, `Setting`, `ArrowDown`, `Message`, `Avatar`, `Expand`, `Stamp`. Import from `@element-plus/icons-vue`.

## State Management

User authentication state is managed via Pinia store (`src/stores/user.js`). The router guard checks `userStore.isLoggedIn` before allowing access to protected routes.

## CSS Conventions

- All component styles use SCSS with scoped styling
- Design tokens defined as local SCSS variables within each component
- Follow the design system specs for radius, padding, shadows
- Use `rgba($primary, 0.1)` for tinted backgrounds rather than opacity on solid colors
