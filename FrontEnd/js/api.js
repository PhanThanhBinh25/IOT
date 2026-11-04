/**
 * IoT Smart Home - Module API Client tập trung
 * Kết nối Frontend với Backend Spring Boot (http://localhost:8080/api)
 */

const API_BASE_URL = 'http://localhost:8080/api';

const StorageKey = {
  TOKEN: 'iot_token',
  USER: 'iot_user'
};

/**
 * Lấy JWT Access Token từ LocalStorage
 * @returns {string|null} Chuỗi token hoặc null nếu chưa đăng nhập
 */
function getAuthToken() {
  return localStorage.getItem(StorageKey.TOKEN);
}

/**
 * Lấy thông tin người dùng đã lưu trong LocalStorage
 * @returns {object|null} Đối tượng thông tin user hoặc null
 */
function getUserInfo() {
  try {
    const raw = localStorage.getItem(StorageKey.USER);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

/**
 * Lưu token xác thực và thông tin người dùng vào LocalStorage
 * @param {string} token - JWT Token nhận được từ server
 * @param {object|string} user - Thông tin tài khoản người dùng
 */
function saveAuth(token, user) {
  if (token) localStorage.setItem(StorageKey.TOKEN, token);
  if (user)  localStorage.setItem(StorageKey.USER, typeof user === 'string' ? user : JSON.stringify(user));
}

/**
 * Xóa toàn bộ thông tin phiên đăng nhập khỏi LocalStorage khi đăng xuất
 */
function clearAuth() {
  localStorage.removeItem(StorageKey.TOKEN);
  localStorage.removeItem(StorageKey.USER);
}

/**
 * Kiểm tra quyền truy cập: nếu chưa có token thì tự động chuyển hướng về trang đăng nhập
 */
function checkAuth() {
  const token = getAuthToken();
  if (!token) {
    window.location.href = 'login.html';
  }
}

/**
 * Gắn sự kiện click cho nút Đăng xuất trên thanh điều hướng sidebar
 */
function setupLogout() {
  const btnLogout = document.getElementById('btnLogout');
  if (btnLogout) {
    btnLogout.addEventListener('click', () => {
      clearAuth();
      window.location.href = 'login.html';
    });
  }
}

/**
 * Hiển thị tên, vai trò và chữ cái avatar của người dùng lên sidebar
 */
function renderSidebarUser() {
  const user = getUserInfo();
  if (!user) return;

  const nameEl = document.querySelector('.sidebar-user-name');
  const roleEl = document.querySelector('.sidebar-user-role');
  const avatarEl = document.querySelector('.sidebar-user-avatar');

  if (nameEl) nameEl.textContent = user.username || user.fullName || 'Admin';
  if (roleEl) roleEl.textContent = user.role === 'ADMIN' ? 'Quản trị viên' : (user.role || 'Người dùng');
  if (avatarEl && (user.username || user.fullName)) {
    const name = user.fullName || user.username;
    avatarEl.textContent = name.trim().charAt(0).toUpperCase();
  }
}

/**
 * Hàm gửi HTTP Request dùng chung kèm JWT Bearer token và xử lý lỗi tập trung
 * @param {string} endpoint - Đường dẫn API (ví dụ: '/dashboard')
 * @param {object} options - Cấu hình fetch (method, headers, body)
 * @returns {Promise<any>} Dữ liệu JSON phản hồi từ server
 */
async function apiFetch(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers
  };

  try {
    const response = await fetch(url, config);

    if (response.status === 401) {
      clearAuth();
      if (!window.location.pathname.endsWith('login.html')) {
        window.location.href = 'login.html';
      }
      throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }

    const result = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMsg = (result && result.message) ? result.message : `Lỗi ${response.status}: ${response.statusText}`;
      throw new Error(errorMsg);
    }

    return result; // ApiResponse: { success, message, data }
  } catch (err) {
    console.error(`[API Error] ${options.method || 'GET'} ${url}:`, err);
    throw err;
  }
}

/**
 * Đối tượng gom nhóm các API dịch vụ gọi đến Backend
 */
const API = {
  /**
   * Đăng nhập hệ thống lấy JWT Token
   * @param {string} username - Tên đăng nhập
   * @param {string} password - Mật khẩu
   * @returns {Promise<object>} Dữ liệu phiên đăng nhập { token, username, fullName, role }
   */
  async login(username, password) {
    const res = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    return res.data; // LoginResponse: { token, tokenType, username, fullName, role }
  },

  /**
   * Lấy thông tin hồ sơ của tài khoản đang đăng nhập
   * @returns {Promise<object>} Thông tin hồ sơ người dùng
   */
  async getProfile() {
    const res = await apiFetch('/users/me');
    return res.data;
  },

  /**
   * Lấy tổng quan Dashboard: Giá trị cảm biến mới nhất và trạng thái các thiết bị
   * @returns {Promise<object>} { temperature, humidity, light, led1, led2, led3 }
   */
  async getDashboard() {
    const res = await apiFetch('/dashboard');
    return res.data;
  },

  /**
   * Lấy danh sách 7 điểm dữ liệu cảm biến gần nhất để vẽ biểu đồ
   * @param {string} sensorName - Tên loại cảm biến ('Nhiệt độ' | 'Độ ẩm' | 'Ánh sáng')
   * @returns {Promise<Array>} Danh sách điểm đo cảm biến theo thời gian
   */
  async getSensorChart(sensorName) {
    const res = await apiFetch(`/sensors/chart?sensorName=${encodeURIComponent(sensorName)}`);
    return res.data;
  },

  /**
   * Tìm kiếm, phân trang và sắp xếp dữ liệu đo đạc của cảm biến
   * @param {object} params - Các tham số lọc (page, size, sortBy, sortDir, sensorName, keyword, from, to)
   * @returns {Promise<object>} Đối tượng phân trang { content, totalPages, totalItems, ... }
   */
  async getSensors(params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', params.page);
    if (params.size !== undefined) query.set('size', params.size);
    if (params.sortBy) query.set('sortBy', params.sortBy);
    if (params.sortDir) query.set('sortDir', params.sortDir);
    if (params.sensorName && params.sensorName !== 'all') query.set('sensorName', params.sensorName);
    if (params.keyword) query.set('keyword', params.keyword);
    if (params.from) query.set('from', params.from);
    if (params.to) query.set('to', params.to);

    const res = await apiFetch(`/sensors?${query.toString()}`);
    return res.data; // PageResponse<SensorDataResponse>
  },

  /**
   * Gửi lệnh bật hoặc tắt thiết bị đến Backend & ESP32 qua MQTT
   * @param {string} device - Mã thiết bị ('light' | 'fan' | 'ac')
   * @param {string} command - Lệnh điều khiển ('ON' | 'OFF')
   * @returns {Promise<any>} Kết quả phản hồi từ server
   */
  async sendDeviceCommand(device, command) {
    const res = await apiFetch('/devices/command', {
      method: 'POST',
      body: JSON.stringify({ device, command })
    });
    return res.data;
  },

  /**
   * Lấy trạng thái hiện tại của tất cả thiết bị
   * @returns {Promise<object>} { light: "ON"|"OFF", fan: "ON"|"OFF", ac: "ON"|"OFF" }
   */
  async getDeviceStates() {
    const res = await apiFetch('/devices/states');
    return res.data;
  },

  /**
   * Tìm kiếm và phân trang lịch sử thao tác bật/tắt thiết bị
   * @param {object} params - Bộ lọc (page, size, sortBy, sortDir, deviceName, action, status, from, to)
   * @returns {Promise<object>} Đối tượng phân trang lịch sử hoạt động
   */
  async getDeviceHistory(params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', params.page);
    if (params.size !== undefined) query.set('size', params.size);
    if (params.sortBy) query.set('sortBy', params.sortBy);
    if (params.sortDir) query.set('sortDir', params.sortDir);
    if (params.deviceName && params.deviceName !== 'all') query.set('deviceName', params.deviceName);
    if (params.action && params.action !== 'all') query.set('action', params.action);
    if (params.status && params.status !== 'all') query.set('status', params.status);
    if (params.from) query.set('from', params.from);
    if (params.to) query.set('to', params.to);

    const res = await apiFetch(`/devices/history?${query.toString()}`);
    return res.data; // PageResponse<DeviceHistoryResponse>
  }
};


