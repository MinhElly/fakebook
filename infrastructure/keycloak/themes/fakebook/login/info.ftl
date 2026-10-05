<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thông báo khôi phục mật khẩu - Fakebook</title>
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
          Kiểm tra<br/>
          hộp thư<br/>
          email của <span class="fb-highlight-blue">bạn.</span>
        </h1>

        <div class="fb-hero-graphics">
          <div class="fb-graphic-card main-photo">
            <svg class="fb-placeholder-avatar" viewBox="0 0 120 120" fill="none">
              <rect width="120" height="120" rx="16" fill="#E4E6EB"/>
              <path d="M60 40a20 20 0 100 40 20 20 0 000-40zM30 100c0-16.57 13.43-30 30-30s30 13.43 30 30H30z" fill="#90949C"/>
            </svg>
            <div class="fb-reaction-heart">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="#0064E0">
                <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
              </svg>
            </div>
          </div>
          <div class="fb-graphic-emoji">📩</div>
        </div>
      </div>
    </div>

    <!-- ── CENTER DIVIDER ──────────────────────────────────────── -->
    <div class="fb-divider"></div>

    <!-- ── RIGHT FORM COLUMN ──────────────────────────────────── -->
    <div class="fb-form-section">
      <div class="fb-form-card">
        <h2 class="fb-form-title">Đã gửi hướng dẫn khôi phục</h2>

        <#if message??>
          <div class="fb-alert-success" role="alert">
            ${message.summary}
          </div>
        <#else>
          <div class="fb-alert-success" role="alert">
            Nếu thông tin email/tên tài khoản chính xác, bạn sẽ nhận được email hướng dẫn đặt lại mật khẩu trong giây lát.
          </div>
        </#if>

        <p class="fb-terms-text" style="font-size: 14px; margin-top: 12px; line-height: 1.5;">
          Vui lòng kiểm tra hộp thư đến (Inbox) hoặc thư mục Spam/Rác để nhận đường dẫn đặt lại mật khẩu.
        </p>

        <div style="margin-top: 24px; display: flex; flex-direction: column; gap: 12px;">
          <#if actionUri??>
            <a href="${actionUri}" class="fb-btn-primary" style="display: flex; align-items: center; justify-content: center; text-decoration: none;">Gửi lại email xác nhận</a>
          </#if>
          <a href="${url.loginUrl}" class="fb-btn-secondary">Quay lại Đăng nhập</a>
        </div>

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
