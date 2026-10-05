# Core Business Data Flows (Luồng dữ liệu nghiệp vụ chính)

Tài liệu này tổng hợp 3 luồng dữ liệu nghiệp vụ phức tạp và quan trọng nhất trong Fakebook, thể hiện sự phối hợp chặt chẽ giữa Frontend, Gateway, Microservices, Database, Redis và Kafka.

---

## 1. Luồng 1: Đăng bài viết kèm Media và Phân phối Feed (Post & Fan-out Flow)

```mermaid
flowchart TD
    subgraph Step1["1. Tải ảnh/video lên Cloudinary"]
        A[User chọn file ảnh] --> B[Frontend gọi POST /services/mediaservice/api/media/upload]
        B --> C[MediaService đẩy file lên Cloudinary CDN]
        C --> D[Lưu metadata vào DB mediaservice và trả về Media UUID]
    end

    subgraph Step2["2. Đăng bài viết"]
        D --> E[Frontend gọi POST /services/postservice/api/posts/create<br/>kèm content, visibility, mediaIds, taggedUserIds]
        E --> F[PostService lưu bài vào DB postservice]
        F --> G[Lưu bản ghi liên kết vào bảng post_media]
    end

    subgraph Step3["3. Bắn sự kiện qua Kafka"]
        G --> H[PostService ghi event vào transactional outbox<br/>publisher gửi PostCreatedEvent sang topic post-events]
    end

    subgraph Step4["4. Xử lý bất đồng bộ downstream"]
        H --> I[FeedService tiêu thụ event]
        H --> J[CommentService tiêu thụ event]
        
        I --> K[FeedService gọi Feign sang UserService lấy Friends & Followers]
        K --> L[Ghi FeedItem vào DB feedservice]
        L --> M[Đẩy postId vào Redis ZSet feed:user:{id}]
        
        J --> N[CommentService ghi hoặc cập nhật bản ghi post_cache]
    end
```

---

## 2. Luồng 2: Bình luận trên bài viết (Comment Flow with PostCache)

Để tối ưu hóa hiệu năng và độ độc lập giữa các service, `commentService` không gọi HTTP sang `postService` mỗi khi có người bình luận, mà sử dụng bảng cục bộ `post_cache`:

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant CS as Comment Service (:8085)
    participant CacheDB as MariaDB (comment_service.post_cache)
    participant CommentDB as MariaDB (comment_service.comments)

    User->>CS: POST /api/comments/create {postId, content}
    CS->>CacheDB: SELECT * FROM post_cache WHERE id = :postId
    
    alt Bài viết không tồn tại trong Cache hoặc status == INACTIVE
        CS-->>User: HTTP 400 Bad Request ("Bài viết không tồn tại hoặc đã bị khóa")
    else Bài viết ACTIVE
        CS->>CommentDB: INSERT INTO comments (id, post_id, author_id, content, parent_id, created_at)
        CommentDB-->>CS: Thành công
        CS-->>User: HTTP 201 Created (Trả về Comment DTO)
    end
```

---

## 3. Luồng 3: Gợi ý kết bạn dựa trên bạn chung (Friend Suggestion Flow)

Trong `userService` (`UserProfileService.java`), thuật toán tìm kiếm gợi ý kết bạn hoạt động theo cơ chế tính toán số lượng bạn chung:

```mermaid
flowchart TD
    Req[Client gọi GET /api/user-profiles/suggestions] --> GetSubject[Lấy currentUserId từ JWT Subject]
    GetSubject --> SQL[Chạy Native Query: findFriendSuggestions]
    
    SQL --> Logic["1. Tìm danh sách tất cả bạn bè của currentUserId (Tập F)<br/>2. Tìm bạn bè của các bạn bè trong F (Bạn của bạn - Friend of Friends)<br/>3. Loại trừ chính currentUserId và những người đã là bạn / đã gửi request<br/>4. GROUP BY gợi ý và COUNT số lượng bạn chung (mutualFriendsCount)<br/>5. ORDER BY mutualFriendsCount DESC LIMIT 100"]
    
    Logic --> Hydrate[Load thông tin UserProfile tương ứng từ DB]
    Hydrate --> DTO[Ghép profile + mutualFriendsCount + friendshipStatus = NONE]
    DTO --> Resp[Trả về danh sách UserSearchDTO cho Frontend]
```

---

## 4. Luồng 4: Lưu bài viết & Truy xuất bài đã lưu (Saved Posts Flow)

Tính năng lưu bài viết (Bookmark) cho phép người dùng lưu trữ các bài đăng quan tâm và xem lại tập trung:

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant UI as React Frontend (/saved)
    participant GW as API Gateway (:8080)
    participant PS as Post Service (:8083)
    participant DB as MariaDB (post_service.saved_post)

    alt Lưu hoặc Bỏ lưu bài viết (Toggle)
        User->>UI: Nhấn "Lưu bài viết" trên menu Post
        UI->>GW: POST /services/postservice/api/posts/{postId}/save (Bearer JWT)
        GW->>PS: Forward request kèm userId từ token
        PS->>DB: Kiểm tra bản ghi (user_id, post_id)
        alt Đã lưu trước đó
            PS->>DB: DELETE FROM saved_post WHERE user_id = :uid AND post_id = :pid
            PS-->>UI: HTTP 200 OK (body: false - đã bỏ lưu)
        else Chưa lưu
            PS->>DB: INSERT INTO saved_post (id, user_id, post_id, created_at)
            PS-->>UI: HTTP 200 OK (body: true - đã lưu)
        end
    else Xem danh sách bài viết đã lưu
        User->>UI: Truy cập menu "Đã lưu" (/saved)
        UI->>GW: GET /services/postservice/api/posts/saved (Bearer JWT)
        GW->>PS: Forward request kèm userId
        PS->>DB: Query các post_id từ saved_post WHERE user_id = :uid ORDER BY created_at DESC
        PS->>DB: Nạp thông tin Post tương ứng, lọc bài còn ACTIVE/quyền xem
        PS-->>UI: HTTP 200 OK (List<PostDTO>)
        UI-->>User: Render danh sách bài viết trên SavedPage
    end
```

