package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.repository.ContactMessageRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminContactController {

    private final ContactMessageRepository contactRepo;

    public AdminContactController(ContactMessageRepository contactRepo) {
        this.contactRepo = contactRepo;
    }

    @GetMapping("/admin/contact-messages")
    public String listMessages(Model model) {
        model.addAttribute("messages", contactRepo.findAll());
        return "admin-contact-messages";
    }

    @PostMapping("/admin/contact-messages/delete")
    public String deleteMessage(@RequestParam Long id, RedirectAttributes ra) {
        try {
            contactRepo.deleteById(id);
            ra.addFlashAttribute("success", "Message deleted");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to delete message: " + ex.getMessage());
        }
        return "redirect:/admin/contact-messages";
    }
}
