#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"
DEFAULT_JVM_OPTS='""'
MAX_FD="maximum"
warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }
if [ "$APP_HOME" ] ; then
    case "$APP_HOME" in
        *\\* ) APP_HOME=`echo $APP_HOME | sed 's@\\\\@/@g'`;;
    esac
fi
for arg do
    if [ "x$arg" = "x--no-daemon" ] ; then
        DEFAULT_JVM_OPTS=`echo "$DEFAULT_JVM_OPTS" | sed 's/-Dorg.gradle.daemon=true/-Dorg.gradle.daemon=false/g'`
    fi
done
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE="java"
exec "$JAVA_EXE" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
