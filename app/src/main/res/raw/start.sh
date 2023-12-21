#!/system/bin/sh

SOURCE=@SOURCE@
TARGET="/data/local/tmp/cleaner_starter"
FAILURE_PREFIX="Failure:"

recopy() {
  rm -rf /data/local/tmp
  mkdir -p /data/local/tmp
  cp "$SOURCE" $TARGET
}

if ! [ -f "$SOURCE" ]; then
  echo "$FAILURE_PREFIX $SOURCE not exist"
  exit 1
else
  rm -f $TARGET

  if ! cp "$SOURCE" $TARGET; then
    recopy
  fi

  if ! [ -f "$TARGET" ]; then
    echo "$FAILURE_PREFIX can't copy starter to $TARGET"
    exit 1
  fi

  chmod 700 $TARGET
  chown 2000 $TARGET
  chgrp 2000 $TARGET
fi

if $TARGET "$1"; then
  echo "Success"
fi
