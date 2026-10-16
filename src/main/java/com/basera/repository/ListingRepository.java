package com.basera.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.basera.model.Listing;

/** Database access for listings. Search/filter queries arrive with the listings phase (ListingService). */
public interface ListingRepository extends MongoRepository<Listing, String> {
}
