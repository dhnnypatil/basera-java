package com.basera.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.basera.model.Review;

/** Database access for reviews. */
public interface ReviewRepository extends MongoRepository<Review, String> {
}
