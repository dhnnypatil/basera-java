package com.basera.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A person who can log in (models/user.js).
 *
 * How this maps to Mongoose:
 *   - @Document("users") = mongoose.model("User", ...) - each User object is one document in the "users" collection.
 *   - Each field is a schema path. The Java type does the work of `type: String`, `type: Number`, ...
 *   - @Id = the _id field. MongoDB generates it on the first save.
 *   - @Indexed(unique = true) = `unique: true`.
 *   - @CreatedDate / @LastModifiedDate = `{ timestamps: true }` (switched on by @EnableMongoAuditing in MongoConfig).
 *
 * passport-local-mongoose quietly added `username`, `hash` and `salt` for us. Here they're written out:
 * `password` holds a BCrypt hash (Spring Security creates it - we never store the real password).
 */
@Document("users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email; // always stored lowercase (the auth service does that at signup)

    private String password;

    private Role role = Role.CUSTOMER;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Spring Data needs a no-argument constructor to rebuild a User from a database document. */
    public User() {
    }

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // userSchema.methods.isHost / isCustomer
    public boolean isHost() {
        return role == Role.HOST;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    // ----- Getters and setters -----
    // Java convention: fields are private and read/written through these methods. Spring Data, Thymeleaf
    // (${user.username} calls getUsername()) and JSON conversion all rely on them. Your IDE can generate them.

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
