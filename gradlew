#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" || exit; pwd -P)
APP_NAME="Gradle"

DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

MAX_FD=maximum
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  MSYS* | MINGW*) msys=true ;;
  NONSTOP*) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVACMD=$JAVA_HOME/jre/sh/java
  else
    JAVACMD=$JAVA_HOME/bin/java
  fi
  if [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME points to an invalid directory: $JAVA_HOME" >&2
    echo "Install JDK 17 and set JAVA_HOME to its installation directory." >&2
    exit 1
  fi
else
  JAVACMD=java
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java is not installed or not available in PATH." >&2
    echo "Install JDK 17 before running Gradle." >&2
    exit 1
  fi
fi

if [ "$cygwin" = "false" ] && [ "$darwin" = "false" ] && [ "$nonstop" = "false" ]; then
  case $MAX_FD in
    max*)
      MAX_FD=$(ulimit -H -n) || true
      ;;
  esac
  case $MAX_FD in
    '' | soft) : ;;
    *)
      ulimit -n "$MAX_FD" || true
      ;;
  esac
fi

if $darwin; then
  GRADLE_OPTS="$GRADLE_OPTS -Xdock:name=$APP_NAME"
fi

if $cygwin || $msys; then
  APP_HOME=$(cygpath --path --mixed "$APP_HOME")
  CLASSPATH=$(cygpath --path --mixed "$CLASSPATH")
  JAVACMD=$(cygpath --unix "$JAVACMD")
fi

set -- \
  "-Dorg.gradle.appname=$APP_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "$@"
