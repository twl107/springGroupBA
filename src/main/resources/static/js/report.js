/* ============================================================
   Report.js - INDUSTRIAL REFACTOR (2025-12-03)
   - CSV FULL LOAD 버튼
   - 월별/주별 테이블 & 그래프
   - 주차별 탭
   - 정렬 기능 업그레이드 (data-col-type 기반)
   - 이전 달 리포트 호출로 월별/주별 등락폭 계산
   - eventCount(이벤트 구간 수) 표시 + 클릭 시 이벤트 로그 화면 오픈
   - BAD%(badRate) 컬럼 추가 (백엔드에서 값 제공 시)
   - Tooltip (data-tooltip 기반 말풍선)
   - BAD / EVENT 개념 Modal 중앙 팝업
============================================================ */

let monthlyChart = null;
let weeklyChart = null;

// 컨텍스트 패스
const ctx = window.ctx || "/";

/* ============================================================
   0) DOM 로딩 후 초기 세팅
============================================================ */
window.addEventListener("DOMContentLoaded", () => {
  const yearSel    = document.getElementById("yearSelect");
  const monthSel   = document.getElementById("monthSelect");
  const deviceSel  = document.getElementById("deviceSelect");
  const loadBtn    = document.getElementById("loadReportBtn");
  const loadCsvBtn = document.getElementById("loadCsvBtn");

  const runReport = () => {
    loadReport(yearSel.value, monthSel.value, deviceSel.value);
  };

  if (loadBtn) {
    loadBtn.addEventListener("click", runReport);
  }

  // CSV FULL LOAD 실행 버튼
  if (loadCsvBtn) {
    loadCsvBtn.addEventListener("click", async () => {
      if (!confirm("CSV 원본 데이터를 sensor_raw 테이블에 다시 로드할까요?\n(기존 sensor_raw 데이터는 모두 대체됩니다)")) {
        return;
      }

      try {
        const res = await fetch(`${ctx}api/report/load-csv`, {
          method: "POST"
        });

        if (!res.ok) {
          alert("❌ CSV 로드 실패!");
          return;
        }

        alert("✅ CSV 로드 완료!\n이제 리포트를 조회할 수 있습니다.");
      } catch (e) {
        console.error(e);
        alert("❌ CSV 로드 중 오류가 발생했습니다.");
      }
    });
  }

  // BAD / EVENT 안내 모달 초기화
  initBadEventModal();

  // 첫 화면 로딩 시 바로 현재 선택값으로 리포트 조회
  runReport();
});

/* ============================================================
   1) 리포트 API 호출 (현재월 + 이전월)
============================================================ */
async function loadReport(year, month, device) {

  showLoading();

  try {
    const y = Number(year);
    const m = Number(month);

    // 1) 현재 월 데이터
    const currentUrl = `${ctx}api/report/${y}/${m}?device=${device}`;
    const res = await fetch(currentUrl);
    if (!res.ok) throw new Error("HTTP " + res.status);

    const data = await res.json(); // { year, month, deviceCode, monthlyStats, weeklyStats }

    // 2) 이전 월 데이터 (1월이면 없음)
    let prevMonthlyStats = null;
    let prevWeeklyStats = null;

    if (m > 1) {
      const prevMonth = m - 1;
      const prevUrl = `${ctx}api/report/${y}/${prevMonth}?device=${device}&checkOnly=true`;
      try {
        const prevRes = await fetch(prevUrl);
        if (prevRes.ok) {
          const prevData = await prevRes.json();
          prevMonthlyStats = prevData.monthlyStats || null;
          prevWeeklyStats = prevData.weeklyStats || null;
        } else {
          console.warn("⚠ 이전 달 리포트 호출 실패:", prevRes.status);
        }
      } catch (e) {
        console.warn("⚠ 이전 달 리포트 통신 오류:", e);
      }
    }

    // 화면 렌더링
    renderHeader(data);
    renderMonthlyTable(data, prevMonthlyStats);
    renderWeeklySummary(data, prevWeeklyStats);
    renderMonthlyChart(data.monthlyStats, data);

  } catch (e) {
    console.error("⛔ Report API 실패:", e);
    alert("리포트 데이터를 불러오지 못했습니다.");
  } finally {
    hideLoading();
  }
}

/* ============================================================
   2) 헤더
============================================================ */
function renderHeader(data) {
  const subtitle = document.getElementById("reportSubtitle");
  if (!subtitle) return;
  subtitle.textContent =
      `데이터 기간: ${data.year}년 ${data.month}월 (${data.deviceCode})`;
}

/* ============================================================
   3) BAD% 렌더링 헬퍼 (산업용 모니터링 스타일)
   - 백엔드에서 s.badRate(%)를 제공하면 사용
   - 없으면 "-" 표시
============================================================ */
function buildBadRateCell(badRate) {
  if (badRate == null || isNaN(badRate)) {
    return `
      <td class="bad-rate-cell" data-sort-value="-1">
        <span class="bad-rate-empty has-tooltip"
              data-tooltip="BAD 비율 데이터가 없습니다.">
          -
        </span>
      </td>
    `;
  }

  const v = Number(badRate);
  let cls = "bad-good";
  if (v >= 30) {
    cls = "bad-bad";
  } else if (v >= 10) {
    cls = "bad-warn";
  }

  const tooltip = "BAD% = 임계값을 벗어난 샘플 비율입니다. (GOOD: 0~10%, WARN: 10~30%, BAD: 30% 이상)";

  return `
    <td class="bad-rate-cell" data-sort-value="${v.toFixed(1)}">
      <span class="bad-rate-badge ${cls} has-tooltip"
            data-tooltip="${tooltip}">
        ${v.toFixed(1)}%
      </span>
    </td>
  `;
}

/* ============================================================
   4) 월별 테이블
============================================================ */
function renderMonthlyTable(reportData, prevList) {
  const tbody = document.getElementById("monthlyStatsBody");
  const table = document.getElementById("monthlyTable");
  if (!tbody || !table) return;

  tbody.innerHTML = "";

  const currList = reportData.monthlyStats;

  if (!currList || currList.length === 0) {
    tbody.innerHTML =
        `<tr><td colspan="8" class="text-center text-muted">데이터 없음</td></tr>`;
    return;
  }

  currList.forEach((s, idx) => {
    // 이전 달에서 같은 센서(label)를 찾아서 매칭
    let prev = null;
    if (prevList && prevList.length > 0) {
      prev = prevList.find(p => p.label === s.label) || null;
    }

    const diffMin = prev ? calcDiffPercent(prev.minValue, s.minValue) : "";
    const diffAvg = prev ? calcDiffPercent(prev.avgValue, s.avgValue) : "";
    const diffMax = prev ? calcDiffPercent(prev.maxValue, s.maxValue) : "";

    const safeEventCount = s.eventCount != null ? s.eventCount : 0;
    const badRate = s.badRate; // 백엔드에서 제공하면 BAD% 계산, 아니면 "-"

    const rowHtml = `
      <tr>
        <td data-sort-value="${idx + 1}">${idx + 1}</td>
        <td>${s.label}</td>

        <td data-sort-value="${s.minValue}">
          ${s.minValue.toFixed(2)}
          ${diffMin}
        </td>

        <td data-sort-value="${s.avgValue}">
          ${s.avgValue.toFixed(2)}
          ${diffAvg}
        </td>

        <td data-sort-value="${s.maxValue}">
          ${s.maxValue.toFixed(2)}
          ${diffMax}
        </td>

        ${buildBadRateCell(badRate)}

        <!-- Event 구간 수 (클릭 시 로그 페이지) -->
        <td class="event-cell has-tooltip"
            data-sort-value="${safeEventCount}"
            data-sensor-type="${s.sensorType}"
            data-tooltip="클릭하면 이벤트 로그를 새 창에서 엽니다."
            onclick="openEventLogView('MONTH', ${reportData.year}, ${reportData.month}, '${reportData.deviceCode}', '${s.sensorType}', null)">
          ${safeEventCount}
        </td>

        <td data-sort-value="${s.sampleCount}">
          ${s.sampleCount}
        </td>
      </tr>
    `;

    tbody.insertAdjacentHTML("beforeend", rowHtml);
  });

  makeTableSortable(table);
}

/* ============================================================
   5) 주차별 탭 + 그래프 + 테이블
============================================================ */
function renderWeeklySummary(reportData, prevWeeklyStats) {
  const weeklyStats = reportData.weeklyStats;
  const tabs = document.getElementById("weekTabs");
  const tableWrapper = document.getElementById("weeklyTableWrapper");
  const canvas = document.getElementById("weeklyChart");

  if (!tabs || !tableWrapper || !canvas) return;

  tabs.innerHTML = "";
  tableWrapper.innerHTML = "";

  if (!weeklyStats || Object.keys(weeklyStats).length === 0) {
    tableWrapper.innerHTML = `<p class="text-muted">주간 데이터 없음</p>`;
    if (weeklyChart) weeklyChart.destroy();
    return;
  }

  const weeks = Object.keys(weeklyStats)
      .map(Number)
      .sort((a, b) => a - b);

  // 탭 생성
  weeks.forEach((week, index) => {
    const tab = document.createElement("div");
    tab.className = "week-tab";
    if (index === 0) tab.classList.add("active");
    tab.textContent = `${week}주차`;
    tab.dataset.week = String(week);

    tab.addEventListener("click", () => {
      document.querySelectorAll(".week-tab").forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      renderWeeklyTable(weeklyStats, week, prevWeeklyStats, weeks, reportData);
      renderWeeklyChart(weeklyStats, week, canvas);
    });

    tabs.appendChild(tab);
  });

  // 첫 주차 자동 렌더링
  const firstWeek = weeks[0];
  renderWeeklyTable(weeklyStats, firstWeek, prevWeeklyStats, weeks, reportData);
  renderWeeklyChart(weeklyStats, firstWeek, canvas);
}

/**
 * 주차별 테이블
 */
function renderWeeklyTable(weeklyStats, week, prevWeeklyStats, weekList, reportData) {
  const tableWrapper = document.getElementById("weeklyTableWrapper");
  if (!tableWrapper) return;

  const rows = weeklyStats[week];
  if (!rows || rows.length === 0) {
    tableWrapper.innerHTML = `<p class="text-muted">${week}주차 데이터 없음</p>`;
    return;
  }

  // === 이전 주 데이터 결정 로직 ===
  let prevRows = null;

  if (Array.isArray(weekList)) {
    const wNum = Number(week);
    const idx = weekList.indexOf(wNum);

    if (idx > 0) {
      // 같은 달 내에서 이전 주차
      const prevWeek = weekList[idx - 1];
      prevRows = weeklyStats[prevWeek] || null;
    } else if (idx === 0 && prevWeeklyStats) {
      // 이번 달의 첫 주차 → 이전 달의 마지막 주차 사용
      const prevWeeks = Object.keys(prevWeeklyStats)
          .map(Number)
          .sort((a, b) => a - b);
      if (prevWeeks.length > 0) {
        const prevLastWeek = prevWeeks[prevWeeks.length - 1];
        prevRows = prevWeeklyStats[prevLastWeek] || null;
      }
    }
  }

  let html = `
    <table class="report-table" id="weeklyTable">
      <thead>
        <tr>
          <th data-col-key="label" data-col-type="text">센서</th>
          <th data-col-key="min" data-col-type="number">최솟값</th>
          <th data-col-key="avg" data-col-type="number">평균값</th>
          <th data-col-key="max" data-col-type="number">최댓값</th>
          <th data-col-key="badRate" data-col-type="number">BAD%</th>
          <th data-col-key="event" data-col-type="number">Event</th>
          <th data-col-key="sample" data-col-type="number">샘플 수</th>
        </tr>
      </thead>
      <tbody>
  `;

  rows.forEach(s => {
    let diffMin = "";
    let diffAvg = "";
    let diffMax = "";

    if (prevRows && prevRows.length > 0) {
      const prevSensor = prevRows.find(p => p.label === s.label);
      if (prevSensor) {
        diffMin = calcDiffPercent(prevSensor.minValue, s.minValue);
        diffAvg = calcDiffPercent(prevSensor.avgValue, s.avgValue);
        diffMax = calcDiffPercent(prevSensor.maxValue, s.maxValue);
      }
    }

    const safeEventCount = s.eventCount != null ? s.eventCount : 0;
    const badRate = s.badRate;

    html += `
      <tr>
        <td>${s.label}</td>

        <td data-sort-value="${s.minValue}">
          ${s.minValue.toFixed(2)}
          ${diffMin}
        </td>

        <td data-sort-value="${s.avgValue}">
          ${s.avgValue.toFixed(2)}
          ${diffAvg}
        </td>

        <td data-sort-value="${s.maxValue}">
          ${s.maxValue.toFixed(2)}
          ${diffMax}
        </td>

        ${buildBadRateCell(badRate)}

        <!-- Event 구간 수 (클릭 시 로그 페이지) -->
        <td class="event-cell has-tooltip"
            data-sort-value="${safeEventCount}"
            data-sensor-type="${s.sensorType}"
            data-tooltip="클릭하면 이벤트 로그를 새 창에서 엽니다."
            onclick="openEventLogView('WEEK', ${reportData.year}, ${reportData.month}, '${reportData.deviceCode}', '${s.sensorType}', ${week})">
          ${safeEventCount}
        </td>

        <td data-sort-value="${s.sampleCount}">
          ${s.sampleCount}
        </td>
      </tr>
    `;
  });

  html += `</tbody></table>`;
  tableWrapper.innerHTML = html;

  const table = document.getElementById("weeklyTable");
  makeTableSortable(table);
}

/* ============================================================
   6) 주차별 그래프
============================================================ */
function renderWeeklyChart(weeklyStats, week, canvas) {
  const rows = weeklyStats[week];
  if (!rows || !canvas) return;

  const labels = rows.map(r => r.label);
  const avgValues = rows.map(r => r.avgValue);
  const eventCounts = rows.map(r => r.eventCount != null ? r.eventCount : 0);

  if (weeklyChart) weeklyChart.destroy();

  weeklyChart = new Chart(canvas, {
    type: "bar",
    data: {
      labels,
      datasets: [
        {
          label: `${week}주차 평균`,
          data: avgValues,
          borderWidth: 1,
          yAxisID: "y"
        },
        {
          type: "line",
          label: "이벤트 수",
          data: eventCounts,
          borderWidth: 2,
          tension: 0.3,
          pointRadius: 3,
          yAxisID: "y1"
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: { beginAtZero: true },
        y1: {
          beginAtZero: true,
          position: "right",
          grid: { drawOnChartArea: false }
        }
      }
    }
  });
}

/* ============================================================
   7) 월별 그래프 (평균 + 이벤트 카운트 라인)
============================================================ */
function renderMonthlyChart(list, info) {
  const canvas = document.getElementById("monthlyChart");
  const caption = document.getElementById("chartCaption");

  if (!canvas) return;

  if (!list || list.length === 0) {
    if (caption) caption.textContent = "데이터 없음";
    if (monthlyChart) monthlyChart.destroy();
    return;
  }

  const labels = list.map(s => s.label);
  const avgValues = list.map(s => s.avgValue);
  const eventCounts = list.map(s => s.eventCount != null ? s.eventCount : 0);

  if (monthlyChart) monthlyChart.destroy();

  monthlyChart = new Chart(canvas, {
    type: "bar",
    data: {
      labels,
      datasets: [
        {
          label: `${info.year}년 ${info.month}월 평균값`,
          data: avgValues,
          borderWidth: 1,
          yAxisID: "y"
        },
        {
          type: "line",
          label: "이벤트 수",
          data: eventCounts,
          borderWidth: 2,
          tension: 0.3,
          pointRadius: 3,
          yAxisID: "y1"
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: { beginAtZero: true },
        y1: {
          beginAtZero: true,
          position: "right",
          grid: { drawOnChartArea: false }
        }
      }
    }
  });

  if (caption) {
    caption.textContent = `${info.deviceCode} 센서별 평균값 + 이벤트 수`;
  }
}

/* ============================================================
   8) 공통: 테이블 정렬 기능
   - thead th에 data-col-type="number"|"text"
   - 각 td에 data-sort-value가 있으면 우선 사용
============================================================ */
function makeTableSortable(table) {
  if (!table) return;

  const thead = table.querySelector("thead");
  const tbody = table.querySelector("tbody");
  if (!thead || !tbody) return;

  const headers = thead.querySelectorAll("th");

  headers.forEach((th, index) => {
    th.style.cursor = "pointer";

    th.addEventListener("click", () => {
      const currentOrder = th.classList.contains("sort-asc") ? "asc"
          : th.classList.contains("sort-desc") ? "desc"
          : null;

      headers.forEach(h => h.classList.remove("sort-asc", "sort-desc"));

      const newOrder = currentOrder === "asc" ? "desc" : "asc";
      th.classList.add(newOrder === "asc" ? "sort-asc" : "sort-desc");

      const rows = Array.from(tbody.querySelectorAll("tr"));
      const colType = th.dataset.colType || "text";
      const isNumericCol = colType === "number";

      rows.sort((a, b) => {
        const aCell = a.children[index];
        const bCell = b.children[index];

        const aRaw = aCell ? (aCell.getAttribute("data-sort-value") ?? aCell.textContent.trim()) : "";
        const bRaw = bCell ? (bCell.getAttribute("data-sort-value") ?? bCell.textContent.trim()) : "";

        if (isNumericCol) {
          const aVal = parseFloat(String(aRaw).replace(/,/g, "")) || 0;
          const bVal = parseFloat(String(bRaw).replace(/,/g, "")) || 0;
          return newOrder === "asc" ? aVal - bVal : bVal - aVal;
        } else {
          const aText = String(aRaw);
          const bText = String(bRaw);
          return newOrder === "asc"
              ? aText.localeCompare(bText)
              : bText.localeCompare(aText);
        }
      });

      rows.forEach(r => tbody.appendChild(r));
    });
  });
}

/* ============================================================
   9) 등락폭(증감률) 계산 함수
============================================================ */
function calcDiffPercent(prev, curr) {
  if (prev == null || prev === 0) return "";

  const diff = ((curr - prev) / prev) * 100;
  const sign = diff >= 0 ? "+" : "";
  const isUp = diff >= 0;
  const color = isUp ? "#e74c3c" : "#2ecc71"; // 상승: 빨강, 하락: 초록 (산업용 느낌)

  return ` <span class="diff-badge" style="color:${color}; font-size:12px;">(${sign}${diff.toFixed(1)}%)</span>`;
}

/* ============================================================
   10) 이벤트 로그 화면 오픈
============================================================ */
function openEventLogView(scope, year, month, deviceCode, sensorType, week) {
  const params = new URLSearchParams({
    scope,
    year: String(year),
    month: String(month),
    device: deviceCode,
    sensorType
  });

  if (week != null) {
    params.append("week", String(week));
  }

  const url = `${ctx}sensor/event-log?${params.toString()}`;
  window.open(url, "_blank");
}

/* ============================================================
   11) BAD / EVENT 안내 모달
============================================================ */
function initBadEventModal() {
  const btn = document.getElementById("badEventInfoBtn");
  const modal = document.getElementById("badEventModal");
  const closeBtn = document.getElementById("badEventModalClose");
  const okBtn = document.getElementById("badEventModalOk");

  if (!modal || !btn) return;

  const open = () => {
    modal.classList.add("open");
  };

  const close = () => {
    modal.classList.remove("open");
  };

  btn.addEventListener("click", open);

  if (closeBtn) {
    closeBtn.addEventListener("click", close);
  }
  if (okBtn) {
    okBtn.addEventListener("click", close);
  }

  // ESC 로 닫기
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && modal.classList.contains("open")) {
      close();
    }
  });

  // 바깥 클릭 시 닫기
  modal.addEventListener("click", (e) => {
    if (e.target === modal) {
      close();
    }
  });
}

/* ============================================================
   12) show/hide spinner
============================================================ */
function showLoading() {
  const overlay = document.getElementById("loadingOverlay");
  if (overlay) overlay.style.display = "flex";
}

function hideLoading() {
  const overlay = document.getElementById("loadingOverlay");
  if (overlay) overlay.style.display = "none";
}
