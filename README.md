# ![项目图片](./doc/img/ShingToPic-2.png)

# **智能协同云图库平台**

## 📌 项目介绍

基于 **Vue 3 + Spring Boot + COS + WebSocket** 开发的 **智能协同云图库平台**，支持公开上传、检索、管理和协同编辑图片。

### 🌟 主要功能：

- **📂 公开图库**：所有用户可上传和搜索图片素材，适用于表情包、设计素材、壁纸等网站。
- **🔍 管理员功能**：支持图片上传、审核、管理及内容分析。
- **🖼️ 个人空间**：用户可上传图片至私有空间进行管理、检索、编辑及分析，支持个人网盘、相册、作品集等用途。
- **👥 团队协作**：企业可创建团队空间，邀请成员 **实时协同编辑图片**，适用于企业活动相册、内部素材库等。


该项目涵盖 **文件存管、内容检索、权限控制、实时协同** 等主流业务场景，并通过多种架构设计方法和优化策略，确保项目的 **高效迭代与稳定运行**。

---

## 🛠 技术选型

### 🚀 **后端技术栈**

- **🌐 框架**：Java **Spring Boot**
- **🗄️ 数据库**：MySQL + **MyBatis-Plus** + **MyBatis X**
- **⚡ 缓存**：Redis 分布式缓存 + Caffeine 本地缓存
- **📡 数据抓取**：Jsoup
- **🗂️ 存储**：⭐ **COS 对象存储**
- **🔀 分库分表**：⭐ **ShardingSphere**
- **🔐 权限控制**：⭐ **Sa-Token**
- **🏛️ 领域设计**：⭐ **DDD 领域驱动设计**
- **🔄 实时通信**：⭐ **WebSocket 双向通信**
- **⚡ 高性能队列**：⭐ **Disruptor**
- **⚙️ 并发编程**：⭐ **JUC 并发 & 异步编程**
- **🎨 AI 接入**：⭐ **AI 绘图大模型**
- **🏗️ 设计模式**：⭐ 多种设计模式的实践
- **🚀 项目优化**：⭐ 多角度优化 **(性能 / 成本 / 安全性)**

### 🎨 **前端技术栈**

- **🖥️ 框架**：Vue 3
- **⚡ 构建工具**：Vite
- **📦 组件库**：Ant Design Vue
- **🔗 请求库**：Axios
- **📊 状态管理**：Pinia
- **📈 其他组件**：数据可视化、图片编辑等
- **🛠️ 前端工程化**：⭐ ESLint + Prettier + TypeScript
- **📜 API 代码生成**：⭐ OpenAPI

---

✨ 该项目凭借丰富的功能和强大的技术选型，致力于提供 **高效、稳定、安全** 的智能图库体验！