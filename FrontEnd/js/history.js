/**
 * Trang Lịch Sử Hoạt Động (Active History) - Kết nối Backend API IoT
 * Xem lịch sử thao tác bật/tắt thiết bị lưu từ database
 */

const state = {
  currentPage: 1, // 1-based cho UI
  pageSize: 10,
  sortField: 'createdAt',
  sortOrder: 'desc',
  filterDevice: 'all',
  filterAction: 'all',
  filterStatus: 'all',
  filterTime: '',
  totalPages: 1,
  totalItems: 0,
  data: [],
  realtimeInterval: null
};

// Ánh xạ data-sort từ HTML sang tên thuộc tính Entity trong Backend Spring Boot
const SORT_FIELD_MAP = {
  'id': 'id',
  'device': 'deviceName',
  'action': 'action',
  'status': 'status',
  'time': 'createdAt'
};

document.addEventListener('DOMContentLoaded', () => {
  checkAuth();
  renderSidebarUser();
  setupLogout();
  injectLiveStyles();
  initControls();
  fetchHistoryData();

  // Tự động làm mới dữ liệu từ server mỗi 5 giây
  state.realtimeInterval = setInterval(() => {
    fetchHistoryData(true);
  }, 5000);
});

window.addEventListener('beforeunload', () => {
  if (state.realtimeInterval) clearInterval(state.realtimeInterval);
});

/**
 * Chèn style và hiển thị huy hiệu trạng thái kết nối Backend
 */
function injectLiveStyles() {
  const style = document.createElement('style');
  style.textContent = `
    .live-indicator {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      margin-left: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      color: #28a745;
    }
    .live-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #28a745;
      animation: livePulse 1.5s ease-in-out infinite;
    }
    @keyframes livePulse {
      0%, 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(40, 167, 69, 0.5); }
      50% { opacity: 0.6; box-shadow: 0 0 0 6px rgba(40, 167, 69, 0); }
    }
    th[data-sort] { cursor: pointer; user-select: none; }
    th[data-sort].sort-asc .sort-icon::after { content: ' ▲'; }
    th[data-sort].sort-desc .sort-icon::after { content: ' ▼'; }
  `;
  document.head.appendChild(style);

  const resultCountEl = document.getElementById('resultCount');
  if (resultCountEl && resultCountEl.parentElement) {
    const liveEl = document.createElement('span');
    liveEl.className = 'live-indicator';
    liveEl.innerHTML = '<span class="live-dot"></span> Đang kết nối Backend';
    resultCountEl.parentElement.appendChild(liveEl);
  }
}

/**
 * Khởi tạo sự kiện cho các bộ lọc, tìm kiếm theo thời gian, chọn số dòng và sắp xếp cột
 */
function initControls() {
  const filterDevice = document.getElementById('filterDevice');
  const filterAction = document.getElementById('filterAction');
  const filterStatus = document.getElementById('filterStatus');
  const filterTime = document.getElementById('filterTime');
  const btnSearch = document.getElementById('btnSearch');
  const pageSizeSelect = document.getElementById('pageSizeSelect');

  function handleFilter() {
    if (filterDevice) state.filterDevice = filterDevice.value;
    if (filterAction) state.filterAction = filterAction.value;
    if (filterStatus) state.filterStatus = filterStatus.value;
    if (filterTime)   state.filterTime   = filterTime.value.trim();
    state.currentPage = 1;
    fetchHistoryData();
  }

  if (btnSearch) btnSearch.addEventListener('click', handleFilter);

  if (filterTime) {
    filterTime.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') handleFilter();
    });
  }

  if (pageSizeSelect) {
    pageSizeSelect.value = state.pageSize;
    pageSizeSelect.addEventListener('change', (e) => {
      state.pageSize = parseInt(e.target.value, 10);
      state.currentPage = 1;
      fetchHistoryData();
    });
  }

  document.querySelectorAll('th[data-sort]').forEach(th => {
    th.addEventListener('click', () => {
      const rawField = th.dataset.sort;
      const backendField = SORT_FIELD_MAP[rawField] || rawField;

      if (state.sortField === backendField) {
        state.sortOrder = state.sortOrder === 'asc' ? 'desc' : 'asc';
      } else {
        state.sortField = backendField;
        state.sortOrder = 'desc';
      }

      updateSortHeaders(rawField, state.sortOrder);
      fetchHistoryData();
    });
  });
}

/**
 * Cập nhật mũi tên biểu thị chiều sắp xếp (tăng/giảm dần) trên tiêu đề cột
 * @param {string} activeRawField - Tên cột đang được click sort
 * @param {string} sortOrder - 'asc' hoặc 'desc'
 */
function updateSortHeaders(activeRawField, sortOrder) {
  document.querySelectorAll('th[data-sort]').forEach(th => {
    th.classList.remove('sort-asc', 'sort-desc');
    if (th.dataset.sort === activeRawField) {
      th.classList.add(sortOrder === 'asc' ? 'sort-asc' : 'sort-desc');
    }
  });
}

/**
 * Gọi API GET /api/devices/history
 */
async function fetchHistoryData(isBackground = false) {
  const params = {
    page: state.currentPage - 1,
    size: state.pageSize,
    sortBy: state.sortField,
    sortDir: state.sortOrder
  };

  if (state.filterDevice && state.filterDevice !== 'all') {
    params.deviceName = state.filterDevice;
  }
  if (state.filterAction && state.filterAction !== 'all') {
    params.action = state.filterAction;
  }
  if (state.filterStatus && state.filterStatus !== 'all') {
    params.status = state.filterStatus;
  }

  // Xử lý lọc thời gian nếu có (hỗ trợ nhập dạng YYYY-MM-DD)
  if (state.filterTime) {
    const raw = state.filterTime;
    if (raw.length === 10) { // Ví dụ: 2026-08-19
      params.from = `${raw}T00:00:00`;
      params.to   = `${raw}T23:59:59`;
    } else if (raw.includes(' ')) {
      const parts = raw.split(' ');
      params.from = `${parts[0]}T${parts[1]}`;
    }
  }

  try {
    const pageData = await API.getDeviceHistory(params);
    if (!pageData) return;

    state.data = pageData.content || [];
    state.totalPages = Math.max(1, pageData.totalPages || 1);
    state.totalItems = pageData.totalItems || 0;

    renderTable();
    renderPagination();
  } catch (err) {
    if (!isBackground) {
      console.error('Không thể tải lịch sử thiết bị:', err);
      const tbody = document.getElementById('historyTableBody');
      if (tbody) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px; color: #e74c3c;">Không thể tải dữ liệu: ${escapeHtml(err.message)}</td></tr>`;
      }
    }
  }
}

/**
 * Hiển thị dữ liệu lịch sử thiết bị lên bảng HTML
 */
function renderTable() {
  const resultCountEl = document.getElementById('resultCount');
  if (resultCountEl) {
    resultCountEl.textContent = `${state.totalItems} kết quả`;
  }

  const tbody = document.getElementById('historyTableBody');
  if (!tbody) return;

  if (state.data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px; color: #888;">Không tìm thấy lịch sử hoạt động nào</td></tr>`;
    return;
  }

  tbody.innerHTML = state.data.map(item => {
    let statusClass = '';
    if (item.status === 'Thành công') statusClass = 'status-success';
    else if (item.status === 'Thất bại') statusClass = 'status-fail';
    else statusClass = 'status-pending';

    const actionText = item.action ? `${item.action} thiết bị` : '--';

    return `
      <tr>
        <td>${item.id}</td>
        <td>${escapeHtml(item.deviceName)}</td>
        <td>${escapeHtml(actionText)}</td>
        <td><span class="badge ${statusClass}">${escapeHtml(item.status)}</span></td>
        <td>${formatDateTime(item.createdAt)}</td>
      </tr>
    `;
  }).join('');
}

/**
 * Hiển thị thanh phân trang động dựa trên tổng số trang
 */
function renderPagination() {
  const infoEl = document.getElementById('paginationInfo');
  const controlsEl = document.getElementById('paginationControls');

  if (infoEl) {
    infoEl.textContent = `Trang ${state.currentPage} / ${state.totalPages}`;
  }

  if (!controlsEl) return;

  let html = '';
  html += `<button ${state.currentPage === 1 ? 'disabled' : ''} onclick="goToPage(${state.currentPage - 1})">Trước</button>`;

  const maxVisiblePages = 3;
  let startPage = Math.max(1, state.currentPage - Math.floor(maxVisiblePages / 2));
  let endPage = Math.min(state.totalPages, startPage + maxVisiblePages - 1);
  startPage = Math.max(1, endPage - maxVisiblePages + 1);

  if (startPage > 1) {
    html += `<button onclick="goToPage(1)">1</button>`;
    if (startPage > 2) html += `<button disabled>...</button>`;
  }

  for (let i = startPage; i <= endPage; i++) {
    html += `<button class="${i === state.currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
  }

  if (endPage < state.totalPages) {
    if (endPage < state.totalPages - 1) html += `<button disabled>...</button>`;
    html += `<button onclick="goToPage(${state.totalPages})">${state.totalPages}</button>`;
  }

  html += `<button ${state.currentPage === state.totalPages ? 'disabled' : ''} onclick="goToPage(${state.currentPage + 1})">Sau</button>`;

  controlsEl.innerHTML = html;
}

/**
 * Chuyển trang và tải lại dữ liệu lịch sử tương ứng
 * @param {number} page - Chỉ số trang muốn xem (1-based)
 */
function goToPage(page) {
  if (page < 1 || page > state.totalPages) return;
  state.currentPage = page;
  fetchHistoryData();
}

/**
 * Định dạng chuỗi thời gian ISO thành chuỗi dễ đọc YYYY-MM-DD HH:mm:ss
 * @param {string} isoString - Chuỗi ISO thời gian
 * @returns {string} Chuỗi ngày giờ đã định dạng
 */
function formatDateTime(isoString) {
  if (!isoString) return '--';
  const d = new Date(isoString);
  if (isNaN(d.getTime())) return isoString;

  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  const s = String(d.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${day} ${h}:${min}:${s}`;
}

/**
 * Thoát các ký tự đặc biệt trong chuỗi để tránh lỗi XSS khi chèn vào HTML
 * @param {string} text - Chuỗi văn bản thô
 * @returns {string} Chuỗi văn bản an toàn
 */
function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
