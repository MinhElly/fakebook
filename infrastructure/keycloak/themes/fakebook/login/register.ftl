<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tạo tài khoản Fakebook</title>
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
          tài khoản<br/>
          để kết nối<br/>
          với <span class="fb-highlight-blue">bạn bè.</span>
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
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </div>
          </div>
          <div class="fb-graphic-emoji">🚀</div>
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
        <h2 class="fb-form-title">Tạo tài khoản mới</h2>
        <p class="fb-form-subtitle">Nhanh chóng và dễ dàng.</p>

        <#if message?? && message.type == 'error'>
          <div class="fb-alert-error" role="alert">
            ${message.summary}
          </div>
        </#if>

        <form id="kc-register-form" action="${url.registrationAction}" method="post">
          <div class="fb-input-row">
            <div class="fb-input-group">
              <input
                id="firstName"
                name="firstName"
                type="text"
                required
                placeholder="Họ"
                value="${(register.formData.firstName!'')}"
              />
            </div>
            <div class="fb-input-group">
              <input
                id="lastName"
                name="lastName"
                type="text"
                required
                placeholder="Tên"
                value="${(register.formData.lastName!'')}"
              />
            </div>
          </div>

          <div class="fb-input-group">
            <input
              id="email"
              name="email"
              type="email"
              required
              placeholder="Địa chỉ Email"
              value="${(register.formData.email!'')}"
              autocomplete="email"
            />
          </div>

          <div class="fb-input-group">
            <input
              id="username"
              name="username"
              type="text"
              required
              placeholder="Tên người dùng (Username)"
              value="${(register.formData.username!'')}"
              autocomplete="username"
            />
          </div>

          <#if passwordRequired??>
            <div class="fb-input-group fb-password-group">
              <input
                id="password"
                name="password"
                type="password"
                required
                placeholder="Mật khẩu mới"
                autocomplete="new-password"
              />
            </div>

            <div class="fb-input-group fb-password-group">
              <input
                id="password-confirm"
                name="password-confirm"
                type="password"
                required
                placeholder="Xác nhận mật khẩu"
                autocomplete="new-password"
              />
            </div>
          </#if>

          <p class="fb-terms-text">
            Bằng cách nhấp vào Đăng ký, bạn đồng ý với Điều khoản, Chính sách quyền riêng tư và Chính sách cookie của chúng tôi.
          </p>

          <button type="submit" id="kc-register" class="fb-btn-primary">
            Đăng ký
          </button>

          <div class="fb-forgot-wrap">
            <a href="${url.loginUrl}" class="fb-forgot-link">Bạn đã có tài khoản? Đăng nhập ngay</a>
          </div>
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
</body>
</html>
