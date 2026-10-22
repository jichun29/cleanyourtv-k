#!/usr/bin/env bash
# 禁用 debloat 清单里的所有包（只停用，不卸载；还原用 restore.sh）。
# 用法: TV_HOST=192.168.x.x ./disable.sh ../examples/<机型>/debloat.txt
set -euo pipefail
LIST="${1:?用法: disable.sh <debloat.txt 路径>}"
HOST="${TV_HOST:?先 export TV_HOST=<电视IP>}"
adb connect "$HOST:5555" >/dev/null
# 过滤注释和空行，校验每一行都是合法包名（清单来自社区贡献，包名会拼进 shell 命令，必须严检）
PKGS=$(sed -e 's/\r$//' -e 's/^\xef\xbb\xbf//' "$LIST" | grep -vE '^\s*(#|$)' | sed -E 's/\s+#.*$//' | awk '{print $1}')
BAD=$(printf '%s\n' "$PKGS" | grep -vE '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$' || true)
[ -n "$BAD" ] && { echo "拒绝执行：以下行不是合法包名:"; echo "$BAD"; exit 1; }
echo "$PKGS" | while read -r pkg; do
  if adb -s "$HOST:5555" shell pm list packages "$pkg" 2>/dev/null | grep -q .; then
    adb -s "$HOST:5555" shell pm disable-user --user 0 "$pkg" && echo "已停用: $pkg" || echo "失败: $pkg"
  else
    echo "本机没有: $pkg（跳过）"
  fi
done
echo "完成。想还原：TV_HOST=$HOST ./restore.sh $LIST"
