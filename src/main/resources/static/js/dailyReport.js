/* ======================================================
 * 센서 라벨 정의
 ====================================================== */
const sensorLabels = [
  "실내온도","상대습도","이산화탄소","VOC","PM1.0","PM2.5","PM10",
  "온도1","온도2","온도3","소음","TNC","조도",
  "센서14","센서15","센서16","센서17","센서18","센서19","센서20"
];

// 센서별 단위(필요 없으면 "" 로)
const sensorUnits = [
  "℃","%","ppm","ppb","㎍/㎥","㎍/㎥","㎍/㎥",
  "℃","℃","℃","dB","count","lx",
  "","", "", "", "", "", ""
];


const grid = document.getElementById("sensorGrid");

/* ======================================================
 * 전역 상태
 ====================================================== */
let currentDevice = "ENV_V2_1";
let selectedIndex = -1;

let virtualTime = null;     // Date 객체
let pollingTimer = null;
let clockTimer = null;

/* ======================================================
 * INIT
 ====================================================== */
window.addEventListener("DOMContentLoaded", () => {
  const dateInput   = document.getElementById("reportDate");
  const timeInput   = document.getElementById("reportTime");
  const deviceSelect = document.getElementById("deviceSelect");

  // ✅ 1. 초기값 세팅
  if (dateInput) dateInput.value = "2025-02-01";
  if (timeInput) timeInput.value = "00:00";

  // ✅ 2. 디바이스
  if (window.deviceCode) currentDevice = window.deviceCode;
  if (deviceSelect) deviceSelect.value = currentDevice;

  // ✅ 3. 그리드 생성
  initGridIfNeeded();

  // ✅ 4. 센서 시뮬레이션 시작
  startSimulation();

  // ✅ 5. 🔥 날씨 최초 로딩 (이게 빠져 있었음)
  loadWeather();

  // ✅ 6. 날짜 변경 시 날씨 갱신
  dateInput?.addEventListener("change", loadWeather);
});

/* ======================================================
 * 조회 버튼
 ====================================================== */
const loadBtn = document.getElementById("loadBtn");
if (loadBtn) {
  loadBtn.addEventListener("click", () => {
    startSimulation();
    loadWeather();
  });
}

/* ======================================================
 * Date 생성 (🔥 핵심 개선)
 ====================================================== */
function createVirtualTime(dateStr, timeStr) {
  const [y, m, d] = dateStr.split("-").map(Number);
  const parts = timeStr.split(":").map(Number);

  const hh = parts[0] ?? 0;
  const mm = parts[1] ?? 0;
  const ss = parts[2] ?? 0;

  return new Date(y, m - 1, d, hh, mm, ss);
}

/* ======================================================
 * 시뮬레이션 시작
 ====================================================== */
function startSimulation() {
  const date = document.getElementById("reportDate").value;
  const time = document.getElementById("reportTime").value;
  currentDevice = document.getElementById("deviceSelect").value;

  virtualTime = createVirtualTime(date, time);

  if (isNaN(virtualTime.getTime())) {
    alert("시간 형식이 올바르지 않습니다.");
    virtualTime = null;
    return;
  }

  startVirtualClock();
  startPolling();
}

/* ======================================================
 * 가상 시계
 ====================================================== */
function startVirtualClock() {
  if (clockTimer) clearInterval(clockTimer);

  updateClockUI();

  clockTimer = setInterval(() => {
    if (!virtualTime || isNaN(virtualTime.getTime())) return;
    virtualTime.setSeconds(virtualTime.getSeconds() + 1);
    updateClockUI();
  }, 1000);
}

function updateClockUI() {
  const el = document.getElementById("virtualTime");
  if (el && virtualTime && !isNaN(virtualTime.getTime())) {
    el.innerText = virtualTime.toLocaleTimeString();
  }
}

/* ======================================================
 * Polling
 ====================================================== */
function startPolling() {
  if (!virtualTime || isNaN(virtualTime.getTime())) return;

  if (pollingTimer) clearInterval(pollingTimer);

  pollingTimer = setInterval(async () => {
    if (!virtualTime || isNaN(virtualTime.getTime())) return;
    const data = await fetchDailySensorData();
    renderSensorGrid(data);
  }, 1000);
}

function formatLocalDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function formatLocalTime(date) {
  const h = String(date.getHours()).padStart(2, "0");
  const m = String(date.getMinutes()).padStart(2, "0");
  const s = String(date.getSeconds()).padStart(2, "0");
  return `${h}:${m}:${s}`;
}

async function fetchDailySensorData() {
  if (!virtualTime || isNaN(virtualTime.getTime())) return [];

  const dateStr = formatLocalDate(virtualTime);
  const timeStr = formatLocalTime(virtualTime);

  const url =
    `${ctx}api/report/daily?date=${dateStr}&time=${timeStr}&device=${currentDevice}`;

  return fetch(url).then(r => r.json());
}

/* ======================================================
 * GRID 초기 생성
 ====================================================== */
function initGridIfNeeded() {
  if (grid.children.length > 0) return;

  for (let i = 0; i < 20; i++) {
    const card = document.createElement("div");
    card.className = "sensor-card EMPTY";
    card.dataset.index = i;
    card.dataset.status = "EMPTY";

    card.innerHTML = `
      <div class="left">
        <div class="icon"><i class="fa-regular fa-face-meh"></i></div>
        <div class="label">${sensorLabels[i]}</div>
      </div>
      <div class="right">
        <div class="value">N/A</div>
      </div>
    `;

    card.addEventListener("click", () => openSensorModal(i));
    grid.appendChild(card);
  }
}

/* ======================================================
 * 센서 카드 렌더링
 ====================================================== */
function renderSensorGrid(result) {
  if (!Array.isArray(result)) return;

  result.forEach((item, i) => {
    const card = grid.children[i];
    if (!card) return;

    const newStatus = (item?.status ?? "EMPTY").toUpperCase();
    const isActive =
      item?.active === "Y" || item?.active === true || item?.active === "1";

    if (!item || newStatus === "EMPTY" || !isActive || item.value == null) {
      card.classList.remove("GOOD", "BAD");
      card.classList.add("EMPTY");
      card.dataset.status = "EMPTY";
      card.querySelector(".value").innerText = "N/A";
      card.querySelector(".icon").innerHTML =
        `<i class="fa-regular fa-face-meh"></i>`;
      return;
    }

    if (card.dataset.status !== newStatus) {
      card.classList.remove("GOOD", "BAD", "EMPTY");
      card.classList.add(newStatus);
      card.classList.add("status-changed");
      setTimeout(() => card.classList.remove("status-changed"), 200);
    }
    card.dataset.status = newStatus;

    const unit = sensorUnits[i] ?? "";
    card.querySelector(".value").innerText =
      `${item.value}${unit ? " " + unit : ""}`;

    card.querySelector(".icon").innerHTML =
      newStatus === "GOOD"
        ? `<i class="fa-regular fa-face-smile"></i>`
        : `<i class="fa-regular fa-face-frown"></i>`;
  });
}


 /* ======================================================
 * 모달 열기
 ====================================================== */
async function openSensorModal(idx) {
  selectedIndex = idx;

  if (pollingTimer) clearInterval(pollingTimer);
  if (clockTimer) clearInterval(clockTimer);

  const res = await fetch(
    `${ctx}api/threshold/get?device=${currentDevice}&index=${idx}`
  );
  const t = await res.json();

  document.getElementById("modalSensorName").innerText = sensorLabels[idx];
  document.getElementById("thresholdMin").value = t.minValue;
  document.getElementById("thresholdMax").value = t.maxValue;
  document.getElementById("sensorActive").value = t.active;

  document.getElementById("sensorModal").classList.add("active");
}

/* ======================================================
 * 모달 저장
 ====================================================== */
const saveBtn = document.getElementById("btnSaveThreshold");
if (saveBtn) {
  saveBtn.addEventListener("click", async () => {
    const dto = {
      deviceCode: currentDevice,
      sensorType: sensorTypeFromIndex(selectedIndex),
      minValue: Number(document.getElementById("thresholdMin").value),
      maxValue: Number(document.getElementById("thresholdMax").value),
      active: document.getElementById("sensorActive").value
    };

    await fetch(`${ctx}api/threshold/save`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dto)
    });

    document.getElementById("sensorModal").classList.remove("active");

    if (virtualTime && !isNaN(virtualTime.getTime())) {
      startVirtualClock();
      startPolling();
    }
  });
}

/* ======================================================
 * 센서 타입 매핑
 ====================================================== */
function sensorTypeFromIndex(i) {
  const map = [
    "TEMP","HUMIDITY","CO2","VOC","PM1","PM25","PM10",
    "T1","T2","T3","NOISE","TNC","LUX",
    "VAL14","VAL15","VAL16","VAL17","VAL18","VAL19","VAL20"
  ];
  return map[i] || null;
}

/* ======================================================
 * 날씨 api
 ====================================================== */
async function loadWeather() {
    const date = document.getElementById("reportDate").value;
    const areaNo = 108; // 기본: 서울 (지역별 선택 UI 추가 가능)

    const res = await fetch(`${ctx}api/report/weather/daily`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ date, areaNo })
    });

    const data = await res.json();
    renderWeather(data);
}

function renderWeather(list) {

    if (!list || list.length === 0 || list.error) {
        document.getElementById("weatherBox").innerHTML = `
            <div class="weather-error">${list?.error || "날씨 데이터가 없습니다."}</div>
        `;
        return;
    }

    // ★ 선택한 날짜 값
    const date = document.getElementById("reportDate").value;
    const target = date.replace(/-/g, ""); // YYYYMMDD

    // ★ 해당 날짜 날씨 찾기 (없으면 첫 날 표시)
    const latest = list.find(item => item.tm === target) || list[0];

    document.getElementById("weatherBox").innerHTML = `
        <div class="weather-header">
            <i class="fa-solid fa-cloud-sun weather-icon"></i>
            <div>
                <div class="weather-title">서울(종로) 날씨</div>
            </div>
        </div>

        <div class="weather-grid">

            <div class="weather-item big">
                <div class="label">현재 기온</div>
                <div class="value">${latest.temp}℃</div>
            </div>

            <div class="weather-item">
                <div class="label">최저 / 최고</div>
                <div class="value">${latest.tempMin}℃ / ${latest.tempMax}℃</div>
            </div>

            <div class="weather-item">
                <div class="label">습도</div>
                <div class="value">${latest.humidity}%</div>
            </div>

            <div class="weather-item">
                <div class="label">강수량</div>
                <div class="value">${latest.rain} mm</div>
            </div>

            <div class="weather-item">
                <div class="label">풍속</div>
                <div class="value">${latest.wind} m/s</div>
            </div>

        </div>
    `;
}
