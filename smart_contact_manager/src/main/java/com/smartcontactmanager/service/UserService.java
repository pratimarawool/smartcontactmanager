package com.smartcontactmanager.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartcontactmanager.dao.ContactRepository;
import com.smartcontactmanager.entities.Contact;

@Service
@Transactional
public class UserService {

	@Autowired
	ContactRepository contactRepository;

	// upload Image in database
	public boolean uploadImage(Contact contact, MultipartFile file) {
		try {
			if (file.isEmpty()) {
				// if the file is empty
				System.err.println("Image is empty");
				contact.setImage("contact.png");
				// httpSession.setAttribute("message", new Message("Please select Image",
				// "danger"));

			} else {
				contact.setImage(file.getOriginalFilename());
				File filesave = new ClassPathResource("/static/image").getFile();

				Path path = Paths.get(filesave.getAbsolutePath() + File.separator + file.getOriginalFilename());
				System.err.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image is uploaded");
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public Contact getcontactbyId(int cId) {

		Optional<Contact> optionalcontact = this.contactRepository.findById(cId);
		Contact contact = optionalcontact.get();
		return contact;

	}

	public boolean updateImage(Contact contact, MultipartFile file) {
		try {

			/** get contact data saved in database **/
			Contact oldcontactdetails = getcontactbyId(contact.getcId());
			/** check if file is not empty **/
			if (!file.isEmpty()) {

				// get static path
				File filesave = new ClassPathResource("/static/image").getFile();

				/**
				 * delete old photo from database only if the old photo is not the default photo
				 **/
				if (!oldcontactdetails.getImage().equals("contact.png")) {

					File deleteFile = new File(filesave, oldcontactdetails.getImage());
					deleteFile.delete();
					System.out.println(contact.getcId() + "ID Contact deleted successfully ");

				}

				// upload new photo and save path in database
				Path path = Paths.get(filesave.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				System.out.println("Image is uploaded");

			} else {
				// if not photo is being uploaded by the user restore old photo oin the database
				contact.setImage(oldcontactdetails.getImage());
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
