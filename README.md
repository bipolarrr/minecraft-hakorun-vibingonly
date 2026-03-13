# HakoRun

**하코런(하드코어 엔더런)** 콘텐츠 자동 운영을 위한 Paper 플러그인.

런 시작부터 월드 생성, 목숨 관리, 드래곤 처치 감지, 로비 복귀까지 전 과정을 자동으로 처리합니다.

---

## Features

### 런 생명주기 자동화
런을 시작하면 오버월드 / 네더 / 엔드 월드를 각각 독립 시드로 자동 생성하고, 카운트다운 후 전 플레이어를 일괄 이동시킵니다. 엔더드래곤 처치 시 클리어 처리, 목숨 소진 또는 강제 리셋 시 실패 처리 후 자동으로 로비로 복귀합니다.

### 런 모드
| 모드 | 설명 |
|------|------|
| `TEAM_SHARED_LIVES` | 팀 전체가 공유 목숨 풀을 사용 |
| `INSTANT_WIPE` | 누군가 사망하는 즉시 런 실패 |

### 목숨 정책
| 정책 | 표시 이름 | 설명 |
|------|----------|------|
| `SHARED_POOL` | 공유목숨 | 팀 공유 목숨 풀에서 차감 |
| `PER_PLAYER` | 인당 데스 | 플레이어별 개인 목숨에서 차감 |

### 사망 메시지 모드
- `REVEAL_ON_WIPE` — 런 실패 시점에 이번 런의 사망 기록 전체 공개
- 즉시 공개 모드도 config에서 설정 가능

### HUD
- **액션바 (핫바 위)**: 런 진행 중 `♥ 남은목숨  ⏱ 경과시간  ↻ 지구번호` 실시간 표시
- **사이드바 (우측 스코어보드)**: 상태 / 목숨 정책 / 승리·실패 누적 / 생존 인원 표시
  - 사이드바 섹션은 플레이어별로 개별 숨김/표시 가능 (`/hakorun sidebar`)
- **보스바**: config에서 활성화 시 경과 시간 추가 표시
- 플레이어별 전체 HUD 토글 지원 (`/hakorun ui`)

### 훅(Shell Hooks)
런 이벤트(`onRunStart`, `onPlayerDeathInRun`, `onRunFail`, `onRunWin`)에 셸 명령어를 연결해 외부 시스템과 연동할 수 있습니다.

### 데이터 영속성
런 세션(시드, 목숨, 사망 로그, 지구 번호 등)을 파일에 저장하며, 서버 재시작 후에도 누적 승리·실패 기록을 유지합니다.

---

## Commands

별칭: `/hr`
관리자 권한: `hakorun.admin` (기본값: OP)

### 런 관리

| 명령어 | 권한 | 설명 |
|--------|------|------|
| `/hakorun start` | admin | 새 런 시작. 오버월드·네더·엔드 월드를 생성하고 카운트다운 후 전 플레이어를 이동시킵니다. 이미 런이 진행 중이거나 준비 중이면 거부됩니다. |
| `/hakorun reset` | admin | 현재 진행 중인 런을 강제 실패 처리합니다. 3초 후 전 플레이어를 로비로 이동하고 이전 런 월드를 정리합니다. |
| `/hakorun status` | 모두 | 현재 런 상태(상태, 지구 번호, 총 승리/실패, runId, 공유 목숨, 목숨 정책, 생존 시간)를 출력합니다. |
| `/hakorun mode <MODE>` | admin | 런 모드를 변경합니다. 가능한 값: `TEAM_SHARED_LIVES`, `INSTANT_WIPE` |
| `/hakorun cleanup` | admin | 현재 세션의 이전 런 월드를 수동으로 정리합니다. |
| `/hakorun revealdeaths` | admin | 현재 런에서 발생한 사망 기록을 전체 채팅에 즉시 공개합니다. |

### 이동

| 명령어 | 권한 | 설명 |
|--------|------|------|
| `/hakorun lobby` | 모두 | 로비 스폰으로 텔레포트합니다. |
| `/hakorun setlobby` | admin | 현재 위치를 로비 스폰으로 저장합니다. |
| `/hakorun rejoin` | 모두 | 진행 중인 런 월드(오버월드 스폰)로 재입장합니다. 런이 `RUNNING` 상태가 아니거나 월드가 준비되지 않은 경우 거부됩니다. |

### HUD

| 명령어 | 권한 | 설명 |
|--------|------|------|
| `/hakorun ui` | 모두 | 자신의 HUD 전체(사이드바·액션바·보스바)를 켜거나 끕니다. 다른 플레이어에게는 영향을 주지 않습니다. |
| `/hakorun sidebar policy` | 모두 | 사이드바의 **목숨 정책** 섹션을 자신에게만 숨기거나 다시 표시합니다. |
| `/hakorun sidebar alive` | 모두 | 사이드바의 **생존 인원** 섹션을 자신에게만 숨기거나 다시 표시합니다. |

### 목숨 관리

| 명령어 | 권한 | 설명 |
|--------|------|------|
| `/hakorun lives mode <정책>` | admin | 목숨 정책을 변경합니다. `공유목숨` 또는 `인당 데스` (내부값 `SHARED_POOL` / `PER_PLAYER` 도 허용) |
| `/hakorun lives shared set <n>` | admin | 현재 런의 공유 목숨을 `n`으로 설정합니다. 런이 진행 중이어야 합니다. |
| `/hakorun lives shared add <delta>` | admin | 현재 런의 공유 목숨을 `delta`만큼 증감합니다. 음수를 입력하면 차감됩니다. |
| `/hakorun lives player set <플레이어> <n>` | admin | 특정 플레이어의 개인 목숨을 `n`으로 설정합니다. (`인당 데스` 정책에서 유효) |
| `/hakorun lives player add <플레이어> <delta>` | admin | 특정 플레이어의 개인 목숨을 `delta`만큼 증감합니다. |
| `/hakorun lives sync-default <n>` | admin | 기본 목숨 값을 `n`으로 변경합니다. 현재 런 세션의 공유 목숨에도 즉시 반영됩니다. |

> **참고**: 목숨 관련 변경은 즉시 스코어보드에 반영됩니다. 재시작·리로드 없이 실시간 적용됩니다.

---

## Configuration

`config.yml` 주요 항목:

```yaml
sharedLives: 3                    # 런 시작 시 기본 목숨 수
mode: TEAM_SHARED_LIVES           # 런 모드 (TEAM_SHARED_LIVES | INSTANT_WIPE)
lifePolicy: SHARED_POOL           # 목숨 차감 정책 (SHARED_POOL | PER_PLAYER)
deathMessageMode: REVEAL_ON_WIPE  # 사망 메시지 공개 시점 (REVEAL_ON_WIPE | 즉시공개)
runStartCountdownSeconds: 5       # 월드 준비 완료 후 출발 카운트다운(초)
deleteOldRunWorlds: true          # 런 종료 후 이전 런 월드 자동 삭제 여부
useBossBarTimer: false            # 보스바에 경과 시간 표시 여부
useActionBarTimer: true           # 핫바 위 액션바에 목숨·시간·지구 표시 여부
seed:
  random: true                    # false 시 fixedSeed 값 고정 사용
  fixedSeed: 0
hooks:
  onRunStart: []                  # 런 시작 시 실행할 셸 명령어 목록
  onPlayerDeathInRun: []          # 플레이어 사망 시
  onRunFail: []                   # 런 실패 시
  onRunWin: []                    # 런 승리 시
```

---

## Development Workflow

이 프로젝트는 다음 파이프라인으로 개발되었습니다.

```
1. Requirements  ──→  인간 지능으로 요구사항 명세 작성
                       기능 범위, 엣지 케이스, 우선순위를 직접 정의

2. Prompt        ──→  ChatGPT (Thinking High) 로 구현 프롬프트 생성
                       명세를 바탕으로 구체적인 설계 지침과
                       코드 구조 방향을 담은 프롬프트를 출력

3. Coding        ──→  Claude Code (VSCode Extension, Sonnet) 로 구현
                       프롬프트를 입력으로 받아 실제 코드 작성,
                       빌드 검증, 리팩터링까지 에이전트가 수행
```

요구사항 정의는 사람이, 설계 언어화는 ChatGPT가, 실제 구현은 Claude Code가 담당하는 분업 구조로 진행된 바이브 코딩 프로젝트입니다.
