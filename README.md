# ✨ 협업 규칙

**반드시 develop 브랜치로 PR하기**

## 🚀 워크플로우
1. **이슈 생성** 및 **담당자(Assignee) 지정**
2. **브랜치 생성** (ex. feat/#1)
3. **해당 브랜치로 이동**해 **작업 후 커밋** (ex. [feat] 로그인 API 구현)
4. 작업 완료 후 **PR 생성** (ex. [feat] 로그인 API 구현)
5. **코드 리뷰** 후 승인 및 merge
6. **브랜치 삭제** 및 **이슈 닫기(close)**

&nbsp;

## 📝 커밋 규칙
`git commit -m "[type] 커밋 내용 작성"`

### 예시
```
git commit -m "[feat] 로그인 API 구현"
git commit -m "[fix] 사용자 정보 불러오기 오류 수정"
```

### 타입 설명
| **타입** | **의미** |
| --- | --- |
| `feat` | 새로운 기능 추가 (feature) |
| `fix` | 버그 수정 (bug fix) |
| `chore` | 코드 변경이 아닌 잡일, 설정 변경 등 |
| `build` | 의존성 추가, gradle 관련 변경 등 |
| `style` | 코드 포맷, 세미콜론, 공백 등 스타일 관련 수정 |
| `refactor` | 기능 변경 없이 코드 구조 개선 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 추가/수정 |
| `ci` | CI/CD 관련 설정 변경 |

&nbsp;

## 🌿브랜치 규칙
`git checkout -b {type}/#issue`

### 예시
```
git checkout -b feat/#1
```
