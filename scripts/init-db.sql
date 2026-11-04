-- 功夫吉特 gitKunfu 初始化脚本
-- 使用：mysql -u root -p < scripts/init-db.sql
-- 应用只使用下面这个专用账号，不直接使用 root。

CREATE DATABASE IF NOT EXISTS gitkunfu
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'gitkunfu'@'localhost' IDENTIFIED BY 'gitkunfu_2026';
GRANT ALL PRIVILEGES ON gitkunfu.* TO 'gitkunfu'@'localhost';
FLUSH PRIVILEGES;
