/**
 * Trang Hồ Sơ (Profile) - Kết nối Backend API IoT
 * Tải thông tin người dùng thực tế từ GET /api/users/me
 */

document.addEventListener('DOMContentLoaded', () => {
  checkAuth();
  renderSidebarUser();
  setupLogout();
  loadProfile();
});

/**
 * Tải thông tin cá nhân của người dùng từ API /api/users/me và hiển thị lên giao diện
 */
async function loadProfile() {
  try {
    const profile = await API.getProfile();
    if (!profile) return;

    const nameEl = document.getElementById('profileName');
    const avatarEl = document.getElementById('profileAvatar');
    const usernameEl = document.getElementById('profileUsername');
    const dobEl = document.getElementById('profileDOB');
    const emailEl = document.getElementById('profileEmail');
    const phoneEl = document.getElementById('profilePhone');

    const fullName = profile.fullName || profile.username || 'Admin';
    if (nameEl) nameEl.textContent = fullName;
    if (usernameEl) usernameEl.textContent = profile.username;
    if (dobEl) dobEl.textContent = profile.dateOfBirth || '28/08/2005';
    if (emailEl) emailEl.textContent = profile.email || 'phanthanhbinh@gmail.com';
    if (phoneEl) phoneEl.textContent = profile.phone || '0349883754';

    // Tạo chữ viết tắt cho avatar (Ví dụ: "Phan Thanh Binh" -> "PTB")
    if (avatarEl) {
      const words = fullName.trim().split(/\s+/);
      let initials = '';
      if (words.length >= 2) {
        initials = words.map(w => w.charAt(0).toUpperCase()).join('');
      } else {
        initials = fullName.substring(0, 2).toUpperCase();
      }
      avatarEl.textContent = initials || 'AD';
    }

    // Cập nhật lại tên trên sidebar nếu có
    const sidebarName = document.querySelector('.sidebar-user-name');
    if (sidebarName) {
      sidebarName.textContent = profile.username;
    }
  } catch (err) {
    console.error('Không thể tải thông tin profile từ server:', err);
  }
}
