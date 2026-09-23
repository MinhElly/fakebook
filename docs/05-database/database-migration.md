# Database Migration with Liquibase

Tài liệu này hướng dẫn quy trình quản lý và thực thi migration lược đồ cơ sở dữ liệu (Database Schema Migration) bằng **Liquibase** trong Fakebook.

---

## 1. Nguyên tắc hoạt động của Liquibase trong dự án

- **Tự động thực thi khi khởi động**: Mỗi khi một microservice khởi chạy, Spring Boot Liquibase auto-configuration sẽ đọc file changelog chủ và so sánh với bảng lịch sử `DATABASECHANGELOG` trong schema của service đó. Các changeset chưa được chạy sẽ được thực thi tuần tự trong một transaction.
- **Vị trí file changelog**:
  ```text
  <microservice>/src/main/resources/config/liquibase/
  ├── master.xml                      # File changelog chủ khai báo include các changeset
  ├── changelog/                      # Thư mục chứa các file XML/YAML changeset
  │   ├── 00000000000000_initial_schema.xml
  │   └── 20260920000000_added_entity_*.xml
  └── fake-data/                      # Dữ liệu mẫu (CSV) nạp cho môi trường dev
  ```

---

## 2. Liquibase Contexts

Hệ thống phân chia ngữ cảnh thực thi (Contexts) dựa trên profile môi trường:

| Context | Môi trường áp dụng | Hành vi |
| :--- | :--- | :--- |
| **`dev`** | Local Development | Tạo bảng, index, và nạp dữ liệu cơ bản cho phát triển. |
| **`faker`** | Local Development (Tùy chọn) | Nạp dữ liệu giả lập lớn (hàng chục user, post, reaction) từ các file CSV trong `fake-data/`. |
| **`prod`** | Staging / Production | Chỉ tạo bảng, index và migration dữ liệu bắt buộc. Tuyệt đối không nạp dữ liệu faker. |

Cấu hình trong `central-server-config`:
- `application-dev.yml`: `spring.liquibase.contexts: dev, faker` (hoặc `dev`)
- `application-staging.yml`: `spring.liquibase.contexts: prod`

---

## 3. Quy trình thêm migration mới (Best Practices)

Khi cần tạo bảng mới, thêm cột hoặc tạo index:

1. **Tuyệt đối không sửa trực tiếp các changeset đã chạy**:
   - Liquibase kiểm tra mã băm MD5 (`MD5SUM`) của từng changeset. Nếu sửa đổi nội dung của changeset cũ đã chạy trên dev hoặc staging, service sẽ crash ngay khi khởi động với lỗi `ValidationFailedException: ChangeSet already executed`.
2. **Luôn tạo một file changelog mới**:
   - Tạo file mới theo quy ước đặt tên: `YYYYMMDDHHMMSS_add_<tên_thay_đổi>.xml` trong thư mục `changelog/`.
   - Ví dụ:
     ```xml
     <databaseChangeLog ...>
         <changeSet id="20260922-add-user-location" author="minh">
             <addColumn tableName="user_profiles">
                 <column name="location" type="varchar(255)"/>
             </addColumn>
         </changeSet>
     </databaseChangeLog>
     ```
3. **Include vào `master.xml`**:
   - Thêm dòng khai báo include vào cuối file `master.xml`:
     ```xml
     <include file="config/liquibase/changelog/20260922-add-user-location.xml" relativeToChangelogFile="false"/>
     ```
4. **Kiểm thử trên Local trước**:
   - Chạy service trên local, quan sát log Liquibase:
     ```text
     INFO ... [LiquibaseConfiguration] Liquibase: Successfully released change log lock
     INFO ... [LiquibaseConfiguration] ChangeSet config/liquibase/changelog/20260922-add-user-location.xml ran successfully
     ```
