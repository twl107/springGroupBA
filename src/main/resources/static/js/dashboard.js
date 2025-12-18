/* ============================================================
   dashboard.js - FULL STABLE VERSION (2025-12-01 + EVENT UI)
   - 단일 센서 표시
   - 실시간(CSV+WS) & 기간조회(HISTORY) 모두 지원
   - pause/resume/reset
   - 활성 센서 필터링 (DB active)
   - threshold(min/max) 연동
   - 통계 계산 (현재/평균/최대/최소/개수)
   - ⭐ REALTIME 모드 이벤트 시각화 (건수, 토스트, 차트 깜빡임, 플래시, 로그)
============================================================ */

/* ===================== GLOBAL STATE ===================== */

let stompClient = null;
let subscription = null;

const ctx = window.ctx || "/";

let currentDevice = window.currentDevice || "ENV_V2_1";
let selectedSensorType = "TEMP"; // 초기값

let mode = "REALTIME"; // REALTIME | HISTORY
let chartStartDate = null;
let chartEndDate = null;

let autoLoadTimer = null;
let sensorChart = null;

let isReloading = false;
let currentThreshold = null;

const chartValues = []; // 통계 계산에서 사용

// ⭐ REALTIME 이벤트 관련 전역 상태
let realtimeEventCount = 0;


/* ===================== SENSOR META ===================== */

const SENSOR_META = [
  { type: "TEMP", label: "실내온도", unit: "°C", field: "temp" },
  { type: "HUMIDITY", label: "상대습도", unit: "%", field: "humidity" },
  { type: "CO2", label: "이산화탄소", unit: "ppm", field: "co2" },
  { type: "VOC", label: "VOC", unit: "ppb", field: "voc" },

  { type: "PM1", label: "PM1.0", unit: "㎍/m³", field: "pm1" },
  { type: "PM25", label: "PM2.5", unit: "㎍/m³", field: "pm25" },
  { type: "PM10", label: "PM10", unit: "㎍/m³", field: "pm10" },

  { type: "T1", label: "온도1", unit: "°C", field: "temp1" },
  { type: "T2", label: "온도2", unit: "°C", field: "temp2" },
  { type: "T3", label: "온도3", unit: "°C", field: "temp3" },
  { type: "TNC", label: "비접촉온도", unit: "°C", field: "nonContactTemp" },

  { type: "NOISE", label: "소음", unit: "dB", field: "noise" },
  { type: "LUX", label: "조도", unit: "Lux", field: "lux" }
];


/* ===================== UTIL ===================== */

function toDate(ts) {
  if (!ts) return null;
  return new Date(ts.replace(" ", "T"));
}

function getDateTime(id) {
  const el = document.getElementById(id);
  if (!el?.value) return null;

  let v = el.value.replace("T", " ");
  if (v.length === 16) v += ":00";
  return v;
}

function formatTimestamp(ts) {
  if (!ts) return "-";

  const d = new Date(ts.replace(" ", "T"));
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const HH = String(d.getHours()).padStart(2, "0");
  const MM = String(d.getMinutes()).padStart(2, "0");

  return `${yyyy}-${mm}-${dd} ${HH}:${MM}`;
}

function setRealtimeTimestamp(ts) {
  const el = document.getElementById("realtimeTimestamp");
  if (el) el.innerText = formatTimestamp(ts);
}


/* ===================== CHART ===================== */

function initChart() {
  const canvas = document.getElementById("sensorChart").getContext("2d");

  sensorChart = new Chart(canvas, {
    type: "line",
    plugins: [thresholdPlugin],   // ⭐ threshold shading 플러그인
    data: {
      labels: [],
      datasets: [
        {
          label: "값",
          data: [],
          borderColor: "rgba(54,162,235,1)",
          borderWidth: 2,
          tension: 0.25,
          pointRadius: 3,
          pointBackgroundColor: ctx => getPointColor(ctx)
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { display: false }
      },
      scales: {
        x: { ticks: { maxTicksLimit: 20 } },
        y: {
          beginAtZero: false,
          min: null,
          max: null
        }
      }
    }
  });
}

function getPointColor(ctx) {
  const value = ctx.raw;
  if (!currentThreshold) return "rgba(54,162,235,1)";

  const min = currentThreshold.min;
  const max = currentThreshold.max;

  if (min != null && value < min) return "red";
  if (max != null && value > max) return "red";

  return "rgba(54,162,235,1)";
}

function resetChart() {
  if (!sensorChart) return;
  sensorChart.data.labels = [];
  sensorChart.data.datasets[0].data = [];
  sensorChart.update("none");
  chartValues.length = 0;
}

function addPoint(timestamp, value) {
  if (!sensorChart) return;

  const label = timestamp.substring(11, 16);
  sensorChart.data.labels.push(label);
  sensorChart.data.datasets[0].data.push(value);

  if (sensorChart.data.labels.length > 100) { // 100 OVER SHIFT
    sensorChart.data.labels.shift();
    sensorChart.data.datasets[0].data.shift();
  }

  chartValues.push(value);
  updateStats(value);

  // Y축 자동 조정 + 차트 업데이트
  adjustYScale(value);
  sensorChart.update("none");

  // ⭐ REALTIME 모드에서만 이벤트 판단
  if (mode === "REALTIME") {
    handleRealtimeEvent(timestamp, value);
  }
}

function adjustYScale(value) {
  const y = sensorChart.options.scales.y;

  if (currentThreshold) {
    const minT = currentThreshold.min;
    const maxT = currentThreshold.max;

    const minVal = Math.min(value, minT ?? value);
    const maxVal = Math.max(value, maxT ?? value);

    const pad = (maxVal - minVal) * 0.2;

    y.min = minVal - pad;
    y.max = maxVal + pad;
  } else {
    y.min = null;
    y.max = null;
  }
}


/* ===================== THRESHOLD SHADING PLUGIN ===================== */

const thresholdPlugin = {
  id: "thresholdShading",
  beforeDraw(chart) {
    const { ctx, chartArea, scales } = chart;

    if (!chartArea || !currentThreshold) return;

    const min = currentThreshold.min;
    const max = currentThreshold.max;

    if (min == null || max == null) return;

    const yScale = scales.y;

    // threshold y 좌표
    const yMin = yScale.getPixelForValue(min);
    const yMax = yScale.getPixelForValue(max);

    // ✔ 항상 위쪽 = 더 작은 값
    const upper = Math.min(yMin, yMax);
    const lower = Math.max(yMin, yMax);

    const width = chartArea.right - chartArea.left;

    ctx.save();

    /* 🔥 BAD 영역 (upper 위쪽) */
    ctx.fillStyle = "rgba(255, 100, 100, 0.15)";
    ctx.fillRect(
      chartArea.left,
      chartArea.top,
      width,
      upper - chartArea.top
    );

    /* 🔥 GOOD 영역 (upper~lower 사이) */
    ctx.fillStyle = "rgba(100, 200, 100, 0.12)";
    ctx.fillRect(
      chartArea.left,
      upper,
      width,
      lower - upper
    );

    /* 🔥 BAD 영역 (lower 아래쪽) */
    ctx.fillStyle = "rgba(255, 100, 100, 0.15)";
    ctx.fillRect(
      chartArea.left,
      lower,
      width,
      chartArea.bottom - lower
    );

    ctx.restore();
  }
};



/* ===================== STATS ===================== */

function resetStats() {
  ["statCurrent", "statAvg", "statMin", "statMax"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.innerText = "-";
  });
  const cnt = document.getElementById("statCount");
  if (cnt) cnt.innerText = "0";
}

function updateStats(currentValue) {
  if (!chartValues.length) return;

  const sum = chartValues.reduce((a, b) => a + b, 0);
  const avg = sum / chartValues.length;
  const min = Math.min(...chartValues);
  const max = Math.max(...chartValues);

  document.getElementById("statCurrent").innerText = currentValue.toFixed(2);
  document.getElementById("statAvg").innerText = avg.toFixed(2);
  document.getElementById("statMin").innerText = min.toFixed(2);
  document.getElementById("statMax").innerText = max.toFixed(2);
  document.getElementById("statCount").innerText = chartValues.length;
}


/* ===================== REALTIME EVENT VISUALS ===================== */

// 이벤트 카운터 초기화
function resetRealtimeEvents() {
  realtimeEventCount = 0;
  const el = document.getElementById("realtimeEventCount");
  if (el) el.innerText = "0";

  const toastBox = document.getElementById("realtimeToastContainer");
  if (toastBox) toastBox.innerHTML = "";
}

// 이벤트 카운터 증가
function increaseRealtimeEventCount() {
  realtimeEventCount++;
  const el = document.getElementById("realtimeEventCount");
  if (el) el.innerText = String(realtimeEventCount);
}

// 토스트 알림 표시
function showRealtimeToast(sensorLabel, value, level, timestamp) {
  const container = document.getElementById("realtimeToastContainer");
  if (!container) return;

  const div = document.createElement("div");
  div.className = "sd-toast " + (level === "HIGH" ? "sd-toast-high" : "sd-toast-low");

  const timeStr = timestamp ? timestamp.substring(11, 19) : "";
  div.innerText = `${timeStr} [${sensorLabel}] 이상 감지 (${level === "HIGH" ? "상한 초과" : "하한 미만"})`;


  container.appendChild(div);

  // 3초 후 자동 제거
  setTimeout(() => {
    div.remove();
  }, 3000);
}

// 차트 라인 깜빡임
function flashChartLine() {
  if (!sensorChart) return;
  const ds = sensorChart.data.datasets[0];
  const originalWidth = ds.borderWidth;

  ds.borderWidth = 5;
  sensorChart.update("none");

  setTimeout(() => {
    ds.borderWidth = originalWidth;
    sensorChart.update("none");
  }, 400);
}

// 화면 전체 플래시
function flashScreen() {
  const flash = document.getElementById("eventFlash");
  if (!flash) return;
  flash.style.opacity = "1";
  setTimeout(() => {
    flash.style.opacity = "0";
  }, 200);
}

// 센서 카드 흔들기
function shakeSensorCard(type) {
  const el = document.querySelector(`.sd-sensor-item[data-sensor-type="${type}"]`);
  if (!el) return;
  el.classList.add("shake");
  setTimeout(() => el.classList.remove("shake"), 350);
}

// 차트 박스 하이라이트
function blinkChartWrapper() {
  const wrap = document.getElementById("chartWrapper");
  if (!wrap) return;
  wrap.classList.add("event-blink");
  setTimeout(() => wrap.classList.remove("event-blink"), 100);  // 기존 300 데이터 이후 SHIFT -> 100으로 조정
}

// threshold 기준으로 이벤트 판단 (REALTIME 전용)
function handleRealtimeEvent(timestamp, value) {
  if (!currentThreshold) return;

  const min = currentThreshold.min;
  const max = currentThreshold.max;

  let level = null;

  if (min != null && value < min) {
    level = "LOW";
  } else if (max != null && value > max) {
    level = "HIGH";
  }

  if (!level) return;

  // 1) 이벤트 카운트 증가
  increaseRealtimeEventCount();

  // 2) 로그 패널 로그 추가
  pushRealtimeLog(level, selectedSensorType, value, timestamp);

  // 3) 강한 시각 효과
  flashScreen();
  shakeSensorCard(selectedSensorType);
  blinkChartWrapper();

  // 4) 토스트 알림
  const meta = SENSOR_META.find(m => m.type === selectedSensorType);
  const label = meta ? meta.label : selectedSensorType;
  showRealtimeToast(label, value, level, timestamp);

  // 5) 차트 라인 강조
  flashChartLine();
}



/* ===================== SENSOR LIST ===================== */

async function loadActiveSensors(deviceCode) {
  try {
    const res = await fetch(`${ctx}api/dashboard/active-sensors?deviceCode=${deviceCode}`);
    if (!res.ok) {
      console.warn("active-sensors API 오류 → fallback");
      return SENSOR_META;
    }

    const activeList = await res.json(); // ["CO2","TEMP"]
    return SENSOR_META.filter(m => activeList.includes(m.type));

  } catch (e) {
    console.error("active-sensors API error:", e);
    return SENSOR_META;
  }
}

function renderSensorList(list) {
  const el = document.getElementById("sensorList");
  el.innerHTML = "";

  list.forEach(sensor => {
    const item = document.createElement("div");
    item.className = "sd-sensor-item";
    item.innerText = `${sensor.label} (${sensor.type})`;
    item.dataset.sensorType = sensor.type;

    item.onclick = () => selectSensor(sensor.type);

    el.appendChild(item);
  });
}

async function selectSensor(type) {
  selectedSensorType = type;

  document.querySelectorAll(".sd-sensor-item").forEach(el => {
    el.classList.toggle("active", el.dataset.sensorType === type);
  });

  const meta = SENSOR_META.find(m => m.type === type);
  if (meta) {
    document.getElementById("chartTitle").innerText = `${meta.label} (${meta.type})`;
    document.getElementById("statUnit").innerText = meta.unit;
  }

  currentThreshold = await loadThreshold(type);
  console.log("[Loaded threshold]", currentThreshold);

  if (currentThreshold && currentThreshold.min != null && currentThreshold.max != null) {
    const pad = (currentThreshold.max - currentThreshold.min) * 0.2;
    sensorChart.options.scales.y.min = currentThreshold.min - pad;
    sensorChart.options.scales.y.max = currentThreshold.max + pad;
  } else {
    sensorChart.options.scales.y.min = null;
    sensorChart.options.scales.y.max = null;
  }

  sensorChart.update();

  resetChart();
  resetStats();
  resetRealtimeEvents(); // ⭐ 센서 변경 시 이벤트 카운터/토스트 초기화

  reloadByMode();
}


/* ===================== THRESHOLD ===================== */

async function loadThreshold(sensorType) {
  try {
    const res = await fetch(
      `${ctx}api/dashboard/threshold?deviceCode=${currentDevice}&sensorType=${sensorType}`
    );
    if (!res.ok) return { min: null, max: null };

    return await res.json();
  } catch (e) {
    console.error("loadThreshold error:", e);
    return { min: null, max: null };
  }
}


/* ===================== MODE BADGE ===================== */

function updateModeBadge() {
  const badge = document.getElementById("modeBadge");
  const label = document.getElementById("chartPeriodLabel");

  if (mode === "REALTIME") {
    badge.classList.remove("sd-badge-history");
    badge.classList.add("sd-badge-realtime");
    badge.innerText = "실시간 시뮬레이션 모드";

    label.innerText = chartStartDate
      ? `${chartStartDate} 이후 실시간`
      : "최근 실시간 데이터";

  } else {
    badge.classList.remove("sd-badge-realtime");
    badge.classList.add("sd-badge-history");
    badge.innerText = "기간 조회 모드";
    label.innerText = `${chartStartDate} ~ ${chartEndDate}`;
  }
}


/* ===================== FILTER APPLY ===================== */

async function onApplyFilter() {
  chartStartDate = getDateTime("startDateInput");
  chartEndDate = getDateTime("endDateInput");

  if (!chartStartDate) {
    alert("시작 시간을 선택하세요.");
    return;
  }

  mode = chartEndDate ? "HISTORY" : "REALTIME";

  resetChart();
  resetStats();
  resetRealtimeEvents(); // ⭐ 필터 적용 시 REALTIME 이벤트 초기화

  if (mode === "REALTIME") {
    await applyRealtimeStart();
  } else {
    await loadHistoryData();
  }

  updateModeBadge();
}


/* ===================== REALTIME MODE ===================== */

async function applyRealtimeStart() {
  try {
    await fetch(`${ctx}api/sensor/set-start-date`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: `startDate=${encodeURIComponent(chartStartDate)}`
    });

    stopAutoLoad();
    startAutoLoad();

    await fetchRecentDataRealtime();

  } catch (e) {
    console.error("applyRealtimeStart error:", e);
  }
}

async function fetchRecentDataRealtime() {
  try {
    const res = await fetch(
      `${ctx}api/sensor/recent?deviceCode=${currentDevice}&limit=200`
    );
    if (!res.ok) return;

    const list = await res.json();
    const meta = SENSOR_META.find(m => m.type === selectedSensorType);

    resetChart();
    resetStats();
    resetRealtimeEvents(); // ⭐ 최근 데이터 불러올 때도 초기화

    for (const d of list) {
      if (chartStartDate && toDate(d.timestamp) < toDate(chartStartDate)) continue;
      const v = d[meta.field];
      if (v == null) continue;
      addPoint(d.timestamp, v); // 내부에서 handleRealtimeEvent 호출
    }

    if (list.length > 0) {
      setRealtimeTimestamp(list[list.length - 1].timestamp);
    }

  } catch (e) {
    console.error("fetchRecentDataRealtime error:", e);
  }
}

function startAutoLoad() {
  stopAutoLoad();
  autoLoadTimer = setInterval(async () => {
    try {
      const res = await fetch(`${ctx}api/sensor/load-chunk?size=10`, {  // load-chunk size : 10
        method: "POST"
      });
      if (!res.ok) return;
      const data = await res.json();
      if (data.done) stopAutoLoad();
    } catch (e) {
      console.error("load-chunk error:", e);
    }
  }, 1000);
}

function stopAutoLoad() {
  if (autoLoadTimer) {
    clearInterval(autoLoadTimer);
    autoLoadTimer = null;
  }
}


/* ===================== WEBSOCKET ===================== */

function connectWS() {
  const sock = new SockJS(`${ctx}ws-sensor`);
  stompClient = Stomp.over(sock);

  stompClient.connect({}, () => {
    subscribeDevice(currentDevice);
  });
}

function subscribeDevice(device) {
  if (subscription) {
    subscription.unsubscribe();
    subscription = null;
  }

  subscription = stompClient.subscribe(`/topic/sensor/${device}`, msg => {
    if (mode !== "REALTIME") return;

    const d = JSON.parse(msg.body);
    const meta = SENSOR_META.find(m => m.type === selectedSensorType);

    if (!d.timestamp || d[meta.field] == null) return;
    if (chartStartDate && toDate(d.timestamp) < toDate(chartStartDate)) return;

    const value = d[meta.field];

    setRealtimeTimestamp(d.timestamp);
    addPoint(d.timestamp, value);
  });
}


/* ===================== HISTORY MODE ===================== */

async function loadHistoryData() {
  try {
    stopAutoLoad();

    showHistoryModeNotice();

    const url =
      `${ctx}api/sensor/history?deviceCode=${currentDevice}` +
      `&sensorType=${selectedSensorType}` +
      `&start=${encodeURIComponent(chartStartDate)}` +
      `&end=${encodeURIComponent(chartEndDate)}`;

    const res = await fetch(url);
    if (!res.ok) return;

    const list = await res.json();

    resetChart();
    resetStats();
    // HISTORY 모드는 지금은 이벤트 시각화 X (원하면 나중에 별도 설계)

    list.forEach(row => {
      addPoint(row.timestamp, row.value);
    });

  } catch (e) {
    console.error("loadHistoryData error:", e);
  }
}

function showHistoryModeNotice() {
  const box = document.getElementById("historyNotice");
  if (box) {
    box.style.display = "block";
  }
}

function hideHistoryModeNotice() {
  const box = document.getElementById("historyNotice");
  if (box) {
    box.style.display = "none";
  }
}


/* ===================== SIM CONTROLS ===================== */

async function pauseSim() {
  try {
    stopAutoLoad();
    await fetch(`${ctx}api/sensor/pause`, { method: "POST" });
    alert("시뮬레이터 중단");
  } catch (e) {
    console.error("pauseSim error:", e);
  }
}

async function resumeSim() {
  try {
    await fetch(`${ctx}api/sensor/resume`, { method: "POST" });
    startAutoLoad();
    alert("시뮬레이터 재개");
  } catch (e) {
    console.error("resumeSim error:", e);
  }
}

async function resetSim() {
  try {
    stopAutoLoad();

    await fetch(`${ctx}api/sensor/pause`, { method: "POST" });
    await fetch(`${ctx}api/sensor/reset`, { method: "POST" });

    resetChart();
    resetStats();
    resetRealtimeEvents(); // ⭐ 초기화 시 이벤트/토스트도 리셋
    setRealtimeTimestamp("-");
    chartStartDate = null;

    const s = document.getElementById("startDateInput");
    const e = document.getElementById("endDateInput");
    if (s) s.value = "";
    if (e) e.value = "";

    await fetch(`${ctx}api/sensor/resume`, { method: "POST" });
    startAutoLoad();

    alert("초기화 완료! (CSV 처음부터 다시 재생)");

  } catch (err) {
    console.error("resetSim error:", err);
  }
}


/* ===================== INITIALIZE ===================== */

window.addEventListener("DOMContentLoaded", async () => {
  document.getElementById("deviceSelect").value = currentDevice;

  document.getElementById("deviceSelect").onchange = async e => {
    currentDevice = e.target.value;
    document.getElementById("chartDeviceLabel").innerText = currentDevice;

    resetChart();
    resetStats();
    resetRealtimeEvents(); // ⭐ 디바이스 변경 시 이벤트도 초기화

    const list = await loadActiveSensors(currentDevice);
    renderSensorList(list);

    if (list.length > 0) {
      await selectSensor(list[0].type);
    }

    reloadByMode();

    if (stompClient?.connected) {
      subscribeDevice(currentDevice);
    }
  };

  document.getElementById("applyFilterBtn").onclick = onApplyFilter;
  document.getElementById("pauseBtn").onclick = pauseSim;
  document.getElementById("resumeBtn").onclick = resumeSim;
  document.getElementById("resetBtn").onclick = resetSim;

  initChart();

  const list = await loadActiveSensors(currentDevice);
  renderSensorList(list);

  if (list.length > 0) {
    await selectSensor(list[0].type);
  }

  connectWS();

  mode = "REALTIME";
  updateModeBadge();
  reloadByMode();
});


/* ===================== MODE LOADER ===================== */

async function reloadByMode() {
  if (isReloading) return;
  isReloading = true;

  try {
    if (mode === "REALTIME") {
      if (!chartStartDate) {
        await fetchRecentDataRealtime();
        startAutoLoad();
      } else {
        await applyRealtimeStart();
      }

    } else {
      // ⭐ HISTORY 모드일 때는 무조건 새로 조회하도록 강제
      if (chartStartDate) {
        await loadHistoryData();
      }
    }

  } finally {
    isReloading = false;
  }
}


/* ========= LOG PANEL CONTROL ========= */

const logPanel = document.getElementById("logPanel");
const logBtn = document.getElementById("toggleLogBtn");
const closeLogBtn = document.getElementById("closeLogBtn");
const logList = document.getElementById("logList");

// 패널 열기
if (logBtn && logPanel) {
  logBtn.onclick = () => {
    logPanel.classList.add("active");
  };
}

// 패널 닫기
if (closeLogBtn && logPanel) {
  closeLogBtn.onclick = () => {
    logPanel.classList.remove("active");
  };
}

/* ========= 로그 추가 함수 ========= */
function pushRealtimeLog(level, sensorType, value, timestamp) {
  if (!logList) return;

  const div = document.createElement("div");
  const time = timestamp.substring(11, 16);

  div.className = "log-item";
  div.innerHTML = `
    <b>[${time}]</b>
    <span class="${level === 'HIGH' ? 'log-high' : 'log-low'}">
      ${sensorType} ${level === "HIGH" ? "상한 초과" : "하한 미만"}
    </span>
    (값=${value})
  `;

  logList.prepend(div);

  // 오래된 로그 삭제 200개 유지
  if (logList.children.length > 200) {
    logList.removeChild(logList.lastChild);
  }
}


/* ========= LEFT PANEL TOGGLE ========= */

const leftPanel = document.getElementById("sdLeft");
const leftToggleBtn = document.getElementById("leftToggleBtn");

if (leftPanel && leftToggleBtn) {
  leftToggleBtn.addEventListener("click", () => {
    leftPanel.classList.toggle("collapsed");
    const isCollapsed = leftPanel.classList.contains("collapsed");

    // 아이콘 방향 변경
    leftToggleBtn.textContent = isCollapsed ? "▶" : "◀";

    // 차트 리사이즈 (좌측 영역 폭 변경 반영)
    if (sensorChart) {
      sensorChart.resize();
    }
  });
}

/* ===================== DASHBOARD CAPTURE ===================== */

function captureDashboard() {
  const target = document.getElementById("dashboardCapture");
  if (!target) {
    alert("캡처 대상이 없습니다.");
    return;
  }

  html2canvas(target, {
    scale: 2,              // 고해상도
    useCORS: true,
    backgroundColor: "#ffffff"
  }).then(canvas => {

    const now = new Date();
    const ts =
      now.getFullYear() +
      String(now.getMonth() + 1).padStart(2, "0") +
      String(now.getDate()).padStart(2, "0") + "_" +
      String(now.getHours()).padStart(2, "0") +
      String(now.getMinutes()).padStart(2, "0");

    const link = document.createElement("a");
    link.download = `dashboard_${currentDevice}_${ts}.png`;
    link.href = canvas.toDataURL("image/png");
    link.click();
  });
}

