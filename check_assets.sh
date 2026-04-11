#!/bin/bash
cd /home/per-ahrens/source/repos/baisch
echo "=== Checking all tracked asset files for git vs disk size mismatches ==="
found=0
while IFS= read -r f; do
  [ -f "$f" ] || continue
  disk=$(wc -c < "$f")
  git_size=$(git show HEAD:"$f" 2>/dev/null | wc -c)
  if [ "$disk" != "$git_size" ]; then
    echo "MISMATCH: $f  disk=$disk  git=$git_size"
    found=1
  fi
done < <(git ls-files android/assets/)
if [ "$found" = "0" ]; then
  echo "All assets match."
fi
