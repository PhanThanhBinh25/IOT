/**
 * Trang Đăng Nhập - Module xử lý xác thực người dùng qua Backend API
 */

document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm');
  const usernameInput = document.getElementById('username');
  const passwordInput = document.getElementById('password');
  const togglePasswordBtn = document.getElementById('togglePassword');
  const loginError = document.getElementById('loginError');
  const btnLogin = document.getElementById('btnLogin');

  // Kiểm tra phiên đăng nhập hiện tại
  if (getAuthToken()) {
    window.location.href = 'dashboard.html';
    return;
  }

  // Sự kiện toggle ẩn/hiện mật khẩu
  togglePasswordBtn.addEventListener('click', () => {
    const isPassword = passwordInput.type === 'password';
    passwordInput.type = isPassword ? 'text' : 'password';
    togglePasswordBtn.textContent = isPassword ? 'Ẩn' : 'Hiện';
  });

  // Xử lý sự kiện đăng nhập gọi API
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    // nhận thông tin người dùng nhập vào
    const username = usernameInput.value.trim(); 
    const password = passwordInput.value.trim();

    if (!username || !password) {
      showError('Vui lòng nhập đầy đủ thông tin');
      return;
    }

    // Hiệu ứng loading
    btnLogin.classList.add('loading');
    btnLogin.textContent = 'Đang xử lý...';
    hideError();

    try {
      const data = await API.login(username, password); // Gọi API đăng nhập và nhận dữ liệu phản hồi từ server
      // Lưu token và thông tin người dùng từ Backend
      saveAuth(data.token, {
        username: data.username,
        fullName: data.fullName,
        role: data.role
      });

      window.location.href = 'dashboard.html';
    } catch (err) {
      showError(err.message || 'Tên đăng nhập hoặc mật khẩu không đúng');
      btnLogin.classList.remove('loading');
      btnLogin.textContent = 'Đăng nhập';
    }
  });

  /**
   * Hiển thị thông báo lỗi trên giao diện đăng nhập.
   * @param {string} message - Nội dung thông báo lỗi cần hiển thị
   */
  function showError(message) {
    loginError.textContent = message;
    loginError.classList.add('show');
  }

  /**
   * Ẩn thông báo lỗi trên giao diện đăng nhập khi người dùng nhập lại thông tin.
   */
  function hideError() {
    loginError.classList.remove('show');
  }

  usernameInput.addEventListener('input', hideError);
  passwordInput.addEventListener('input', hideError);
});
