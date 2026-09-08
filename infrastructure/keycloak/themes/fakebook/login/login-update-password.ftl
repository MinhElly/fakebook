<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cập nhật mật khẩu - Fakebook</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
</head>
<body class="fb-login-body">
  <div class="fb-login-container">
    
    <!-- ── LEFT HERO COLUMN ───────────────────────────────────── -->
    <div class="fb-hero-section">
      <div class="fb-brand-header">
        <svg class="fb-logo-icon" viewBox="0 0 40 40" width="52" height="52" fill="none">
          <circle cx="20" cy="20" r="20" fill="#0064E0"/>
          <path d="M27 13h-3c-1.1 0-2 .9-2 2v2h5l-.7 5H22v12h-5V22h-3v-5h3v-2c0-3.3 2.7-6 6-6h4v4z" fill="white"/>
        </svg>
      </div>

      <div class="fb-hero-body">
        <h1 class="fb-hero-title">
          Tạo<br/>
          mật khẩu<br/>
          mới an toàn<br/>
          cho <span class="fb-highlight-blue">tài khoản.</span>
        </h1>

        <!-- Hero graphic illustration cards -->
        <div class="fb-hero-graphics">
          <div class="fb-graphic-card main-photo">
            <svg class="fb-placeholder-avatar" viewBox="0 0 120 120" fill="none">
              <rect width="120" height="120" rx="16" fill="#E4E6EB"/>
              <path d="M60 40a20 20 0 100 40 20 20 0 000-40zM30 100c0-16.57 13.43-30 30-30s30 13.43 30 30H30z" fill="#90949C"/>
            </svg>
            <div class="fb-reaction-heart">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="#0064E0">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-5.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8s0 0 0 0z"/>
              </svg>
            </div>
          </div>
          <div class="fb-graphic-emoji">🔐</div>
          <div class="fb-graphic-badge">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="#0064E0">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- ── CENTER DIVIDER ──────────────────────────────────────── -->
    <div class="fb-divider"></div>

    <!-- ── RIGHT FORM COLUMN ──────────────────────────────────── -->
    <div class="fb-form-section">
      <div class="fb-form-card">
        <h2 class="fb-form-title">Đặt mật khẩu mới</h2>
        <p class="fb-form-subtitle">Mật khẩu mới của bạn phải có ít nhất 6 ký tự để đảm bảo an toàn.</p>

        <#if message?? && message.type == 'error'>
          <div class="fb-alert-error" role="alert">
            ${message.summary}
          </div>
        <#elseif message?? && message.type == 'success'>
          <div class="fb-alert-success" role="alert">
            ${message.summary}
          </div>
        </#if>

        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
          <input type="text" id="username" name="username" value="${username!''}" autocomplete="username" readonly style="display:none;" />
          <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;" />

          <div class="fb-input-group fb-password-group">
            <input
              id="password-new"
              name="password-new"
              type="password"
              required
              placeholder="Mật khẩu mới"
              autocomplete="new-password"
              autofocus
            />
            <button type="button" class="fb-toggle-password" onclick="togglePasswordVisibility('password-new')" aria-label="Hiện mật khẩu">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#65676B" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
          </div>

          <div class="fb-input-group fb-password-group">
            <input
              id="password-confirm"
              name="password-confirm"
              type="password"
              required
              placeholder="Xác nhận mật khẩu mới"
              autocomplete="new-password"
            />
            <button type="button" class="fb-toggle-password" onclick="togglePasswordVisibility('password-confirm')" aria-label="Hiện mật khẩu">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#65676B" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
          </div>

          <#if logoutSessions??>
            <div style="margin-bottom: 16px; display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--fb-secondary-text, #65676B);">
              <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked style="width: 18px; height: 18px; accent-color: var(--fb-blue, #0064E0); cursor: pointer;" />
              <label for="logout-sessions" style="cursor: pointer;">Đăng xuất khỏi các thiết bị khác</label>
            </div>
          </#if>

          <button type="submit" id="kc-submit" class="fb-btn-primary">
            Cập nhật mật khẩu
          </button>

          <#if isAppInitiatedAction?? && isAppInitiatedAction>
            <div class="fb-forgot-wrap">
              <button type="submit" name="cancel-aia" value="true" class="fb-btn-secondary">Hủy bỏ</button>
            </div>
          </#if>
        </form>

        <div class="fb-meta-footer">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="#0064E0">
            <path d="M12 6C8.686 6 6 8.686 6 12s2.686 6 6 6 6-2.686 6-6-2.686-6-6-6zm0 10c-2.209 0-4-1.791-4-4s1.791-4 4-4 4 1.791 4 4-1.791 4-4 4z"/>
          </svg>
          <span>Meta</span>
        </div>
      </div>
    </div>

  </div>

  <script>
    function togglePasswordVisibility(id) {
      var pwdInput = document.getElementById(id);
      if (pwdInput) {
        pwdInput.type = pwdInput.type === "password" ? "text" : "password";
      }
    }
  </script>
</body>
</html>
