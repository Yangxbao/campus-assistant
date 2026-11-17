#!/usr/bin/env bash
# =====================================================================
# 湖南工商大学校园助手 —— 数据库初始化脚本（可重复执行）
# 导入 sql/schema.sql（表结构）与 sql/data_demo.sql（演示数据，非真实校园资料）。
# 支持环境变量覆盖：MYSQL_HOST、MYSQL_PORT、MYSQL_DB、MYSQL_USER、MYSQL_PASSWORD
# =====================================================================
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-campus_assistant}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-campus123}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="$SCRIPT_DIR/../sql"

echo "[init-db] 目标: ${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB} (用户 ${MYSQL_USER})"

# 若库不存在则创建（utf8mb4）
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  -e "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DB}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入表结构（schema 内部含 USE，可重复执行：CREATE TABLE IF NOT EXISTS）
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < "$SQL_DIR/schema.sql"
echo "[init-db] schema.sql 导入完成"

# 导入演示数据（演示数据，非真实校园资料；幂等插入）
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < "$SQL_DIR/data_demo.sql"
echo "[init-db] data_demo.sql 导入完成"

# 简单校验
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" \
  -e "SELECT COUNT(*) AS buildings FROM t_building; SELECT COUNT(*) AS classrooms FROM t_classroom; SELECT username,role FROM t_admin_user;"
echo "[init-db] 完成"
