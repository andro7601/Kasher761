package com.web.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    public User() {}

    private User(Builder b) {
        this.username = b.username;
        this.email    = b.email;
        this.password = b.password;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String username, email, password;
        public Builder username(String v) { this.username = v; return this; }
        public Builder email(String v)    { this.email    = v; return this; }
        public Builder password(String v) { this.password = v; return this; }
        public User build()               { return new User(this); }
    }

    public Long getId()       { return id; }
    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
}
