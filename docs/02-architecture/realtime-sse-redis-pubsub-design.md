# Thiết kế Kiến trúc: Realtime SSE Multi-Pod với Redis Pub/Sub Adapter

## 1. Bối cảnh & Vấn đề Kỹ thuật (Context & Problem Statement)

Hiện tại, chức năng Server-Sent Events (SSE) tại API Gateway được cài đặt thông qua `RealtimeEventHub` (`gateway/src/main/java/com/minh/fakebook/gateway/realtime/RealtimeEventHub.java`):

```java
@Component
public class RealtimeEventHub {
    private final Sinks.Many<RealtimeEvent> sink = Sinks.many().multicast().directBestEffort();

    public void publish(RealtimeEvent event) {
        sink.tryEmitNext(event);
    }

    public Flux<RealtimeEvent> events() {
        return sink.asFlux();
    }
}
```

### Hạn chế kiến trúc (Architectural Bottleneck):
- `sink` là đối tượng **in-memory** cục bộ bên trong JVM của pod Gateway.
- Khi một người dùng mở kết nối SSE (`GET /api/realtime/events`), kết nối HTTP persistent được neo vào một pod Gateway cụ thể (ví dụ: `gateway-pod-1`).
- Khi Kafka Consumers (`ReactionEventConsumerConfiguration`, `CommentEventConsumerConfiguration`, `FriendRequestEventConsumerConfiguration`) nhận message từ broker, Kafka phân phối partition message cho một pod Gateway bất kỳ (ví dụ: `gateway-pod-2`).
- Nếu Gateway scale `replicas > 1`: Message đến `gateway-pod-2` sẽ chỉ phát ra `sink` nội bộ của pod 2. Người dùng đang giữ kết nối SSE tại `gateway-pod-1` **sẽ không bao giờ nhận được thông báo**.
- Do đó, Gateway hiện tại **bắt buộc phải chạy `replicas: 1`**.

---

## 2. Mục tiêu Thiết kế (Design Goals)

1. **Horizontally Scalable SSE**: Cho phép Gateway scale lên $N$ replicas (`replicas: 2..10`) mà không làm mất thông báo realtime của người dùng.
2. **Low Latency & High Throughput**: Độ trễ phát tán thông báo giữa các pod $< 10\text{ms}$.
3. **Reactive Native**: Sử dụng `ReactiveRedisTemplate` của Spring Data Redis Reactive phù hợp với Spring Cloud Gateway (Project Reactor).
4. **Backward Compatibility**: Không làm thay đổi giao thức client SSE hoặc format JSON của `RealtimeEvent`.

---

## 3. Kiến trúc Đề xuất (Target Architecture)

```
[ Downstream Services (User / Post / Comment) ]
                     │  (Kafka Outbox)
                     ▼
             [ Kafka Topics ]
         (post-reaction-events, ...)
                     │
       ┌─────────────┴─────────────┐
       ▼                           ▼
[ Gateway Pod 1 ]           [ Gateway Pod 2 ]
(Kafka Consumer)            (Kafka Consumer)
       │                           │
       └─────────────┬─────────────┘
                     ▼  (publish to channel: "fakebook:realtime:events")
             [ Redis Cluster / Pub/Sub ]
                     │
       ┌─────────────┴─────────────┐
       ▼ (subscribe to channel)    ▼ (subscribe to channel)
[ Gateway Pod 1 ]           [ Gateway Pod 2 ]
(Redis Listener)            (Redis Listener)
       │                           │
       ▼ (Local Sinks.Many)        ▼ (Local Sinks.Many)
[ Client A (SSE) ]          [ Client B (SSE) ]
```

### Luồng xử lý:
1. **Ingest**: Pod Gateway nhận message từ Kafka topic sẽ parse và đóng gói thành `RealtimeEvent`.
2. **Broadcast**: Thay vì chỉ gọi `sink.tryEmitNext(event)` cục bộ, pod Gateway gọi `reactiveRedisTemplate.convertAndSend("fakebook:realtime:events", event)`.
3. **Dispatch**: Tất cả các pod Gateway đang lắng nghe channel `fakebook:realtime:events` sẽ nhận được message và đẩy vào `sink.tryEmitNext(event)` cục bộ của từng pod.
4. **Push**: Client SSE kết nối vào bất kỳ pod Gateway nào cũng đều nhận được thông báo đầy đủ.

---

## 4. Kế hoạch Triển khai (Sprint Roadmap)

### Phase 1: Thêm Redis Reactive Dependencies & Configuration
- Tận dụng `spring-boot-starter-data-redis-reactive` đã có trong Gateway.
- Cấu hình Channel Topic `fakebook:realtime:events` và JSON Serializer cho `RealtimeEvent`.

### Phase 2: Refactor `RealtimeEventHub`
Tách interface và triển khai `RedisRealtimeEventHub`:
```java
public interface EventHub {
    Mono<Void> publish(RealtimeEvent event);
    Flux<RealtimeEvent> events();
}
```

### Phase 3: Integration Tests
- Viết integration test sử dụng `Testcontainers` (Kafka + Redis) với 2 instance Gateway độc lập để kiểm chứng kết nối chéo pod (Cross-pod SSE delivery test).

### Phase 4: K8s Autoscaling (HPA)
- Sau khi hoàn thành và verify Phase 3, mở khóa `replicas: 1` trong `infrastructure/k8s/base/gateway.yaml` và cấu hình HPA (`minReplicas: 2, maxReplicas: 5, targetCPUUtilization: 75%`).
