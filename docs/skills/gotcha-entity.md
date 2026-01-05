# GOTCHA Entity 개발 스킬

## 필수 문서 확인
Entity 작업 전 반드시 읽을 것:
- `docs/entity-design.md` - Entity 구조 정의
- `docs/coding-patterns.md` - Builder 패턴 등

## Entity 기본 구조

```java
@Entity
@Table(name = "shops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shop extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    public Shop(String name, String address, Boolean isActive) {
        this.name = name;
        this.address = address;
        this.isActive = isActive != null ? isActive : true;
    }

    // 비즈니스 메서드
    public void updateName(String name) {
        this.name = name;
    }

    public void close() {
        this.isActive = false;
    }
}
```

## 필수 규칙

### 1. BaseTimeEntity 상속
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 2. 접근 제어
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수
- `@Setter` 사용 금지
- 상태 변경은 비즈니스 메서드로만

### 3. Builder 패턴
- 생성자에 `@Builder` 적용
- Null 안전 기본값 설정

```java
@Builder
public Shop(String name, Boolean isActive) {
    this.name = name;
    this.isActive = isActive != null ? isActive : true;  // 기본값
}
```

### 4. 연관관계 설정

```java
// ManyToOne (N:1)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;

// OneToMany (1:N)
@OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Comment> comments = new ArrayList<>();
```

### 5. Enum 타입

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private SocialType socialType;
```

### 6. JSON 필드 (PostgreSQL)

```java
@Column(columnDefinition = "jsonb")
@JdbcTypeCode(SqlTypes.JSON)
private Map<String, String> openTime;
```

## 컬럼 명명 규칙

| Java | DB Column |
|------|-----------|
| camelCase | snake_case |
| isActive | is_active |
| createdAt | created_at |

## 테이블 명명 규칙

| Entity | Table |
|--------|-------|
| User | users |
| Shop | shops |
| ShopReport | shop_reports |

## 완료 체크리스트
- [ ] BaseTimeEntity 상속
- [ ] @NoArgsConstructor(access = PROTECTED)
- [ ] @Builder 생성자
- [ ] Null 안전 기본값 처리
- [ ] 비즈니스 메서드 작성 (Setter 대신)
- [ ] 연관관계 LAZY 로딩
- [ ] docs/entity-design.md 업데이트
