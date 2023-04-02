package com.smartcontactmanager.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcontactmanager.entities.Contact;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

	@Query("from Contact as c WHERE c.user.id= :userId")
	public Page<Contact> getContactsByUser(@Param("userId") int userId, Pageable pageable);
}
