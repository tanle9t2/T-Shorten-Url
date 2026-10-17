---
trigger: always_on
---

# Code Comment Rules

## General Principle

Comments should explain **why**, not **what**.

Good code should be self-explanatory.

Do not add comments for obvious code behavior.

---

# Comment Language Rules

All source code comments must be written in English.

Allowed:

- English comments only.
- Technical terms commonly used in engineering are allowed.

Forbidden:

- Vietnamese comments.
- Mixed language comments.
- Informal comments.
- Personal notes.

Bad:

```java
// Tạo user mới
User user = new User();
```

Bad:

```java
// Kiểm tra user có tồn tại không
validateUser();
```

Good:

```java
// Validate user existence before processing payment.
validateUser();
```

---

# Forbidden Comments

Never write comments that simply describe the code.

Bad examples:

```java
// Create user
User user = new User();

// Set username
user.setUsername(username);

// Loop through users
for(User user : users) {

}

// Return response
return response;
```

The code already explains these actions.

---

# When To Add Comments

Add comments only when explaining:

* Business rules
* Complex algorithms
* Non-obvious decisions
* Workarounds
* External constraints
* Performance considerations
* Security decisions

---

# Good Comments

Example:

```java
// We intentionally use READ_COMMITTED isolation
// because concurrent updates may occur and dirty reads are unacceptable.
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateOrder() {
}
```

---

Example:

```java
// Redis TTL is shorter than business expiration
// to prevent stale URL mappings after deletion.
redisTemplate.expire(key, Duration.ofHours(1));
```

---

# Avoid Commenting Bad Code

Do not add comments to explain confusing code.

Bad:

```java
// Check if user is active
if(a == 1) {
}
```

Fix the code instead:

```java
if(user.isActive()) {
}
```

---

# TODO Comments

Allowed only when linked to a real task.

Good:

```java
// TODO: Replace polling with Kafka consumer after migration is completed.
```

Bad:

```java
// TODO: Fix this later.
```

---

# Method Comments

Do not add comments for normal methods.

Bad:

```java
/**
 * Get user by id
 */
public User getUser(Long id)
```

The method name already explains the purpose.

---

# Documentation

Use:

* README
* ADR
* API Documentation
* Architecture Documentation

for large explanations.

Do not put large documentation inside source code.

---

# AI Requirements

When generating code:

* Write comments only in English.
* Do not create unnecessary comments.
* Prefer meaningful names over comments.
* Refactor unclear code instead of explaining it.
* Add comments only when the reason cannot be expressed by code.
* Remove outdated comments when modifying code.