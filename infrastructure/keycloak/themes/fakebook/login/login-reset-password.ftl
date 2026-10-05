<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Khôi phục mật khẩu Fakebook</title>
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
          Tìm lại<br/>
          tài khoản<br/>
          Fakebook<br/>
          của <span class="fb-highlight-blue">bạn.</span>
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
                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
              </svg>
            </div>
          </div>
          <div class="fb-graphic-emoji">🔑</div>
        </div>
      </div>
    </div>

    <!-- ── CENTER DIVIDER ──────────────────────────────────────── -->
    <div class="fb-divider"></div>

    <!-- ── RIGHT FORM COLUMN ──────────────────────────────────── -->
    <div class="fb-form-section">
      <div class="fb-form-card">
        <h2 class="fb-form-title">Tìm tài khoản của bạn</h2>
        <p class="fb-form-subtitle">Vui lòng nhập email hoặc tên người dùng để tìm kiếm tài khoản của bạn.</p>

        <#if message?? && message.type == 'error'>
          <div class="fb-alert-error" role="alert">
            ${message.summary}
          </div>
        <#elseif message?? && message.type == 'success'>
          <div class="fb-alert-success" role="alert">
            ${message.summary}
          </div>
        </#if>

        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
          <div class="fb-input-group">
            <input
              id="username"
              name="username"
              type="text"
              required
              placeholder="Email hoặc tên người dùng"
              value="${(auth.attemptedUsername!'')}"
              autofocus
            />
          </div>

          <button type="submit" id="kc-reset" class="fb-btn-primary">
            Gửi yêu cầu khôi phục
          </button>

          <div class="fb-forgot-wrap">
            <a href="${url.loginUrl}" class="fb-btn-secondary">Hủy và quay lại Đăng nhập</a>
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
