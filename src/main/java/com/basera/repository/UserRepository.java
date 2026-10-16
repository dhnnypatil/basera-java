package com.basera.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.basera.model.User;

/**
 * Database access for users.
 *
 * This is only an interface - we never write the class that implements it. Spring Data generates it when the
 * app starts. Extending MongoRepository<User, String> (entity type, id type) gives us save(), findById(),
 * findAll(), deleteById(), count() ... - the same things Mongoose models give you for free.
 *
 * Extra queries are written as method names, and Spring Data reads the name to build the query:
 *   findByUsername(name)  ->  db.users.findOne({ username: name })
 * Optional<User> means "a User, or nothing" - Java's safer alternative to returning null.
 */
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
