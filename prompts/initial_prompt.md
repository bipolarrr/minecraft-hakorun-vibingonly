당신은 숙련된 Minecraft Paper 플러그인 개발자입니다.
목표는 “하코런(하드코어 엔더런)” 콘텐츠를 구현하는 PaperMC 플러그인을 완성하는 것입니다.

반드시 실제로 빌드 가능한 프로젝트 형태로 작성하세요.
설명 위주가 아니라, 소스코드/설정파일/프로젝트 구조를 모두 생성해야 합니다.

## 기술 스택
- PaperMC 1.21.11 API
- Java 21
- Maven 프로젝트
- plugin.yml 사용
- 패키지명: com.example.hakorun
- 플러그인명: HakoRun
- 버전: 1.0.0-SNAPSHOT

## 플러그인 목표
이 플러그인은 마인크래프트 하코런 콘텐츠의 핵심 규칙을 서버에서 자동으로 운영한다.

하코런의 핵심 규칙:
1. 시도 회차를 “지구”라고 부른다.
2. 각 런은 독립된 Overworld / Nether / End 세트를 가진다.
3. 플레이어들은 팀으로 한 런에 참가한다.
4. 팀은 공유 생명(lives)을 가진다. 기본값은 3.
5. 누군가 죽으면 공유 생명이 1 감소한다.
6. 누가 죽었는지 즉시 전체 공개하지 않는 옵션이 있어야 한다.
7. 남은 생명이 0이 되면 현재 런은 실패 처리되고, 모든 플레이어를 로비로 이동시킨다.
8. 새 런을 시작하면 “다음 지구”로 넘어가며 새 월드 세트를 생성한다.
9. 엔더드래곤 처치 후 엔드 포털 진입 시 런 성공으로 처리한다.
10. 총 실패 횟수와 총 승리 횟수는 서버 재시작 후에도 유지된다.

## 반드시 구현할 기능

### 1) 런 상태 관리
상태머신을 구현하라:
- LOBBY
- RUNNING
- FAILED
- WON

현재 지구 번호(attemptIndex)를 관리하라.
첫 시작은 1지구로 시작한다.
실패할 때마다 다음 시작은 2지구, 3지구처럼 증가한다.
성공 후에도 다음 시작은 증가하도록 하라.

### 2) 로비 시스템
- 기본 월드 또는 설정된 로비 월드를 로비로 사용
- 모든 런이 끝나거나 실패하면 플레이어를 로비 스폰으로 이동
- 로비에서는 하코런 전용 HUD만 표시하고, 런 월드 데이터와 분리
- `/hakorun start` 명령으로 새 런 시작
- `/hakorun reset` 명령으로 현재 런 강제 실패 및 정리
- `/hakorun lobby` 명령으로 로비 이동

### 3) 월드 생성 및 차원 연결
런 시작 시 아래 3개 월드를 현재 런 전용으로 생성:
- hakorun_<지구번호>_world
- hakorun_<지구번호>_nether
- hakorun_<지구번호>_end

요구사항:
- 각 지구는 독립 시드 사용 가능
- 설정에서 랜덤 시드/고정 시드 선택 가능
- 네더 포털, 엔드 진입이 현재 런의 차원 세트에만 연결되도록 처리
- 이전 지구 월드와 현재 지구 월드가 섞이지 않도록 보장
- 런 종료 후 이전 월드는 설정에 따라 삭제 또는 보관

### 4) 공유 생명 시스템
기본값:
- sharedLives: 3

플레이어가 사망하면:
- sharedLivesRemaining 감소
- 즉시 리스폰 대신 로비 또는 관전자 상태로 이동
- 설정에 따라 두 모드를 지원:
  - TEAM_SHARED_LIVES: 팀 공유 목숨 소진 시 런 실패
  - INSTANT_WIPE: 누가 죽든 즉시 런 실패

추가 옵션:
- deathMessageMode:
  - VANILLA
  - HIDDEN
  - REVEAL_ON_WIPE

HIDDEN 모드에서는 즉시 전체 채팅에 죽은 사람/사망 원인을 띄우지 말 것.
REVEAL_ON_WIPE 모드에서는 런이 완전히 실패했을 때 누적 사망 로그를 한 번에 공개할 것.

### 5) HUD / UI
플레이어에게 아래 정보를 표시하라.

스코어보드:
- 현재 상태 (LOBBY / RUNNING / FAILED / WON)
- 현재 지구
- 남은 목숨
- 총 실패 횟수
- 총 승리 횟수
- 생존 시간
- 현재 생존 중 플레이어 수

액션바 또는 보스바:
- 런 타이머 (HH:MM:SS)

옵션:
- `/hakorun ui` 로 개인 HUD on/off 가능

### 6) 승리 판정
승리 조건:
- 엔더드래곤이 사망했고
- 플레이어가 엔드 포털을 통과했을 때

승리 시:
- 상태를 WON으로 변경
- totalWins 증가
- 모든 플레이어에게 타이틀/사운드/브로드캐스트 출력
- 잠시 후 로비 이동
- 다음 지구를 준비할 수 있도록 정리

### 7) 실패 판정
실패 조건:
- 공유 목숨 0
- 또는 INSTANT_WIPE 모드에서 첫 사망 발생
- 또는 `/hakorun reset`

실패 시:
- 상태를 FAILED로 변경
- totalResets 증가
- deathMessageMode가 REVEAL_ON_WIPE면 저장된 사망 기록 공개
- 모든 플레이어를 로비로 이동
- 현재 런 월드 종료 처리
- 다음 지구 번호 증가

### 8) 데이터 저장
영속 저장이 필요하다.
최소 저장 대상:
- currentAttemptIndex
- totalWins
- totalResets
- currentMode
- config values
- lobby world / lobby spawn
- current run metadata

YAML 또는 JSON 기반으로 구현해도 좋지만, 코드 구조는 나중에 SQLite로 교체하기 쉽게 설계하라.

### 9) 명령어
다음 명령을 구현하라:
- `/hakorun start`
- `/hakorun reset`
- `/hakorun status`
- `/hakorun lobby`
- `/hakorun ui`
- `/hakorun mode <TEAM_SHARED_LIVES|INSTANT_WIPE>`
- `/hakorun lives <number>`
- `/hakorun setlobby`
- `/hakorun revealdeaths`
- `/hakorun cleanup`

권한도 plugin.yml에 명시하라:
- hakorun.admin
- hakorun.player

### 10) 이벤트/확장 포인트
방송형 확장 룰을 위해 훅을 제공하라.
최소한 아래 이벤트에서 콘솔 커맨드를 실행할 수 있게 하라:
- onRunStart
- onPlayerDeathInRun
- onRunFail
- onRunWin

예:
- 설정파일에 hook command 템플릿을 넣고
- `%player%`, `%attempt%`, `%lives%`, `%reason%` 같은 플레이스홀더 지원

예시:
- death 시 외부 플러그인 명령 호출
- fail 시 벌칙 명령 호출
- win 시 보상 명령 호출

## 비기능 요구사항
- 클래스 분리를 깔끔하게 하라. 모든 로직을 메인 클래스 하나에 몰아넣지 말 것.
- 추천 구조:
  - HakoRunPlugin
  - RunManager
  - WorldManager
  - LifeManager
  - HudManager
  - CommandHandler
  - ConfigManager
  - HookManager
  - model 패키지 (RunState, RunMode, DeathMessageMode, RunSession 등)
- null-safe하고 예외 처리 포함
- 서버 재시작 후 상태 복구 가능
- 불필요한 `/reload` 의존 금지
- 로그 메시지는 읽기 쉽게 prefix 포함
- 메인 스레드에서 위험한 I/O를 남발하지 말 것
- 월드 정리 실패 시 안전하게 로그를 남기고 서버가 죽지 않게 할 것

## 생성해야 하는 출력물
반드시 아래를 모두 출력하라:
1. 전체 프로젝트 트리
2. pom.xml
3. plugin.yml
4. config.yml
5. 각 Java 클래스의 전체 코드
6. 핵심 설계 설명
7. 빌드 방법
8. 설치 방법
9. 테스트 시나리오

## config.yml 기본값 예시
아래 성격을 반영하라:
- lobbyWorld: world
- sharedLives: 3
- mode: TEAM_SHARED_LIVES
- deathMessageMode: REVEAL_ON_WIPE
- deleteOldRunWorlds: true
- runStartCountdownSeconds: 5
- useBossBarTimer: false
- useActionBarTimer: true
- hooks:
    onRunStart: []
    onPlayerDeathInRun: []
    onRunFail: []
    onRunWin: []

## 테스트 시나리오
최소 다음 시나리오를 검증하라:
1. 1지구 시작 후 사망 1회 -> 남은 목숨 감소
2. 남은 목숨 0 -> 실패 처리, 로비 이동, 2지구 준비
3. 2지구 시작 후 엔더드래곤 클리어 -> 승리 카운트 증가
4. REVEAL_ON_WIPE 모드에서 실패 시 사망 로그 공개
5. UI 토글 시 해당 플레이어만 HUD 비표시
6. 서버 재시작 후 totalWins / totalResets / currentAttemptIndex 유지
7. 네더/엔드 이동이 현재 지구 월드 세트에만 연결됨

## 출력 형식
- 먼저 프로젝트 트리
- 그 다음 파일별 코드 블록
- 마지막에 설치/빌드/테스트 방법
- 축약하지 말고, placeholder code도 넣지 말고, 실제 동작 가능한 수준으로 작성하라

## 추가 핵심 요구사항 (매우 중요)

### A. 생명 수의 실시간 운영 변경
플러그인은 인게임 명령어를 통해 생명 수 정책을 실시간 변경할 수 있어야 한다.
설정파일 편집 + 리로드 방식이 아니라, 런 중에도 즉시 반영되어야 한다.

반드시 아래를 지원하라:
- shared life pool 방식
- per-player life 방식
- 현재 런에 대한 즉시 반영
- HUD/scoreboard/actionbar 즉시 갱신
- 변경 후 fail 조건 재평가

반드시 구현할 명령:
- `/hakorun lives mode <SHARED_POOL|PER_PLAYER>`
- `/hakorun lives shared set <n>`
- `/hakorun lives shared add <delta>`
- `/hakorun lives player set <player> <n>`
- `/hakorun lives player add <delta>`
- `/hakorun lives sync-default <n>`

구현 요구:
- LifePolicy enum 분리
- sharedLivesRemaining 과 playerLivesRemaining 을 동시에 모델링
- 명령 실행 시 메모리 상태 변경 -> 영속 저장 -> HUD 갱신 -> 상태 재평가 순서로 처리
- 공유 목숨이 0 이하가 되면 즉시 실패 처리
- 플레이어 개별 목숨 모드에서는 해당 플레이어만 탈락 처리할 수 있도록 설계

### B. 월드 준비 중에도 플레이어 연결 유지
새 런 월드가 준비되는 동안 플레이어가 서버에서 연결 해제되면 안 된다.
서버 재시작, /reload, 킥, 강제 재접속을 사용하면 안 된다.

이를 위해 상태머신에 아래 상태를 추가하라:
- PREPARING_NEXT_RUN
- READY_TO_TRANSFER

구현 요구:
- 런 종료 후 플레이어는 모두 로비 월드에 안전하게 유지
- 다음 런용 월드 생성이 완료되기 전까지 플레이어를 새 월드로 보내지 말 것
- 월드 준비 실패 시에도 플레이어는 로비에 남아 있어야 하며 연결이 유지되어야 함
- 월드 준비 완료 후 카운트다운을 거쳐 일괄 이동
- 중복 `/hakorun start` 방지
- 월드 생성/전환 작업을 단일 operation queue 로 직렬화

### C. 시드가 실제로 바뀌는 것을 보장하는 설계
이 플러그인은 “월드 재생성 후에도 같은 시드처럼 보이는 문제”를 반드시 방지해야 한다.

절대 지켜야 할 규칙:
1. 같은 월드 이름을 재사용하지 말 것
2. 각 런은 고유한 runId 를 가진다
3. 월드 이름은 runId 기반으로 고유해야 한다
4. 시드는 월드 생성 직전에 랜덤 생성하지 말고, RunSession 생성 시 확정 후 영속 저장할 것
5. createWorld 직전 로그에 runId 와 seed 를 반드시 출력할 것
6. 이전 월드 삭제 실패 여부를 무시하지 말 것
7. 이전 월드를 지우고 같은 이름으로 다시 만드는 방식을 금지할 것

반드시 world naming 규칙을 이렇게 사용하라:
- hakorun_<runId>_world
- hakorun_<runId>_nether
- hakorun_<runId>_end

RunSession 에 최소 아래 필드를 포함하라:
- attemptIndex
- runId
- overworldSeed
- netherSeed
- endSeed
- createdAt
- state

월드 생성 시 반드시:
- WorldCreator.name(uniqueWorldName)
- seed(persistedSeed)
를 사용하고,
생성 전에 “이미 같은 이름의 월드가 로드되어 있는지” 검사하라.

### D. 안전한 월드 전환
월드 unload/delete/create 는 무조건 안전 큐에서 처리하고,
플레이어가 남아 있는 월드는 unload 하지 말 것.

구현 요구:
- WorldOperationQueue 클래스 추가
- WorldTransitionService 클래스 추가
- unload -> delete -> create -> initialize -> ready 순서
- 실패 시 서버를 멈추지 말고 로비 유지 + 에러 로그 + 관리자 알림
- 월드 전환 중에는 현재 상태를 `/hakorun status` 에 표시

### E. 구현 품질 요구
placeholder code 금지.
특히 월드 삭제/생성/시드/런 세션 영속화 부분은 실제 동작 가능한 수준으로 구현하라.
명령어 실행 예시와 테스트 시나리오를 반드시 포함하라.

추가 테스트 시나리오:
1. 런 도중 `/hakorun lives shared set 5` 실행 시 즉시 HUD 반영
2. 런 도중 `/hakorun lives player set <player> 1` 실행 시 해당 플레이어 라이프만 변경
3. 런 종료 후 다음 런 준비 중에도 플레이어가 로비에 남아 있음
4. 연속 3회 런 생성 시 매번 world name 과 seed 가 모두 다름
5. 이전 월드 삭제 실패 상황에서도 새 runId 월드 생성이 가능
6. 서버 재시작 후 마지막 runId, attemptIndex, seed 정보가 유지됨
7. 같은 이름 재사용으로 인해 createWorld 가 기존 월드를 반환하는 문제가 구조적으로 발생하지 않음

월드 리셋을 “기존 월드 폴더 삭제 후 같은 이름 재생성”으로 구현하지 말고,
반드시 “새 runId 기반의 새 월드 생성 후 이전 월드는 저장해놓되 사용하지 않는” 방식으로 구현하라.