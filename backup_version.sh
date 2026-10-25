#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

mod_id="$(sed -n 's/^mod_id=//p' gradle.properties)"
version="$(sed -n 's/^mod_version=//p' gradle.properties)"
stamp="$(date +%Y-%m-%d_%H-%M-%S)"
dir="versions/${version}_${stamp}"
jar="build/libs/${mod_id}-${version}.jar"

if [[ ! -f "$jar" ]]; then
    echo "Сначала собери мод: $jar не найден" >&2
    exit 1
fi

mkdir -p "$dir"
cp -a src build.gradle gradle.properties settings.gradle gradle gradlew gradlew.bat "$dir"/
cp "$jar" "$dir"/
printf 'Версия: %s\nДата: %s\n\nВ папке: исходники, Gradle-файлы и готовый jar.\nЗависимости: в основной папке libs.\n' \
    "$version" "$stamp" > "$dir/README.txt"

echo "Сохранено: $dir"
