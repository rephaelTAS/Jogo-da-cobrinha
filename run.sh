#!/usr/bin/env bash
#
# Atalho para abrir o jogo. Usa o Maven do sistema quando disponível e, como
# conveniência para o ambiente local de desenvolvimento, cai para o toolchain
# portátil em .tools/ (Java 21 + Maven) se ele existir.
#
set -euo pipefail
cd "$(dirname "$0")"

if command -v mvn >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    exec mvn javafx:run
fi

if [[ -x .tools/apache-maven-3.9.9/bin/mvn ]]; then
    for candidate in .tools/temurin/jdk-21*; do
        if [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]]; then
            export JAVA_HOME="$PWD/$candidate"
            exec .tools/apache-maven-3.9.9/bin/mvn \
                -Dmaven.repo.local="$PWD/.tools/m2" javafx:run
        fi
    done
    if [[ -x .tools/jdkpkg/jdk4py/java-runtime/bin/java ]]; then
        export JAVA_HOME="$PWD/.tools/jdkpkg/jdk4py/java-runtime"
        exec .tools/apache-maven-3.9.9/bin/mvn \
            -Pecj -Dmaven.repo.local="$PWD/.tools/m2" javafx:run
    fi
fi

cat <<'EOF'
Java 21 e Maven 3.9+ são necessários para executar o projeto.

No Ubuntu/Pop!_OS:
    sudo apt install openjdk-21-jdk maven

Depois, rode:
    mvn javafx:run
EOF
exit 1
