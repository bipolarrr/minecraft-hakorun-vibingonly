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
| 정책 | 설명 |
|------|------|
| `SHARED_POOL` | 공유 목숨 풀에서 차감 |
| `PER_PLAYER` | 플레이어별 개인 목숨 차감 |

### 사망 메시지 모드
- `REVEAL_ON_WIPE` — 런 실패 시점에 이번 런의 사망 기록 전체 공개
- 즉시 공개 모드도 config에서 설정 가능

### HUD
- 액션바 타이머 및 보스바로 현재 목숨 / 경과 시간 실시간 표시
- 플레이어별 HUD 토글 지원

### 훅(Shell Hooks)
런 이벤트(`onRunStart`, `onPlayerDeathInRun`, `onRunFail`, `onRunWin`)에 셸 명령어를 연결해 외부 시스템과 연동할 수 있습니다.

### 데이터 영속성
런 세션(시드, 목숨, 사망 로그, 지구 번호 등)을 파일에 저장하며, 서버 재시작 후에도 누적 승리·실패 기록을 유지합니다.

---

## Commands

별칭: `/hr`
관리자 권한: `hakorun.admin` (기본값: OP)

| 명령어 | 권한 | 설명 |
|--------|------|------|
| `/hakorun start` | admin | 새 런 시작 (월드 생성 → 카운트다운 → 이동) |
| `/hakorun reset` | admin | 현재 런 강제 실패 처리 |
| `/hakorun status` | 모두 | 현재 상태, 지구 번호, 목숨, 생존 시간 등 확인 |
| `/hakorun lobby` | 모두 | 로비 스폰으로 텔레포트 |
| `/hakorun setlobby` | admin | 현재 위치를 로비 스폰으로 저장 |
| `/hakorun ui` | 모두 | 자신의 HUD(보스바/액션바) 토글 |
| `/hakorun mode <MODE>` | admin | 런 모드 변경 (`TEAM_SHARED_LIVES` \| `INSTANT_WIPE`) |
| `/hakorun revealdeaths` | admin | 현재 런의 사망 기록 즉시 공개 |
| `/hakorun cleanup` | admin | 현재 세션의 이전 런 월드 수동 정리 |

### 목숨 관리 서브커맨드

```
/hakorun lives mode <SHARED_POOL|PER_PLAYER>     목숨 정책 변경
/hakorun lives shared set <n>                    공유 목숨 절대값 설정
/hakorun lives shared add <delta>                공유 목숨 증감
/hakorun lives player set <player> <n>           특정 플레이어 목숨 설정
/hakorun lives player add <player> <delta>       특정 플레이어 목숨 증감
/hakorun lives sync-default <n>                  기본 목숨 일괄 변경
```

---

## Configuration

`config.yml` 주요 항목:

```yaml
sharedLives: 3                    # 런 시작 시 기본 목숨
mode: TEAM_SHARED_LIVES           # 런 모드
lifePolicy: SHARED_POOL           # 목숨 차감 정책
deathMessageMode: REVEAL_ON_WIPE  # 사망 메시지 공개 시점
runStartCountdownSeconds: 5       # 월드 준비 후 출발 카운트다운(초)
deleteOldRunWorlds: true          # 런 종료 후 이전 런 월드 자동 삭제
seed:
  random: true                    # false 시 fixedSeed 사용
  fixedSeed: 0
hooks:
  onRunStart: []                  # 런 시작 시 실행할 셸 명령어 목록
  onPlayerDeathInRun: []
  onRunFail: []
  onRunWin: []
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
