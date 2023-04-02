package com.smartcontactmanager.controller;

import java.io.File;
import java.security.Principal;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smartcontactmanager.dao.ContactRepository;
import com.smartcontactmanager.dao.UserRepository;
import com.smartcontactmanager.entities.Contact;
import com.smartcontactmanager.entities.User;
import com.smartcontactmanager.helper.Message;
import com.smartcontactmanager.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ContactRepository contactRepository;

	@Autowired
	UserService userService;

	@ModelAttribute
	public void addCommonData(Model model, Principal principal, @ModelAttribute("contact") Contact contact) {

		String username = principal.getName();
		System.out.println(username);

		// getting user data from database
		User user = userRepository.loadUserByUsername(username);
		System.out.println(user);

		model.addAttribute("user", user);
	}

	// dashboard home
	@RequestMapping("/dashboard")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "Smart Contact Manager - User Dashboard");
		model.addAttribute("contact", new Contact());

		return "user/user_dashboard";

	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Smart Contact Manager - Add Contact");
		model.addAttribute("contact", new Contact());
		return "user/user_addcontactform";
	}

	// adding user data into database
	@PostMapping("/process-contact")
	public String processdata(@Valid @ModelAttribute("contact") Contact contact, BindingResult result,
			@RequestParam("imageurl") MultipartFile file, Principal principal, HttpSession httpSession, Model model) {

		try {

			/** Check if there are errors while validating **/
			if (result.hasErrors()) {
				System.err.println("There is some error");
				System.out.println(result);
				// return "user/user_addcontactform";
			}

			/** get logged in user data **/
			String username = principal.getName();
			User user = userRepository.loadUserByUsername(username);
			System.out.println(user);

			/** upload Image into database **/
			boolean status = userService.uploadImage(contact, file);
			if (status == false) {
				httpSession.setAttribute("message", new Message("Something went wrong.Try Again!", "danger"));
			}

			/**
			 * 1.map loggedin user with the contact 2.add contact details 3.save contact to
			 * the database
			 **/
			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);

			System.out.println("DATA " + contact);
			System.out.println("Adding to database");

			httpSession.setAttribute("message", new Message("Contact Added!!! Add more...", "success"));

		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();
			httpSession.setAttribute("message", new Message("Something went wrong.Try Again!", "danger"));

		}

		return "user/user_addcontactform";
	}

	@GetMapping("/view-contact/{page}")
	public String viewContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Smart Contact Manager - View Contacts");

		/** get logged in user data **/
		String username = principal.getName();
		User user = userRepository.loadUserByUsername(username);

		/** set number of contact to be viewed per page **/
		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contactlist = this.contactRepository.getContactsByUser(user.getId(), pageable);

		model.addAttribute("contacts", contactlist);
		model.addAttribute("currentpage", page); // current page always starts with 0
		model.addAttribute("totalpages", contactlist.getTotalPages()); // get total number of pages depending upon
																		// contact per page

		return "user/user_viewcontact";
	}

	/** Showing particular contact details **/
	@RequestMapping("/{cId}/contact/")
	public String showContactDetail(@PathVariable("cId") int cId, Model model, Principal principal) {

		/** Set Title of the page **/
		model.addAttribute("title", "Smart Contact Manager - View Contact Data");

		/** get logged in user data **/
		String username = principal.getName();
		User user = this.userRepository.loadUserByUsername(username);

		/** get contact data **/
		Contact contact = this.userService.getcontactbyId(cId);

		/**
		 * check whether loggedin user and the user mapped with the contact is same - >
		 * if same show the contact data
		 **/
		if (contact.getUser() == user) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "user/user_contactdetail";
	}

	/** delete contact handler **/
	@GetMapping("/delete-contact/{cId}")
	public String deletecontact(@PathVariable("cId") Integer cId, Model model, Principal principal,
			@ModelAttribute("user") User user) {
		try {

			/** get logged in user data **/
			String username = principal.getName();
			User currentUser = this.userRepository.loadUserByUsername(username);

			/** get contact data **/
			Contact contact = userService.getcontactbyId(cId);

			/** compare both userID for avoiding url miss-leading purpose */
			if (currentUser.getId() == contact.getUser().getId()) {

				/** Remove contact from given user List */

				/** It is not deleted directly because its is mapped with user */
				user.getContacts().remove(contact);

				// contact.setUser(null);
				// this.contacRepository.delete(contact);

				// Now we must delete photo from folder
				File saveFile = new ClassPathResource("/static/image").getFile();
				if (!contact.getImage().equals("contact.png")) {
					File deleteFile = new File(saveFile, contact.getImage());
					deleteFile.delete();
					System.out.println(contact.getcId() + "ID Contact deleted successfully ");
				}
				this.contactRepository.delete(contact);

			} else {
				model.addAttribute("message", new Message("You are not an authorized user for this contact", "denger"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/view-contact/0";
	}

	// open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {

		/** Set Title of the page **/
		model.addAttribute("title", "Smart Contact Manager - Update Contact");

		/** get contact data **/
		Contact contact = userService.getcontactbyId(cId);

		model.addAttribute("contact", contact);
		return "user/user_update_form";
	}

	// update contact
	@PostMapping("/process-update")
	public String processupdate(@ModelAttribute("contact") Contact contact,
			@RequestParam("imageurl") MultipartFile file, Model model, HttpSession httpSession, Principal principal) {
		System.out.println("Idhar hu main : " + contact.getcId());
		try {

			boolean status = userService.updateImage(contact, file);
			if (status == false) {
				throw new Exception();
			}

			/** get logged in user data and map user with contact **/
			User user = this.userRepository.loadUserByUsername(principal.getName());
			contact.setUser(user);

			/** save user data **/
			this.contactRepository.save(contact);

			httpSession.setAttribute("message", new Message("Your contact is updated sucessfully..", "success"));

		} catch (Exception e) {
			httpSession.setAttribute("message", new Message("Something went wrong!!!", "danger"));
		}

		return "redirect:/user/" + contact.getcId() + "/contact/";
	}

	@GetMapping("/userprofile")
	public String viewuserprofile(Model model, @ModelAttribute("user") User user) {

		model.addAttribute("user", user);
		int totalnoofcontacts = user.getContacts().size();
		System.out.println(totalnoofcontacts);
		model.addAttribute("totalcontacts", totalnoofcontacts);

		model.addAttribute("title", "Smart Contact Manager - User Profile");
		return "user/user_profile";
	}

	// handler to view settings page
	@RequestMapping("/user-settings/")
	public String usersettings(Model model) {
		model.addAttribute("title", "Smart Contact Manager - Settings");
		return "user/user_settings";
	}

	// handler to change password
	@PostMapping("/change-password/")
	public String changepassword(@RequestParam("oldPassword") String oldPassword,Model model,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession httpSession) {

		model.addAttribute("model", "Smart Contact Manager - Settings");
		/** get logged in user data and map user with contact **/
		User user = this.userRepository.loadUserByUsername(principal.getName());
		String password = user.getPassword();

		/**
		 * 1.check if password stored in db and old password entered by the user
		 * matches. if yess --> set new password if no --> throw exception
		 **/
		if (bCryptPasswordEncoder.matches(oldPassword, password)) {
			user.setPassword(bCryptPasswordEncoder.encode(newPassword));
			System.out.println(bCryptPasswordEncoder.encode(newPassword)); //
			bCryptPasswordEncoder.encode(newPassword);
			this.userRepository.save(user);

			httpSession.setAttribute("message", new Message("Password Updated Sucessfully", "success"));
		} else {
			httpSession.setAttribute("message", new Message("Password Mismatch", "danger"));
		}

		return "user/user_settings";
	}
}
