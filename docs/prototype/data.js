// CaseLab AI — Mock data
// 데모데이 전야 살인사건 (공식 시나리오)

window.CASE = {
  id: 'CL-001',
  code: 'CASE NO. 24-1107',
  title: '데모데이 전야 살인사건',
  subtitle: '모노로그랩스 사건',
  difficulty: 'HARD',
  diffLabel: '어려움',
  estMin: 35,
  suspectsN: 5,
  evidenceN: 12,
  rating: 4.8,
  plays: 12480,
  tags: ['스타트업', '독살', '알리바이 트릭', '시간 조작'],
  date: '2024.11.07',
  briefing: `AI 스타트업 ‘모노로그랩스’의 대표 강도현(38)이 데모데이 전날 밤, 본사 6층 데모룸에서 사망한 채 발견되었다.

처음에는 알레르기 쇼크에 의한 사고사로 처리될 뻔했으나, 현장에는 찢긴 컵 라벨, 사라진 에피펜, 그리고 사망 추정 시각 이후에 전송된 단톡방 메시지가 남아 있었다.

당신은 사건을 의뢰받은 외주 조사관이다. 데모데이 출범까지 남은 시간은 12시간. 그 안에 진범을 가려내라.`,
  victim: {
    name: '강도현',
    age: 38,
    role: '모노로그랩스 대표이사 / Founder',
    cause: '아나필락시스 쇼크 (사인 추정)',
    foundAt: '23:37 / 6층 데모룸',
    allergy: '견과류 (트리넛) 중증 알레르기 — 본인 휴대 에피펜 1정',
  },
  goals: [
    '범인을 밝혀내세요',
    '범행 동기를 설명하세요',
    '범행 방법(독살 트릭)을 추론하세요',
    '결정적 증거 3개를 제시하세요',
  ],
};

window.SUSPECTS = [
  {
    id: 's1', name: '박재민', age: 41, role: 'CFO / 재무 총괄',
    rel: '대표와 공동창업 5년차. 자금 운용 일임받음.',
    statement: '저는 데모데이 자료 마무리 때문에 재무팀 사무실에 22시 30분까지 있었습니다. 대표님과는 21시에 마지막으로 통화했고요.',
    alibi: '재무팀 사무실 — 데모데이 IR 자료 작업',
    suspicion: 72,
    initial: '박',
    accent: '#d4a437',
    relations: '공동창업자 / 자금 운용',
  },
  {
    id: 's2', name: '이준호', age: 35, role: 'CTO / 기술 총괄',
    rel: '대표와는 대학 동문. 최근 지분 협상 중.',
    statement: '서버 안정화 때문에 데모룸 옆 회의실에서 22시 40분까지 있었습니다. 알리바이는 깃 로그로 확인 가능합니다.',
    alibi: '6층 회의실 — 서버 점검 / Git 푸시 기록 존재',
    suspicion: 45,
    initial: '이',
    accent: '#4a9eff',
    relations: '대학 동문 / 지분 협상',
  },
  {
    id: 's3', name: '서유라', age: 33, role: '마케팅 리드',
    rel: '대표와 비공개로 연인 관계였다는 소문.',
    statement: '저는 22시쯤 1층 카페에서 음료를 사 들고 사무실로 올라왔어요. 그 후엔 제 자리에 있었습니다.',
    alibi: '1층 카페 → 5층 마케팅실',
    suspicion: 38,
    initial: '서',
    accent: '#d44a4a',
    relations: '루머 / 연인 관계 의혹',
  },
  {
    id: 's4', name: '김나은', age: 24, role: '재무팀 인턴',
    rel: '박재민 직속 인턴. 입사 3개월차.',
    statement: '박재민 팀장님 심부름으로 22시 5분에 1층 카페에 다녀왔습니다. 그 후엔 자리에 있었어요.',
    alibi: '1층 카페 심부름 → 재무팀',
    suspicion: 28,
    initial: '김',
    accent: '#5fb37c',
    relations: '박재민 직속',
  },
  {
    id: 's5', name: '오세훈', age: 47, role: '벤처캐피탈 심사역',
    rel: '내일 데모데이의 리드 투자자.',
    statement: '저는 외부 미팅을 마치고 22시 50분에 6층에 도착해서 대표님을 기다리고 있었습니다.',
    alibi: '외부 미팅 → 22:50 6층 도착',
    suspicion: 22,
    initial: '오',
    accent: '#9c7ad4',
    relations: '리드 투자자',
  },
];

window.EVIDENCE = [
  // 초기 공개
  { id: 'e1', name: '찢긴 컵 라벨', loc: '데모룸 쓰레기통', related: ['s1','s4'], importance: 'HIGH', initial: true,
    desc: '카페 일회용 컵에 붙어 있던 ‘아몬드라떼’ 라벨이 의도적으로 뜯겨 쓰레기통에 버려져 있었다. 컵 본체는 데모룸 책상 위에 그대로 남아 있다.',
    icon: '✂' },
  { id: 'e2', name: '단톡방 메시지', loc: '피해자 휴대폰', related: ['s1','s2','s3'], importance: 'MID', initial: true,
    desc: '“데모 슬라이드 최종본 올렸음 다들 확인 부탁” — 23:12 전송. 피해자가 보낸 것으로 표시되어 있다.',
    icon: '💬' },
  { id: 'e3', name: '피해자 알레르기 기록', loc: '인사 파일', related: [], importance: 'HIGH', initial: true,
    desc: '강도현 대표는 트리넛(아몬드, 캐슈넛 등) 중증 알레르기 보유자. 본인용 에피펜을 항상 책상 서랍에 비치.',
    icon: '⚠' },
  { id: 'e4', name: '카페 영수증', loc: '데모룸 책상', related: ['s4'], importance: 'MID', initial: true,
    desc: '22:05 ‘리프트커피 강남2호점’에서 결제된 영수증. 결제자 정보는 영수증만으로는 식별 불가.',
    icon: '🧾' },
  { id: 'e5', name: '피해자 발견 현장 사진', loc: '데모룸', related: [], importance: 'MID', initial: true,
    desc: '책상 위에 음료 컵 2개. 한 컵은 거의 비어 있고, 다른 컵은 절반 정도 남아 있다. 에피펜은 서랍에 없다.',
    icon: '📷' },
  // 시간 해금
  { id: 'e6', name: '카페 결제자 정보', loc: '리프트커피 POS', related: ['s1'], importance: 'HIGH', unlockMin: 10,
    desc: '22:05 결제 — 결제자: 박재민(법인카드 9월 등록). 주문: 오트라떼 1, 아몬드라떼 1.',
    icon: '💳' },
  { id: 'e7', name: '출입 로그', loc: '본사 6층', related: ['s1','s2','s5'], importance: 'MID', unlockMin: 12,
    desc: '22:15 박재민 입실, 22:33 박재민 퇴실, 22:50 오세훈 입실, 22:55 박재민 재입실(데모룸).',
    icon: '🚪' },
  { id: 'e8', name: '휴대폰 위치 기록', loc: '피해자 단말', related: [], importance: 'HIGH', unlockMin: 15,
    desc: '강도현의 휴대폰은 22:27 이후 데모룸 한 지점에서 움직이지 않음. 그러나 23:12 메시지는 정상 전송됨.',
    icon: '📍' },
  { id: 'e9', name: '회계 파일 (수정본)', loc: '대표 노트북', related: ['s1'], importance: 'HIGH', unlockMin: 20,
    desc: '22:55에 USB로 접근한 흔적. 운영자금 약 4.2억의 사용처가 불분명한 “기타 지출” 항목으로 잡혀 있다.',
    icon: '📊' },
  { id: 'e10', name: '에피펜 (사용 흔적 없음)', loc: '재무팀 사무실 캐비닛', related: ['s1'], importance: 'CRITICAL', unlockMin: 25,
    desc: '피해자의 에피펜이 사건 현장이 아닌 재무팀 사무실 캐비닛 안쪽에서 발견됨. 사용된 흔적은 없다.',
    icon: '💉' },
  { id: 'e11', name: 'CCTV 사각 동선', loc: '6층 복도', related: ['s1'], importance: 'MID', unlockMin: 18,
    desc: '6층 복도 CCTV에는 사각지대가 있다. 박재민은 22:33~22:55 사이 사각지대를 이용해 재무팀 사무실에 갔다 온 것으로 추정.',
    icon: '📹' },
  { id: 'e12', name: '협박 이메일 (페이크)', loc: '대표 이메일함', related: ['s3'], importance: 'LOW',
    desc: '“만나서 이야기하자” — 익명 발신자로부터 온 메일. 발신자는 서유라의 개인 메일과 무관함이 추후 확인됨.',
    icon: '✉', fake: true, initial: true },
];

window.TIMELINE = [
  { t: '22:05', label: '카페 결제 (리프트커피)', source: '영수증', conflict: false, evidId: 'e4' },
  { t: '22:15', label: '박재민 6층 입실', source: '출입 로그', conflict: false, evidId: 'e7' },
  { t: '22:16', label: '데모룸 음료 컵 배치', source: '현장 사진 추정', conflict: false, evidId: 'e5' },
  { t: '22:27', label: '피해자 휴대폰 정지 (사망 추정)', source: '위치 기록', conflict: false, evidId: 'e8' },
  { t: '22:33', label: '박재민 6층 퇴실', source: '출입 로그', conflict: true, evidId: 'e7', conflictNote: '본인 진술(22:30까지 재무팀)과 일치하지 않음' },
  { t: '22:36', label: '피해자 계정 메시지 전송', source: '단톡방', conflict: true, evidId: 'e2', conflictNote: '휴대폰은 22:27 이후 정지 상태였음' },
  { t: '22:55', label: '박재민 데모룸 재입실 / 노트북 USB 접속', source: '출입 + 회계 파일', conflict: false, evidId: 'e9' },
  { t: '23:12', label: '두 번째 단톡방 메시지 — 슬라이드 최종본', source: '단톡방', conflict: true, evidId: 'e2', conflictNote: '피해자 사망 추정 이후 발신' },
  { t: '23:37', label: '시신 발견 (보안팀)', source: '신고 기록', conflict: false },
];

window.HINTS = [
  { level: 1, content: '피해자가 마신 음료가 무엇이었고, 알레르기 정보와 어떤 관계가 있는지부터 다시 살펴보세요.', cost: 5 },
  { level: 2, content: '사망 추정 시각과 단톡방 메시지 전송 시각, 그리고 휴대폰 위치 기록 사이의 모순을 연결해 보세요.', cost: 15 },
  { level: 3, content: '에피펜이 “현장”이 아닌 “재무팀 사무실”에서 발견된 것은 우연이 아닙니다. 누가 22:33~22:55 사이에 두 장소를 오갈 수 있었을까요?', cost: 30 },
];

window.SOLUTION = {
  culprit: 's1',
  motive: '회사 운영자금 약 4.2억 횡령 사실이 데모데이 자료에 드러날 위기였음. 데모데이 전에 입막음이 필요했다.',
  method: '심부름 인턴(김나은) 명의로 카페에서 아몬드라떼를 구매, 피해자에게 “오트라떼”라고 속여 건넴. 견과류 알레르기 발작 유도 후 에피펜을 미리 빼돌려 사용 불가하게 만들었다.',
  cover: '피해자 휴대폰으로 단톡방 메시지를 두 차례 발신해 사망 시각을 23:12 이후로 조작하고, 그 사이 USB로 회계 파일을 ‘기타 지출’로 수정. 본인의 6층 퇴실/재입실 동선은 복도 CCTV 사각지대를 이용했다.',
  keyEvids: ['e6', 'e10', 'e9'],
};

// ── 심문 응답 ────────────────────────────────────────────────
// state-aware 응답: 공개된 evidence id 집합에 따라 다른 톤
window.INTERROGATION = {
  s1: { // 박재민 (범인)
    suggested: [
      '사건 당시 어디에 계셨습니까?',
      '피해자와 마지막으로 본 것은 언제입니까?',
      '회계상 ‘기타 지출’ 항목은 무엇입니까?',
      '에피펜이 왜 재무팀 사무실에서 발견됐습니까?',
    ],
    base: {
      '사건 당시 어디에 계셨습니까?': '재무팀 사무실에 있었습니다. 22시 30분까지 자료를 정리했고요.',
      '피해자와 마지막으로 본 것은 언제입니까?': '저녁 9시쯤 통화한 게 마지막입니다. 본 건 아니었어요.',
      '카페에서 무엇을 샀습니까?': '저는 그날 카페에 간 적 없습니다. 인턴이 다녀왔어요.',
      '회계상 ‘기타 지출’ 항목은 무엇입니까?': '운영비 일부입니다. 자세한 건 자료를 봐야 답할 수 있겠는데요.',
      '에피펜이 왜 재무팀 사무실에서 발견됐습니까?': '그건 저도 모릅니다. 누가 일부러 둔 거 아닙니까?',
    },
    onEvidence: {
      'e6': '… 제가 산 건 맞습니다. 인턴은 들고만 왔고요. 그런데 그건 제가 마시려고 산 거지, 대표님께 드린 건 아닙니다.',
      'e7': '잠깐 답답해서 바람 쐬러 나갔던 겁니다. 22시 55분에 다시 들어간 건 노트북을 두고 와서요.',
      'e9': '… 회계 작업은 제가 한 거 맞습니다. 하지만 그건 결산 정리고, 횡령 같은 거 아닙니다.',
      'e10': '(잠시 침묵) … 그게 거기 있을 리가 없죠. 누가 옮긴 겁니다.',
      'e8': '메시지는 대표님이 직접 보낸 거 아닙니까? 저는 모르는 일입니다.',
    },
    deny: '그런 일 없습니다. 증거 있으면 보여주시죠.',
  },
  s2: { // CTO 이준호 (무고)
    suggested: ['사건 당시 어디에 계셨습니까?', '대표와 지분 협상에서 갈등이 있었습니까?', '서버 점검은 정말 그 시간이었습니까?'],
    base: {
      '사건 당시 어디에 계셨습니까?': '6층 회의실에서 서버 점검 중이었습니다. Git 푸시 로그 확인하시면 됩니다.',
      '대표와 지분 협상에서 갈등이 있었습니까?': '의견 차이는 있었지만 협상은 진행 중이었어요. 그게 살인 동기가 되진 않습니다.',
      '서버 점검은 정말 그 시간이었습니까?': '22시 40분까지 작업했습니다. 22:38에 마지막 커밋이 찍혀 있고요.',
    },
    onEvidence: {},
    deny: '저는 관련 없습니다.',
  },
  s3: { // 마케팅 리드 서유라 (무고하지만 비밀 있음)
    suggested: ['대표와 연인 관계였습니까?', '협박 메일은 무엇입니까?', '22시 이후 어디 계셨습니까?'],
    base: {
      '대표와 연인 관계였습니까?': '… 그 질문엔 답하고 싶지 않습니다. 사건과는 무관해요.',
      '협박 메일은 무엇입니까?': '제가 보낸 게 아닙니다. 제 메일과 도메인도 다릅니다.',
      '22시 이후 어디 계셨습니까?': '22시쯤 1층 카페에서 음료 사고, 5층 자기 자리에 있었어요.',
    },
    onEvidence: {},
    deny: '저는 모릅니다.',
  },
  s4: { // 인턴 김나은 (무고)
    suggested: ['카페에서 무엇을 샀습니까?', '왜 두 잔을 샀습니까?', '두 잔 모두 직접 골랐습니까?'],
    base: {
      '카페에서 무엇을 샀습니까?': '팀장님 심부름으로 갔어요. 오트라떼랑 아몬드라떼 한 잔씩이요.',
      '왜 두 잔을 샀습니까?': '팀장님이 한 잔은 대표님 갖다드리라고 하셔서요.',
      '두 잔 모두 직접 골랐습니까?': '아뇨, 메뉴는 팀장님이 미리 정해주셨어요.',
    },
    onEvidence: {},
    deny: '저는 시키신 대로만 했어요.',
  },
  s5: { // 투자자 오세훈 (무고)
    suggested: ['언제 6층에 도착했습니까?', '대표와의 약속은 몇 시였습니까?'],
    base: {
      '언제 6층에 도착했습니까?': '22시 50분쯤이었습니다. 출입 로그에 찍혀 있을 거예요.',
      '대표와의 약속은 몇 시였습니까?': '23시에 데모 리허설을 같이 보기로 했었습니다.',
    },
    onEvidence: {},
    deny: '저는 외부에 있었습니다.',
  },
};

window.OTHER_SCENARIOS = [
  { id: 'cl-002', code: 'CL-002', title: '심야 도서관의 사라진 원고', tags: ['실종','학원'], diff: '보통', diffEn:'NORMAL', min: 25, sus: 4, ev: 9, rating: 4.6, plays: 8420, type: 'OFFICIAL' },
  { id: 'cl-003', code: 'CL-003', title: '동아리 회비가 사라진 날', tags: ['절도','동아리'], diff: '쉬움', diffEn:'EASY', min: 18, sus: 3, ev: 7, rating: 4.3, plays: 14210, type: 'OFFICIAL' },
  { id: 'cu-014', code: 'CU-014', title: '카페 마감 후, 점장이 사라졌다', tags: ['커스텀','심야'], diff: '보통', diffEn:'NORMAL', min: 30, sus: 5, ev: 10, rating: 4.5, plays: 5602, type: 'CUSTOM', author: '@nightowl' },
  { id: 'cu-021', code: 'CU-021', title: '연구실 화재의 진짜 원인', tags: ['커스텀','대학원'], diff: '어려움', diffEn:'HARD', min: 40, sus: 6, ev: 13, rating: 4.7, plays: 3208, type: 'CUSTOM', author: '@lab_42' },
  { id: 'cu-033', code: 'CU-033', title: '동창회 호텔에서 일어난 일', tags: ['커스텀','호텔'], diff: '보통', diffEn:'NORMAL', min: 28, sus: 5, ev: 11, rating: 4.4, plays: 4108, type: 'CUSTOM', author: '@reunion88' },
];

window.LOCATIONS = [
  { id: 'l1', name: '데모룸', floor: '6F', tag: '발견 장소', mark: '★', evids: ['e1','e5'] },
  { id: 'l2', name: '재무팀 사무실', floor: '4F', tag: '에피펜 발견', mark: '◉', evids: ['e10'] },
  { id: 'l3', name: '6층 회의실', floor: '6F', tag: 'CTO 알리바이', mark: '○', evids: [] },
  { id: 'l4', name: '6층 복도', floor: '6F', tag: 'CCTV 사각', mark: '◐', evids: ['e11'] },
  { id: 'l5', name: '1F 리프트커피', floor: '1F', tag: '독 음료 구매', mark: '◍', evids: ['e4','e6'] },
];

window.MY_RECORDS = [
  { case: 'CL-001', title: '데모데이 전야 살인사건', state: 'IN_PROGRESS', progress: 62, grade: '-', t: '진행 중' },
  { case: 'CL-002', title: '심야 도서관의 사라진 원고', state: 'DONE', progress: 100, grade: 'A', t: '2일 전' },
  { case: 'CL-003', title: '동아리 회비가 사라진 날', state: 'DONE', progress: 100, grade: 'S', t: '5일 전' },
  { case: 'CU-014', title: '카페 마감 후, 점장이 사라졌다', state: 'ABANDONED', progress: 28, grade: '-', t: '1주 전' },
];
