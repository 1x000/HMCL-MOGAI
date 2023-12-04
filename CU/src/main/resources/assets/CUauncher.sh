#!/usr/bin/env bash

set -e

# Switch message language
if [ -z "${LANG##zh_*}" ]; then
  _CU_USE_CHINESE=true
else
  _CU_USE_CHINESE=false
fi

# _CU_OS
case "$OSTYPE" in
  linux*)
    _CU_OS="linux";;
  darwin*)
    _CU_OS="osx";;
  msys*|cygwin*)
    _CU_OS="windows";;
  *)
    _CU_OS="unknown";;
esac

# Normalize _CU_ARCH
case "$(uname -m)" in
  x86_64|x86-64|amd64|em64t|x64)
    _CU_ARCH="x86_64";;
  x86_32|x86-32|x86|ia32|i386|i486|i586|i686|i86pc|x32)
    _CU_ARCH="x86";;
  arm64|aarch64|armv8*|armv9*)
    _CU_ARCH="arm64";;
  arm|arm32|aarch32|armv7*)
    _CU_ARCH="arm32";;
  loongarch64)
    _CU_ARCH="loongarch64";;
  *)
    _CU_ARCH="unknown";;
esac

# Self path
_CU_PATH="${BASH_SOURCE[0]}"
_CU_DIR=$(dirname "$_CU_PATH")

if [ "$_CU_OS" == "windows" ]; then
  _CU_JAVA_EXE_NAME="java.exe"
else
  _CU_JAVA_EXE_NAME="java"
fi

# _CU_VM_OPTIONS
if [ -n "${CU_JAVA_OPTS+x}" ]; then
  _CU_VM_OPTIONS=${CU_JAVA_OPTS}
else
  _CU_VM_OPTIONS="-XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=15"
fi

# First, find Java in CU_JAVA_HOME
if [ -n "${CU_JAVA_HOME+x}" ]; then
  if [ -x "$CU_JAVA_HOME/bin/$_CU_JAVA_EXE_NAME" ]; then
    "$CU_JAVA_HOME/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
    exit 0
  else
    if [ "$_CU_USE_CHINESE" == true ]; then
      echo "环境变量 CU_JAVA_HOME 的值无效，请设置为合法的 Java 路径。" 1>&2
    else
      echo "The value of the environment variable CU_JAVA_HOME is invalid, please set it to a valid Java path." 1>&2
    fi
    exit 1
  fi
fi

# Find Java in CU_DIR
case "$_CU_ARCH" in
  x86_64)
    if [ -x "$_CU_DIR/jre-x64/bin/$_CU_JAVA_EXE_NAME" ]; then
      "$_CU_DIR/jre-x64/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
      exit 0
    fi
    if [ -x "$_CU_DIR/jre-x86/bin/$_CU_JAVA_EXE_NAME" ]; then
      "$_CU_DIR/jre-x86/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
      exit 0
    fi
    ;;
  x86)
    if [ -x "$_CU_DIR/jre-x86/bin/$_CU_JAVA_EXE_NAME" ]; then
      "$_CU_DIR/jre-x86/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
      exit 0
    fi
    ;;
  arm64)
    if [ -x "$_CU_DIR/jre-arm64/bin/$_CU_JAVA_EXE_NAME" ]; then
      "$_CU_DIR/jre-arm64/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
      exit 0
    fi
    ;;
  arm32)
    if [ -x "$_CU_DIR/jre-arm32/bin/$_CU_JAVA_EXE_NAME" ]; then
      "$_CU_DIR/jre-arm32/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
      exit 0
    fi
    ;;
  loongarch64)
    if [ -x "$_CU_DIR/jre-loongarch64/bin/$_CU_JAVA_EXE_NAME" ]; then
      "$_CU_DIR/jre-loongarch64/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
      exit 0
    fi
    ;;
esac

# Find Java in JAVA_HOME
if [ -f "$JAVA_HOME/bin/$_CU_JAVA_EXE_NAME" ]; then
  "$JAVA_HOME/bin/$_CU_JAVA_EXE_NAME" $_CU_VM_OPTIONS -jar "$_CU_PATH"
  exit 0
fi

# Find Java in PATH
if [ -x "$(command -v $_CU_JAVA_EXE_NAME)" ]; then
  $_CU_JAVA_EXE_NAME $_CU_VM_OPTIONS -jar "$_CU_PATH"
  exit 0
fi

# Java not found

if [[ "$_CU_OS" == "unknown" || "$_CU_ARCH" == "unknown" ]]; then
  if [ "$_CU_USE_CHINESE" == true ]; then
    echo "运行 CU 需要 Java 运行时环境，请安装 Java 并设置环境变量后重试。" 1>&2
  else
    echo "The Java runtime environment is required to run CU. " 1>&2
    echo "Please install Java and set the environment variables and try again." 1>&2
  fi
  exit 1
fi

if [[ "$_CU_ARCH" == "loongarch64" ]]; then
  if [ "$_CU_USE_CHINESE" == true ]; then
    echo "运行 CU 需要 Java 运行时环境，请安装龙芯 JDK8 (https://docs.CU.net/downloads/loongnix.html) 并设置环境变量后重试。" 1>&2
  else
    echo "The Java runtime environment is required to run CU." 1>&2
    echo "Please install Loongson JDK8 (https://docs.CU.net/downloads/loongnix.html) and set the environment variables, then try again." 1>&2
  fi
  exit 1
fi


case "$_CU_OS" in
  linux)
    _CU_DOWNLOAD_PAGE_OS="linux";;
  osx)
    _CU_DOWNLOAD_PAGE_OS="macos";;
  windows)
    _CU_DOWNLOAD_PAGE_OS="windows";;
  *)
    echo "Unknown os: $_CU_OS" 1>&2
    exit 1
    ;;
esac

case "$_CU_ARCH" in
  arm64)
    _CU_DOWNLOAD_PAGE_ARCH="arm64";;
  arm32)
    _CU_DOWNLOAD_PAGE_ARCH="arm32";;
  x86_64)
    _CU_DOWNLOAD_PAGE_ARCH="x86_64";;
  x86)
    _CU_DOWNLOAD_PAGE_ARCH="x86";;
  *)
    echo "Unknown architecture: $_CU_ARCH" 1>&2
    exit 1
    ;;
esac

_CU_DOWNLOAD_PAGE="https://docs.CU.net/downloads/$_CU_DOWNLOAD_PAGE_OS/$_CU_DOWNLOAD_PAGE_ARCH.html"

if [ "$_CU_USE_CHINESE" == true ]; then
  echo "运行 CU 需要 Java 运行时环境，请安装 Java 并设置环境变量后重试。" 1>&2
  echo "$_CU_DOWNLOAD_PAGE" 1>&2
else
  echo "The Java runtime environment is required to run CU. " 1>&2
  echo "Please install Java and set the environment variables and try again." 1>&2
  echo "$_CU_DOWNLOAD_PAGE" 1>&2
fi
exit 1
