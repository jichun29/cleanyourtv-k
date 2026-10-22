#!/usr/bin/env bash
# 还原：把 disable.sh 停用的包全部重新启用。
# 用法: TV_HOST=192.168.x.x ./restore.sh ../examples/<机型>/debloat.txt
set -euo pipefail
LIST="${1:?用法: restore.sh <debloat.txt 路径>}"
HOST="${TV_HOST:?先 export TV_HOST=<电视IP>}"
adb connect "$HOST:5555" >/dev/null
PKGS=$(sed -e 's/\r$//' -e 's/^\xef\xbb\xbf//' "$LIST" | grep -vE '^\s*(#|$)' | sed -E 's/\s+#.*$//' | awk '{print $1}')
echo "$PKGS" | while read -r pkg; do
  adb -s "$HOST:5555" shell pm enable "$pkg" 2>/dev/null && echo "已恢复: $pkg" || echo "失败/不存在: $pkg"
done
echo "完成。"
