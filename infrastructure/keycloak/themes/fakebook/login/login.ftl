<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập vào Fakebook</title>
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
          Khám<br/>
          phá<br/>
          những<br/>
          điều <span class="fb-highlight-blue">bạn<br/>yêu<br/>thích.</span>
        </h1>

        <!-- Hero graphic illustration cards -->
        <div class="fb-hero-graphics">
          <div class="fb-graphic-card main-photo">
            <svg class="fb-placeholder-avatar" viewBox="0 0 120 120" fill="none">
              <rect width="120" height="120" rx="16" fill="#E4E6EB"/>
              <path d="M60 40a20 20 0 100 40 20 20 0 000-40zM30 100c0-16.57 13.43-30 30-30s30 13.43 30 30H30z" fill="#90949C"/>
            </svg>
            <div class="fb-reaction-heart">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="#FF2E55">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </div>
          </div>
          <div class="fb-graphic-emoji">😍</div>
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
        <h2 class="fb-form-title">Đăng nhập vào Fakebook</h2>

        <#if message?? && message.type == 'error'>
          <div class="fb-alert-error" role="alert">
            ${message.summary}
          </div>
        </#if>

        <form id="kc-form-login" action="${url.loginAction}" method="post">
          <div class="fb-input-group">
            <input
              id="username"
              name="username"
              type="text"
              required
              placeholder="Email hoặc số di động"
              value="${(login.username!'')}"
              autocomplete="username"
              autofocus
            />
          </div>

          <div class="fb-input-group fb-password-group">
            <input
              id="password"
              name="password"
              type="password"
              required
              placeholder="Mật khẩu"
              autocomplete="current-password"
            />
            <button type="button" class="fb-toggle-password" onclick="togglePasswordVisibility()" aria-label="Hiện mật khẩu">
              <svg id="eye-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#65676B" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
          </div>

          <button type="submit" id="kc-login" name="login" class="fb-btn-primary">
            Đăng nhập
          </button>

          <#if realm.resetPasswordAllowed>
            <div class="fb-forgot-wrap">
              <a href="${url.loginResetCredentialsUrl}" class="fb-forgot-link">Quên mật khẩu?</a>
            </div>
          </#if>

          <div class="fb-social-wrap">
            <#if social?? && social.providers?? && (social.providers?size > 0)>
              <#list social.providers as p>
                <a href="${p.loginUrl}" id="social-${p.alias}" class="fb-btn-google">
                  <#if p.alias == "google">
                    <svg class="fb-social-icon" viewBox="0 0 24 24" width="20" height="20">
                      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                    </svg>
                  <#else>
                    <svg class="fb-social-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
                    </svg>
                  </#if>
                  <span>Đăng nhập bằng ${p.displayName}</span>
                </a>
              </#list>
            <#else>
              <a href="/realms/jhipster/broker/google/login" class="fb-btn-google">
                <svg class="fb-social-icon" viewBox="0 0 24 24" width="20" height="20">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                </svg>
                <span>Đăng nhập bằng Google</span>
              </a>
            </#if>
          </div>

          <#if realm.password && realm.registrationAllowed?? && realm.registrationAllowed>
            <div class="fb-register-wrap">
              <a href="${url.registrationUrl}" class="fb-btn-secondary">Tạo tài khoản mới</a>
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
    function togglePasswordVisibility() {
      var pwdInput = document.getElementById("password");
      if (pwdInput) {
        pwdInput.type = pwdInput.type === "password" ? "text" : "password";
      }
    }
  </script>
</body>
</html>
