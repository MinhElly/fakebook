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
        D --> E[Frontend gọi POST /services/postservice/api/posts<br/>kèm danh sách mediaIds]
        E --> F[PostService lưu bài vào DB postservice]
        F --> G[Lưu bản ghi liên kết vào bảng post_media]
    end

    subgraph Step3["3. Bắn sự kiện qua Kafka"]
        G --> H[PostService bắn PostCreatedEvent sang topic post-events]
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

    User->>CS: POST /api/comments {postId, content, parentId?}
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
    Req[Client gọi GET /api/user-profiles/friend-suggestions] --> GetSubject[Lấy currentUserId từ JWT Subject]
    GetSubject --> SQL[Chạy Native Query: findFriendSuggestions]
    
    SQL --> Logic["1. Tìm danh sách tất cả bạn bè của currentUserId (Tập F)<br/>2. Tìm bạn bè của các bạn bè trong F (Bạn của bạn - Friend of Friends)<br/>3. Loại trừ chính currentUserId và những người đã là bạn / đã gửi request<br/>4. GROUP BY gợi ý và COUNT số lượng bạn chung (mutualFriendsCount)<br/>5. ORDER BY mutualFriendsCount DESC LIMIT 100"]
    
    Logic --> Hydrate[Load thông tin UserProfile tương ứng từ DB]
    Hydrate --> DTO[Ghép profile + mutualFriendsCount + friendshipStatus = NONE]
    DTO --> Resp[Trả về danh sách UserSearchDTO cho Frontend]
```
