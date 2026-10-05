import java.time.*;
import java.util.*;
import java.sql.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;

public class FriendshipRuntimeProof {
    static final Map<UUID, UUID> POSTS = new java.util.concurrent.ConcurrentHashMap<>();
    @Configuration(proxyBeanMethods = false)
    static class FeedProofClients {
        @Bean static org.springframework.beans.factory.config.BeanFactoryPostProcessor proofClientPriority() {
            return factory -> {
                factory.getBeanDefinition("com.minh.fakebook.feed.client.PostServiceClient").setPrimary(false);
                factory.getBeanDefinition("com.minh.fakebook.feed.client.UserServiceClient").setPrimary(false);
            };
        }
        @Bean @Primary com.minh.fakebook.feed.client.PostServiceClient proofPosts() {
            return (author, limit) -> List.of(new com.minh.fakebook.feed.client.dto.FeedPostReferenceDTO(
                POSTS.get(author), author, "FRIENDS", Instant.parse("2026-10-05T00:00:00Z")));
        }
        @Bean @Primary com.minh.fakebook.feed.client.UserServiceClient proofUsers() {
            return org.mockito.Mockito.mock(com.minh.fakebook.feed.client.UserServiceClient.class);
        }
    }
    public static void main(String[] ignored) throws Exception {
        try (var kafka = new KafkaContainer("apache/kafka-native:4.3.1").withStartupAttempts(3);
             var redis = new GenericContainer<>("redis:8.10.1").withExposedPorts(6379);
             var database = new MariaDBContainer<>("mariadb:12.3.3").withDatabaseName("friendship_proof")) {
            kafka.start(); redis.start(); database.start();
            try (var connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE feed_items (id varchar(36) PRIMARY KEY, user_id varchar(36) NOT NULL, post_id varchar(36) NOT NULL, author_id varchar(36), visibility varchar(20), created_at datetime(6) NOT NULL, UNIQUE KEY ux_feed_items_user_id_post_id (user_id,post_id), KEY idx_feed_items_user_created_post (user_id,created_at DESC,post_id DESC))");
            }
            String[] shared = {"--spring.docker.compose.enabled=false", "--spring.cloud.bootstrap.enabled=false", "--server.port=0", "--spring.cloud.discovery.enabled=false", "--spring.cloud.consul.enabled=false",
                "--spring.cloud.service-registry.auto-registration.enabled=false", "--spring.main.allow-bean-definition-overriding=true",
                "--spring.liquibase.enabled=false", "--logging.level.root=WARN", "--spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                "--spring.cloud.stream.kafka.binder.brokers=" + kafka.getBootstrapServers(), "--spring.profiles.active=test,kafka",
                "--spring.datasource.hikari.maximum-pool-size=4"};
            var feedArgs = new ArrayList<String>(List.of(shared));
            feedArgs.addAll(List.of("--spring.config.location=file:feedService/src/test/resources/config/application.yml,file:feedService/src/main/resources/config/application-kafka.yml",
                "--spring.datasource.url=" + database.getJdbcUrl(), "--spring.datasource.username=" + database.getUsername(),
                "--spring.datasource.password=" + database.getPassword(), "--spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
                "--spring.jpa.hibernate.ddl-auto=none", "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
                "--namastack.outbox.enabled=false", "--spring.cloud.function.definition=processFriendshipEvent"));
            try (ConfigurableApplicationContext feed = new SpringApplicationBuilder(com.minh.fakebook.feed.FeedServiceApp.class,
                    com.minh.fakebook.feed.config.TestSecurityConfiguration.class, FeedProofClients.class).run(feedArgs.toArray(String[]::new))) {
                System.out.println("PROOF: Feed context bound to isolated Kafka and MariaDB");
                var userArgs = new ArrayList<String>(List.of(shared));
                userArgs.addAll(List.of("--spring.config.location=file:userService/src/test/resources/config/application.yml,file:userService/src/main/resources/config/application-kafka.yml",
                    "--spring.datasource.url=jdbc:h2:mem:friendship_proof;DB_CLOSE_DELAY=-1", "--spring.datasource.driver-class-name=org.h2.Driver",
                    "--spring.datasource.username=sa", "--spring.datasource.password=", "--spring.jpa.hibernate.ddl-auto=create-drop",
                    "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                    "--spring.data.redis.url=redis://" + redis.getHost() + ":" + redis.getMappedPort(6379),
                    "--namastack.outbox.enabled=true", "--namastack.outbox.kafka.enabled=true",
                    "--namastack.outbox.polling-interval=200", "--namastack.outbox.processing.delete-completed-records=false",
                    "--namastack.outbox.multicaster.enabled=true", "--namastack.outbox.multicaster.publish-after-save=true",
                    "--namastack.outbox.kafka.default-topic=friendship-events"));
                try (ConfigurableApplicationContext user = new SpringApplicationBuilder(com.minh.fakebook.user.UserServiceApp.class,
                        com.minh.fakebook.user.config.TestSecurityConfiguration.class).run(userArgs.toArray(String[]::new))) {
                    System.out.println("PROOF: User context bound to isolated Kafka, H2 and Redis");
                    JdbcTemplate userDb = user.getBean(JdbcTemplate.class);
                    userDb.execute("ALTER TABLE outbox_record ALTER COLUMN payload CLOB");
                    userDb.execute("ALTER TABLE outbox_record ALTER COLUMN context CLOB");
                    var profiles = user.getBean(com.minh.fakebook.user.repository.UserProfileRepository.class);
                    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
                    for (UUID id : List.of(a,b)) profiles.saveAndFlush(new com.minh.fakebook.user.domain.UserProfile()
                        .id(id).username("proof-" + id).displayName("Proof").createdAt(Instant.now()));
                    POSTS.put(a, UUID.randomUUID()); POSTS.put(b, UUID.randomUUID());
                    JdbcTemplate feedDb = feed.getBean(JdbcTemplate.class);
                    feedDb.update("INSERT INTO feed_items VALUES (?,?,?,?,?,?)", UUID.randomUUID().toString(), a.toString(), UUID.randomUUID().toString(), b.toString(), "PUBLIC", Timestamp.from(Instant.now()));
                    var requests = user.getBean(com.minh.fakebook.user.service.FriendRequestService.class);
                    var friendships = user.getBean(com.minh.fakebook.user.service.FriendshipService.class);
                    var request = requests.sendFriendRequest(a,b);
                    requests.acceptFriendRequest(request.getId(),b);
                    try { awaitCount(feedDb, 2); } catch (AssertionError failure) {
                        System.out.println(userDb.queryForList("SELECT record_type, status, failure_reason FROM outbox_record"));
                        throw failure;
                    }
                    if (!friendships.areFriends(a,b)) throw new AssertionError("User friendship was not committed");
                    long created = userDb.queryForObject("SELECT COUNT(*) FROM outbox_record WHERE payload LIKE '%FRIENDSHIP_CREATED%'", Long.class);
                    System.out.println("PROOF: accept -> friendship-events -> Feed FRIENDS rows=2; created outbox records=" + created);
                    if (created != 1) throw new AssertionError("Friendship was scheduled more than once: " + created);
                    friendships.unFriend(a,b);
                    awaitCount(feedDb, 0);
                    if (friendships.areFriends(a,b)) throw new AssertionError("User friendship was not removed");
                    long deleted = userDb.queryForObject("SELECT COUNT(*) FROM outbox_record WHERE payload LIKE '%FRIENDSHIP_DELETED%'", Long.class);
                    if (deleted != 1) throw new AssertionError("Unfriend was scheduled more than once: " + deleted);
                    if (feedDb.queryForObject("SELECT COUNT(*) FROM feed_items WHERE visibility='PUBLIC'", Long.class) != 1) throw new AssertionError("Unfriend removed PUBLIC projection");
                    System.out.println("PROOF PASS: User accept/unfriend -> transactional outbox -> real Kafka -> real Feed consumer -> MariaDB; PUBLIC row preserved. Post metadata is a stub.");
                }
            }
        }
    }
    static void awaitCount(JdbcTemplate db, long expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        while (System.nanoTime() < deadline) {
            if (db.queryForObject("SELECT COUNT(*) FROM feed_items WHERE visibility='FRIENDS'", Long.class) == expected) return;
            Thread.sleep(200);
        }
        throw new AssertionError("Expected FRIENDS projection count " + expected + " within 45 seconds");
    }
}
