# 📊 InOutCountBar Android Custom View

InOutCountBar는 입장(IN), 퇴장(OUT), 현재 인원(COUNT) 세 가지 데이터의 비율을 하나의 막대 그래프로 시각화하는 Android 커스텀 View 컴포넌트입니다. 비율 변화에 따라 부드러운 애니메이션을 제공하며 상태 변화를 직관적으로 보여줍니다.

<img src="app/src/main/assets/demo.gif" width="600" alt="Demo" />

## 📥 JitPack 리포지토리를 추가합니다.

```kotlin
// settings.gradle.kts

dependencyResolutionManagement {
    // ... (기존 설정 유지)
    repositories {
        google()
        mavenCentral()
        // JitPack 리포지토리 추가
        maven { url = uri("https://jitpack.io") }
    }
}
```

## Module build.gradle.kts 의존성 추가
```kotlin
// app/build.gradle.kts (또는 해당 모듈)
dependencies {
    
    // 의존성 추가
    implementation("com.github.wjjasd:InoutCountbar:1.0.1")
}
```

## 사용방법
```xml
<com.utarex.inoutbar.InOutCountBar
    android:id="@+id/inOutBar"
    android:layout_width="match_parent"
    android:layout_height="100dp"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:barInColor="#4CAF50"
    app:barOutColor="#F44336"
    app:barCountColor="#2196F3" />
```

```kotlin
// MainActivity.kt

// ...

val inOutBar: InOutCountBar = findViewById(R.id.inOutBar)

// IN, OUT, COUNT 값을 설정합니다.
inOutBar.inValue = 45
inOutBar.outValue = 30
inOutBar.countValue = 25 

// 하단에 표시될 텍스트를 업데이트합니다.
inOutBar.lastEntry = "최근 입장: 10:30"
inOutBar.lastExit = "최근 퇴장: 10:45"
```

## ⚙️ 커스텀 속성 목록

| 속성명             | 타입        | 설명 |
|--------------------|-------------|------|
| `barInColor`       | color       | In 바 색상 |
| `barOutColor`      | color       | Out 바 색상 |
| `barCountColor`    | color       | Count 바 색상 |
| `entryTextColor`   | color       | Entry 텍스트 색상 |
| `exitTextColor`    | color       | Exit 텍스트 색상 |
| `entryTextSize`    | dimension   | Entry 텍스트 크기 |
| `exitTextSize`     | dimension   | Exit 텍스트 크기 |
