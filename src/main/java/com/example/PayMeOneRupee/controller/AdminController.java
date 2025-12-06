package com.example.PayMeOneRupee.controller;

import com.example.PayMeOneRupee.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/login")
    public String login() {
        return "admin-login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalMembers = transactionRepository.countByStatus("captured");
        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("transactions", transactionRepository.findAllByOrderByTimestampDesc());
        return "admin-dashboard";
    }
}
