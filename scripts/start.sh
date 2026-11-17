#!/usr/bin/env bash
# 湖南工商大学校园助手 —— 启动脚本集合
# 用法:
#   bash scripts/start-backend.sh   # 启动后端 (8080)
#   bash scripts/start-admin.sh     # 启动管理后台开发服务 (5173)
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

start_backend() {
  echo "== 检查 MySQL / Redis =="
  (mysqladmin -h127.0.0.1 -uroot -pcampus123 ping >/dev/null 2>&1 || sudo service mysql start) || true
  (redis-cli ping >/dev/null 2>&1 || sudo service redis-server start) || true
  echo "== 初始化数据库（可重复执行）=="
  bash "$ROOT_DIR/scripts/init-db.sh" || echo "警告: 数据库初始化失败，若已有数据可忽略"
  echo "== 启动后端 =="
  JAR=$(ls -t "$ROOT_DIR"/backend/target/*.jar 2>/dev/null | grep -v sources | head -1)
  if [ -z "$JAR" ]; then
    echo "未找到已构建的 jar，开始构建..."
    (cd "$ROOT_DIR/backend" && mvn -q clean package -DskipTests)
    JAR=$(ls -t "$ROOT_DIR"/backend/target/*.jar | grep -v sources | head -1)
  fi
  echo "启动: $JAR"
  java -jar "$JAR"
}

start_admin() {
  echo "== 启动管理后台 =="
  cd "$ROOT_DIR/admin"
  [ -d node_modules ] || pnpm install
  pnpm dev
}

case "${1:-}" in
  backend) start_backend ;;
  admin) start_admin ;;
  *) echo "用法: bash scripts/start.sh [backend|admin]" ;;
esac
