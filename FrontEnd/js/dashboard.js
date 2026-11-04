/**
 * Trang Dashboard - Kết nối Backend API IoT
 * Giám sát cảm biến thời gian thực và điều khiển thiết bị qua Backend & MQTT
 */

let dashboardRefreshInterval = null;
let chartRefreshInterval = null;

document.addEventListener('DOMContentLoaded', () => {
  checkAuth();
  renderSidebarUser();
  setupLogout();
  initChart();
  loadDashboardData();
  loadChartData();
  setupDeviceControls();

  // Cập nhật dữ liệu cảm biến & trạng thái thiết bị mỗi 1.5 giây (nhanh & mượt mà)
  dashboardRefreshInterval = setInterval(() => {
    loadDashboardData();
  }, 1500);

  // Cập nhật biểu đồ mỗi 4 giây để giảm tải mạng
  chartRefreshInterval = setInterval(() => {
    loadChartData();
  }, 4000);

  // Tự động làm mới ngay lập tức khi người dùng quay lại tab trình duyệt
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
      loadDashboardData();
      loadChartData();
    }
  });

  window.addEventListener('focus', () => {
    loadDashboardData();
  });
});

// Dừng interval khi rời trang
window.addEventListener('beforeunload', () => {
  if (dashboardRefreshInterval) clearInterval(dashboardRefreshInterval);
  if (chartRefreshInterval) clearInterval(chartRefreshInterval);
});

/**
 * Khởi tạo cấu hình biểu đồ Chart.js
 */
function initChart() {
  const ctx = document.getElementById('sensorChart');
  if (!ctx) return;

  window.sensorChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'Nhiệt độ (°C)',
          data: [],
          borderColor: '#cc8800',
          backgroundColor: 'rgba(204, 136, 0, 0.1)',
          borderWidth: 2,
          tension: 0.4,
          yAxisID: 'y'
        },
        {
          label: 'Độ sáng (lux)',
          data: [],
          borderColor: '#8b7500',
          backgroundColor: 'rgba(139, 117, 0, 0.1)',
          borderWidth: 2,
          tension: 0.4,
          yAxisID: 'y1'
        },
        {
          label: 'Độ ẩm (%)',
          data: [],
          borderColor: '#2c3e6b',
          backgroundColor: 'rgba(44, 62, 107, 0.1)',
          borderWidth: 2,
          tension: 0.4,
          yAxisID: 'y'
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          mode: 'index',
          intersect: false,
          backgroundColor: 'rgba(26, 42, 74, 0.9)',
          titleFont: { family: 'Inter', size: 13 },
          bodyFont: { family: 'Inter', size: 13 },
          padding: 10,
          cornerRadius: 8
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { font: { family: 'Inter' } }
        },
        y: {
          type: 'linear',
          display: true,
          position: 'left',
          grid: { color: 'rgba(0,0,0,0.04)' },
          min: 0,
          max: 100
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          grid: { display: false },
          min: 0,
          max: 5000
        }
      }
    }
  });
}

/**
 * Tải dữ liệu cảm biến mới nhất và trạng thái thiết bị từ GET /api/dashboard
 */
async function loadDashboardData() {
  try {
    const data = await API.getDashboard();
    if (!data) return;

    // Cập nhật giá trị hiển thị trên các thẻ cảm biến
    const tempEl = document.getElementById('tempValue');
    const humEl = document.getElementById('humidityValue');
    const lightEl = document.getElementById('lightValue');

    if (tempEl)  tempEl.textContent  = data.temperature != null ? `${Number(data.temperature).toFixed(1)}°C` : '--°C';
    if (humEl)   humEl.textContent   = data.humidity != null ? `${Number(data.humidity).toFixed(1)}%` : '--%';
    if (lightEl) lightEl.textContent = data.light != null ? `${Number(data.light).toFixed(0)} lux` : '-- lux';

    // Cập nhật trạng thái công tắc (chỉ cập nhật nếu người dùng không đang thao tác)
    updateSwitchUI('light', data.led1);
    updateSwitchUI('fan', data.led2);
    updateSwitchUI('ac', data.led3);
  } catch (err) {
    console.warn('Lỗi khi tải dữ liệu dashboard:', err.message);
  }
}

/**
 * Cập nhật giao diện công tắc bật/tắt của một thiết bị cụ thể
 * @param {string} deviceId - Mã thiết bị ('light' | 'fan' | 'ac')
 * @param {boolean} stateBool - Trạng thái bật (true) hoặc tắt (false)
 */
function updateSwitchUI(deviceId, stateBool) {
  const input = document.querySelector(`.toggle-switch input[data-device="${deviceId}"]`);
  if (!input || input.disabled) return;

  const isChecked = Boolean(stateBool);
  input.checked = isChecked;

  const deviceItem = input.closest('.device-item');
  if (deviceItem) {
    deviceItem.classList.toggle('active', isChecked);
  }
}

/**
 * Tải 7 điểm dữ liệu cảm biến gần nhất từ Backend cho biểu đồ
 */
async function loadChartData() {
  if (!window.sensorChart) return;

  try {
    const [tempList, humList, lightList] = await Promise.all([
      API.getSensorChart('Nhiệt độ').catch(() => []),
      API.getSensorChart('Độ ẩm').catch(() => []),
      API.getSensorChart('Ánh sáng').catch(() => [])
    ]);

    // Lấy danh sách thời gian làm nhãn trục X
    const maxLen = Math.max(tempList.length, humList.length, lightList.length);
    if (maxLen === 0) return;

    // Chọn danh sách có nhiều bản ghi nhất để lấy mốc thời gian
    const sourceList = [tempList, humList, lightList].reduce((a, b) => a.length >= b.length ? a : b);
    const labels = sourceList.map(item => {
      const d = new Date(item.createdAt);
      return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
    });

    const chart = window.sensorChart;
    chart.data.labels = labels;
    chart.data.datasets[0].data = tempList.map(item => item.value);
    chart.data.datasets[1].data = lightList.map(item => item.value);
    chart.data.datasets[2].data = humList.map(item => item.value);
    chart.update('none');
  } catch (err) {
    console.warn('Lỗi khi tải dữ liệu biểu đồ:', err.message);
  }
}

/**
 * Thiết lập sự kiện điều khiển thiết bị gọi POST /api/devices/command
 */
function setupDeviceControls() {
  document.querySelectorAll('.toggle-switch input').forEach(input => {
    input.addEventListener('change', async (e) => {
      const deviceId = input.dataset.device; // 'light' | 'fan' | 'ac'
      const isOn = e.target.checked;
      const command = isOn ? 'ON' : 'OFF';

      const toggleSwitch = input.closest('.toggle-switch');
      const deviceItem = input.closest('.device-item');

      if (toggleSwitch) toggleSwitch.classList.add('is-loading');
      input.disabled = true;

      try {
        await API.sendDeviceCommand(deviceId, command);

        if (deviceItem) {
          deviceItem.classList.toggle('active', isOn);
        }

        // Cập nhật lại trạng thái đồng bộ ngay lập tức từ backend
        loadDashboardData();
      } catch (err) {
        alert(`Điều khiển thiết bị thất bại: ${err.message}`);
        // Phục hồi lại trạng thái cũ
        input.checked = !isOn;
        if (deviceItem) {
          deviceItem.classList.toggle('active', !isOn);
        }
      } finally {
        if (toggleSwitch) toggleSwitch.classList.remove('is-loading');
        input.disabled = false;
      }
    });
  });
}
