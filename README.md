# 功夫吉特 · gitKunfu

功夫吉特（gitKunfu）是一个可以跑在本机上的代码托管网站。界面和日常流程参照 GitHub：注册登录、建仓库、浏览代码、看提交历史、管理分支、提 Issue、发 Pull Request 并合并。Git 部分走的是标准 Smart HTTP 协议，本机的 `git clone / push / pull` 直接可用，不是模拟。

当前版本面向本机回环使用，服务只监听 `127.0.0.1:8080`，不对外网开放。

## 功能

- 账号：注册、登录、JWT 会话、修改个人资料、个人主页
- 仓库：新建、编辑描述与可见性、切换默认分支、删除（需输入仓库名确认）
- 代码浏览：目录树、文件内容、语法高亮、行号、Markdown 渲染、单文件下载
- 提交历史：按分支查看、分页、提交详情与逐文件 diff
- 分支：分支列表与切换
- 对比：任意两个分支之间的提交与差异预览
- Issue：新建、列表筛选、详情时间线、评论、关闭/重新打开
- Pull Request：选择分支发起、差异预览、合并、冲突检测、关闭
- 初始化引导：空仓库页给出本地新建、推送已有仓库、从其他平台镜像迁移三套命令，可逐块复制
- Git 管道：`git clone`、`git push`、`git pull`、`git fetch`，私有仓库鉴权，仅仓库所有者可推送
- 界面：Vue 3 单页应用，响应式布局，内置代码高亮与低饱和配色

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、Spring JDBC |
| 数据库 | MySQL 8（库名 `gitkunfu`） |
| Git 引擎 | JGit 7.8（裸仓库 + Smart HTTP Servlet） |
| 鉴权 | JWT（HS384）+ BCrypt |
| 前端 | Vue 3、Vue Router 4、Vite 6、Axios |
| 渲染 | marked、DOMPurify、highlight.js |

## 目录结构

```
gitKunfu/
├─ backend/                     Spring Boot 服务端
│  ├─ src/main/java/com/gitkunfu/
│  │  ├─ auth/                  登录、JWT、拦截器
│  │  ├─ user/                  用户资料
│  │  ├─ repo/                  仓库、Git HTTP、存储
│  │  ├─ issue/                 Issue
│  │  ├─ pull/                  Pull Request
│  │  ├─ config/                配置、SPA 转发
│  │  └─ common/                异常、通用返回
│  └─ src/main/resources/
│     ├─ application.yml        端口、数据库、仓库根目录
│     ├─ schema.sql             建表语句（启动自动执行）
│     └─ static/                前端构建产物
├─ frontend/                    Vue 3 前端源码
├─ data/repositories/           Git 裸仓库（运行后生成）
├─ scripts/init-db.sql          数据库与专用账号初始化
├─ start-gitkunfu.bat           双击启动
└─ stop-gitkunfu.bat            停止服务
```

## 环境要求

- JDK 21 或更高版本
- MySQL 8，已启动
- Node.js 18 以上（只在改前端源码时需要）
- Git 2.x（用来做 clone / push）

## 首次部署

### 1. 初始化数据库

用 MySQL 的 root 账号执行一次：

```bash
mysql -u root -p < scripts/init-db.sql
```

脚本会创建数据库 `gitkunfu`，以及应用专用的账号 `gitkunfu`（密码 `gitkunfu_2026`，仅授权本库）。应用运行时不会使用 root。

### 2. 构建后端

```bash
mvn -f backend/pom.xml -DskipTests package
```

产物是 `backend/target/gitkunfu-server.jar`。如果本机没有全局 Maven，可以安装 Apache Maven 3.9 或更高版本后再执行。

### 3. 构建前端（修改过前端源码时）

```bash
cd frontend
npm install
npm run build
```

然后把 `frontend/dist` 里的内容复制到 `backend/src/main/resources/static/`，再执行一次第 2 步打包。前端已经打进 jar 的情况下可以跳过这一步。

### 4. 启动

双击 `start-gitkunfu.bat`。脚本会检查端口、在后台启动服务、等待就绪并打开浏览器。也可以手动运行：

```bash
java -jar backend/target/gitkunfu-server.jar
```

访问 <http://localhost:8080>，先注册一个账号即可开始使用。停止服务双击 `stop-gitkunfu.bat`。

## 用 Git 连接

服务默认地址是 <http://localhost:8080>，仓库地址格式为：

```bash
http://localhost:8080/git/<用户名>/<仓库名>.git
```

仓库首页右上角会显示当前仓库的实际克隆地址，点「复制」即可。本项目的源码地址：

```bash
# 功夫吉特（本机）
http://localhost:8080/git/akun/git-kunfu.git

# GitHub（镜像）
https://github.com/akun666yyds/git-kunfu.git
```

### 克隆与推送

从功夫吉特克隆，并把 GitHub 添加为第二个远程：

```bash
git clone http://localhost:8080/git/akun/git-kunfu.git
cd git-kunfu
git remote add github https://github.com/akun666yyds/git-kunfu.git

# 修改代码后
git add .
git commit -m "说明这次改动"
git push origin main
git push github main
```

从 GitHub 克隆时反过来添加本机远程：

```bash
git clone https://github.com/akun666yyds/git-kunfu.git
cd git-kunfu
git remote add gitkunfu http://localhost:8080/git/akun/git-kunfu.git
git push gitkunfu main
```

推送功夫吉特时输入本站的账号密码，推送 GitHub 时使用 GitHub 的凭据。只有仓库所有者可以推送，公开仓库任何人都能克隆；私有仓库必须登录后才能访问，未登录会返回 401。

### 新建仓库后的初始化

新建仓库时如果没有勾选初始化 README，仓库首页会给出三套可直接复制的命令，把 `<用户名>`、`<仓库名>` 换成实际值即可。

本地新建仓库：

```bash
git init
git add .
git commit -m "第一次提交"
git branch -M main
git remote add origin http://localhost:8080/git/<用户名>/<仓库名>.git
git push -u origin main
```

推送已有的本地仓库：

```bash
cd 你的项目目录
git remote add origin http://localhost:8080/git/<用户名>/<仓库名>.git
git branch -M main
git push -u origin main
```

如果项目里已经有 `origin`，把第二行换成：

```bash
git remote set-url origin http://localhost:8080/git/<用户名>/<仓库名>.git
```

### 从其他平台迁移

已有的 GitHub、Gitee 等仓库可以连同分支和标签整体搬过来：

```bash
git clone --mirror https://github.com/<账号>/<仓库>.git
cd <仓库>.git
git remote set-url origin http://localhost:8080/git/<用户名>/<仓库>.git
git push --mirror
```

例如把 GitHub 上的 `highschool-runtime` 迁到本机主账号：

```bash
git clone --mirror https://github.com/akun666yyds/highschool-runtime.git
cd highschool-runtime.git
git remote set-url origin http://localhost:8080/git/akun/highschool-runtime.git
git push --mirror
```

镜像会同步全部分支、标签与引用。源仓库需要登录时，克隆阶段按提示输入原平台凭据；迁移完成后本地的裸仓库可以直接删除。仓库页的初始化引导里也能直接复制这套命令。

## 数据与备份

- 元数据（用户、仓库信息、Issue、PR）在 MySQL 的 `gitkunfu` 库中。
- 代码本身存放在 `data/repositories/<仓库ID>.git`，是标准裸仓库，可以直接用 Git 命令读取。
- 备份时把数据库和 `data/repositories` 一起打包即可；两者要保持同一时间点，避免元数据与仓库内容对不上。

## 配置

配置文件是 `backend/src/main/resources/application.yml`，常用项：

- `server.port`：服务端口，默认 8080
- `server.address`：监听地址，默认 `127.0.0.1`
- `spring.datasource.*`：数据库连接，账号密码可用环境变量 `GITKUNFU_DB_USER`、`GITKUNFU_DB_PASSWORD` 覆盖
- `gitkunfu.repository-root`：裸仓库存放目录
- `gitkunfu.base-url`：生成克隆地址时使用的站点地址
- `gitkunfu.jwt-secret`：会话签名密钥，可用环境变量 `GITKUNFU_JWT_SECRET` 覆盖；文件里的默认值只用于本地开发，部署前务必更换

修改配置后需要重新打包或通过启动参数覆盖，例如 `--server.port=9090`。

## 安全说明

本项目当前定位是本机实验环境：

- 服务只绑定回环地址，局域网内其他机器无法直接访问。
- 使用 HTTP 明文传输，不适合直接暴露到公网。
- 数据库密码与 JWT 密钥带有本地开发默认值，可用环境变量覆盖；公开部署前必须换成自己的值。

如果要放到服务器上，至少需要：改为 HTTPS、重设数据库密码与 JWT 密钥、限制注册入口、增加推送频率与仓库大小限制。

## 作者

啊坤 · <akun666yyds@gmail.com>
