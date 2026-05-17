# Dispara SMS

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" alt="Dispara SMS" width="120" height="120">
</p>

<p align="center">
  <strong>App Android para envio de SMS em massa em Moçambique</strong>
  <br>
  Rápido • Leve • Offline • Dual SIM • Sem gateways externos
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin">
  <img src="https://img.shields.io/badge/Compose-BOM--2024.12.01-blue?logo=jetpackcompose">
  <img src="https://img.shields.io/badge/API-24%2B-green">
  <img src="https://img.shields.io/badge/APK-8MB-brightgreen">
  <img src="https://img.shields.io/badge/License-MIT-blue">
</p>

---

## Visão Geral

O **Dispara SMS** permite enviar SMS em massa para centenas ou milhares de contactos directamente do teu smartphone Android, usando o plano de SMS da tua operadora — sem gateways externos, sem custos adicionais.

Criado especialmente para o mercado moçambicano, com suporte para **Vodacom**, **Movitel** e **Tmcel**, e optimizado para funcionar perfeitamente em celulares básicos e médios com pouca memória RAM e conexões lentas.

### Funcionalidades Principais

| Funcionalidade | Descrição |
|---|---|
| **Disparo em Massa** | Envia SMS para milhares de contactos com fila inteligente |
| **Grupos** | Organiza contactos em grupos ilimitados com tags e favoritos |
| **Importação Excel** | Importa contactos via XLSX e CSV com mapeamento automático |
| **Mensagens Personalizadas** | Usa `{first_name}`, `{last_name}`, `{phone}` nas mensagens |
| **Dual SIM** | Escolhe qual SIM usar para cada campanha |
| **Retry Inteligente** | Reconhece falhas e reenvia automaticamente |
| **Offline-first** | Funciona completamente offline, sem necessidade de internet |
| **Histórico** | Relatório completo de todas as campanhas |
| **Modo Escuro** | Tema escuro nativo (opcional) |

---

## Stack Tecnológica

| Tecnologia | Versão | Função |
|---|---|---|
| **Kotlin** | 2.0.21 | Linguagem principal |
| **Jetpack Compose** | BOM 2024.12.01 | UI declarativa moderna |
| **Material 3** | — | Design System |
| **Room** | 2.6.1 | Banco de dados local (SQLite) |
| **Hilt** | 2.54 | Injecção de dependências |
| **WorkManager** | 2.10.0 | Tarefas em background |
| **Navigation Compose** | 2.8.5 | Navegação entre telas |
| **Coroutines** | 1.9.0 | Async/threading |
| **Apache POI** | 5.3.0 | Leitura de ficheiros Excel |
| **Kotlin CSV** | 1.10.0 | Leitura de ficheiros CSV |

### Porquê Kotlin nativo e não React Native?

1. **API de SMS** — `SmsManager` e `SubscriptionManager` (dual SIM) são APIs nativas. React Native precisaria de módulos nativos para cada funcionalidade crítica.
2. **Performance em low-end** — Kotlin nativo consome ~50-80MB RAM a menos que React Native.
3. **WorkManager** — Sobrevive a reinicializações, Doze Mode, e optimizações OEM (Xiaomi, Huawei, Samsung).
4. **APK enxuto** — ~8MB vs 30-40MB de uma app React Native + Expo.

---

## Pré-requisitos

### Para compilar (qualquer ambiente)

| Requisito | Versão Mínima |
|---|---|
| **JDK** | 17+ |
| **Android SDK** | 35 (compileSdk) |
| **Android Build Tools** | 35.0.0 |
| **Gradle** | 8.11.1 (gerido pelo wrapper) |

---

## Instalação e Configuração

### 🔹 GitHub Codespaces (recomendado)

O ambiente já vem com Java e Android SDK pré-instalados.

```bash
# 1. Clonar o repositório
git clone https://github.com/Zacarias-thequimo/m-sms.git
cd m-sms

# 2. Compilar APK Debug
./gradlew assembleDebug
```

O APK estará em:
```
app/build/outputs/apk/debug/app-debug.apk
```

Para release (menor, optimizado):
```bash
./gradlew assembleRelease
```

---

### 🔹 Linux (Ubuntu/Debian)

#### Passo 1: Instalar JDK 17+

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version
```

Se preferir JDK 21 (compatível):
```bash
sudo apt install openjdk-21-jdk -y
```

#### Passo 2: Instalar Android SDK Command-Line Tools

```bash
mkdir -p ~/Android && cd ~/Android

# Baixar command-line tools
curl -sL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o cmdline-tools.zip
unzip -q cmdline-tools.zip
rm cmdline-tools.zip

# Organizar a estrutura (o zip extrai para cmdline-tools/, precisa estar em cmdline-tools/latest/)
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null

# Verificar se o sdkmanager está acessível
ls ~/Android/cmdline-tools/latest/bin/
```

#### Passo 3: Configurar variáveis de ambiente

Adiciona ao `~/.bashrc` ou `~/.zshrc`:

```bash
export ANDROID_HOME=$HOME/Android
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
# Opcional: se o Java não for encontrado automaticamente
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

Carregar as configurações:
```bash
source ~/.bashrc   # ou source ~/.zshrc
```

#### Passo 4: Instalar SDK platforms e build tools

```bash
yes | sdkmanager --install "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

#### Passo 5: Compilar

```bash
cd /caminho/para/m-sms

# Gerar Gradle Wrapper (se não existir)
gradle wrapper --gradle-version 8.11.1

# Compilar
export ANDROID_HOME=$HOME/Android
./gradlew assembleDebug
```

#### Solução de problemas comuns

| Erro | Solução |
|---|---|
| `java: command not found` | Instalar JDK: `sudo apt install openjdk-17-jdk` |
| `sdkmanager: command not found` | Verificar PATH ou usar caminho completo: `~/Android/cmdline-tools/latest/bin/sdkmanager` |
| `No connected devices` | Ignorar — estás só a compilar o APK |
| `Unsupported class file major version` | JDK muito novo. Usa JDK 17: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` |
| Erro de permissão `./gradlew` | `chmod +x gradlew` |

---

### 🔹 macOS

```bash
# JDK
brew install openjdk@17

# Android SDK Command-Line Tools
brew install --cask android-commandlinetools

# Ou manualmente (mesmo processo do Linux)
mkdir -p ~/Android
cd ~/Android
curl -sL "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip" -o cmdline-tools.zip
unzip -q cmdline-tools.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/
export ANDROID_HOME=$HOME/Android
yes | sdkmanager --install "platforms;android-35" "build-tools;35.0.0"

cd /caminho/para/m-sms
./gradlew assembleDebug
```

---

### 🔹 Windows (PowerShell)

```powershell
# 1. Instalar JDK 17
# Baixar de: https://adoptium.net/
# Ou via winget:
winget install EclipseAdoptium.Temurin.17.JDK

# 2. Android SDK Command-Line Tools
mkdir $env:USERPROFILE\Android
cd $env:USERPROFILE\Android
Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile "cmdline-tools.zip"
Expand-Archive -Path "cmdline-tools.zip" -DestinationPath "."
mkdir cmdline-tools\latest
move cmdline-tools\* cmdline-tools\latest\

# 3. Variáveis de ambiente (PowerShell persistente)
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:USERPROFILE\Android", "User")
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.14-hotspot", "User")

# 4. SDK platforms
$env:ANDROID_HOME = "$env:USERPROFILE\Android"
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-35" "build-tools;35.0.0"

# 5. Compilar
cd C:\caminho\para\m-sms
.\gradlew.bat assembleDebug
```

---

## Estrutura do Projecto

```
app/
├── build.gradle.kts              # Dependências e configuração de build
├── src/main/
│   ├── AndroidManifest.xml        # Permissões e configuração do app
│   └── java/com/disparasms/app/
│       ├── MzSmsApp.kt           # Application class (Hilt + WorkManager)
│       ├── MainActivity.kt       # Single Activity (Edge-to-Edge + Splash)
│       ├── di/AppModule.kt       # Injecção de dependências
│       ├── data/
│       │   ├── local/
│       │   │   ├── entity/       # Room Entities (Group, Contact, Campaign, Log)
│       │   │   ├── dao/          # Room DAOs com Flow
│       │   │   └── AppDatabase.kt
│       │   └── repository/       # Group, Contact, Campaign, Import
│       ├── sms/
│       │   ├── SmsSender.kt      # Motor de envio SMS (dual SIM)
│       │   ├── SmsQueueManager.kt# Fila inteligente com progresso
│       │   └── SmsWorker.kt      # Worker para background
│       ├── ui/
│       │   ├── theme/            # Design System (Cores, Tipografia, Tema)
│       │   ├── components/       # Componentes reutilizáveis
│       │   ├── navigation/       # Navegação + Bottom Nav
│       │   └── screen/           # Telas e ViewModels
│       └── util/PhoneUtils.kt    # Utilitário para números Moçambique
└── res/                          # Recursos (ícones, strings, temas)
```

---

## Comandos Úteis

```bash
# Compilar APK Debug
./gradlew assembleDebug

# Compilar APK Release
./gradlew assembleRelease

# Limpar build
./gradlew clean

# Verificar dependências
./gradlew app:dependencies

# Executar lint
./gradlew lint

# APK Debug localização
# app/build/outputs/apk/debug/app-debug.apk

# APK Release localização
# app/build/outputs/apk/release/app-release.apk
```

---

## Licença

```
MIT License

Copyright (c) 2026 Zacarias Thequimo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files...
```
