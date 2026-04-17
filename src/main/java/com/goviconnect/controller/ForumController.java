package com.goviconnect.controller;

import com.goviconnect.entity.ForumQuestion;
import com.goviconnect.entity.User;
import com.goviconnect.service.ForumService;
import com.goviconnect.service.UserService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final UserService userService;

    @GetMapping
    public String forum(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "sort", defaultValue = "recent") String sort,
                        @RequestParam(value = "filter", required = false) String filter,
                        @RequestParam(value = "prefill", required = false) String prefill,
                        @RequestParam(value = "cropAdvisory", required = false) String cropAdvisory,
                        @RequestParam(value = "cropName", required = false) String cropName,
                        @RequestParam(value = "cropImage", required = false) String cropImage,
                        @RequestParam(value = "season", required = false) String season,
                        @RequestParam(value = "soil", required = false) String soil,
                        @RequestParam(value = "location", required = false) String location,
                        Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Discussion Forum | GOVI CONNECT");
        model.addAttribute("prefill", prefill);
        model.addAttribute("cropAdvisory", cropAdvisory);
        model.addAttribute("cropName", cropName);
        model.addAttribute("cropImage", cropImage);
        model.addAttribute("season", season);
        model.addAttribute("soil", soil);
        model.addAttribute("location", location);
        
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            currentUser = userService.findByUsername(authentication.getName());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("likedAnswerIds", forumService.getLikedAnswerIdsByUser(currentUser));
        }

        List<ForumQuestion> questions;
        if ("my_questions".equals(filter) && currentUser != null) {
            if (search != null && !search.trim().isEmpty()) {
                questions = forumService.searchMyQuestions(currentUser, search);
            } else {
                questions = forumService.getMyActiveQuestions(currentUser);
            }
            model.addAttribute("currentFilter", filter);
        } else {
            if (search != null && !search.trim().isEmpty()) {
                questions = forumService.searchQuestions(search);
            } else {
                questions = forumService.getAllActiveQuestions();
            }
        }

        // Sorting logic (currently questions are mostly sorted by date DESC by default)
        if ("oldest".equals(sort)) {
            questions.sort((q1, q2) -> q1.getCreatedDate().compareTo(q2.getCreatedDate()));
        } else if ("popular".equals(sort)) {
            questions.sort((q1, q2) -> Integer.compare(q2.getAnswers().size(), q1.getAnswers().size()));
        }
        
        model.addAttribute("questions", questions);
        model.addAttribute("searchQuery", search);
        model.addAttribute("currentSort", sort);
        
        return "forum/index";
    }

    @PostMapping("/ask")
    public String askQuestion(@RequestParam("content") String content, 
                              @RequestParam("category") String category,
                              @RequestParam(value = "image", required = false) MultipartFile image,
                              Authentication authentication, 
                              RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByUsername(authentication.getName());
            forumService.askQuestion(user, content, category, image);
            redirectAttributes.addFlashAttribute("successMessage", "Your question has been posted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to post question: " + e.getMessage());
        }
        
        return "redirect:/forum";
    }

    @PostMapping("/ask-crop")
    public String askCropQuestion(@RequestParam("content") String content, 
                                  @RequestParam("category") String category,
                                  @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                  Authentication authentication, 
                                  RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByUsername(authentication.getName());
            forumService.askCropQuestion(user, content, category, imageUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Your question has been posted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to post question: " + e.getMessage());
        }
        
        return "redirect:/forum";
    }

    @PostMapping("/edit/{id}")
    public String editQuestion(@PathVariable("id") Long id,
                               @RequestParam("content") String content, 
                               @RequestParam("category") String category,
                               Authentication authentication, 
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByUsername(authentication.getName());
            forumService.editQuestion(id, user, content, category);
            redirectAttributes.addFlashAttribute("successMessage", "Your question has been updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update question: " + e.getMessage());
        }
        
        return "redirect:/forum";
    }

    @PostMapping("/delete/{id}")
    public String deleteQuestion(@PathVariable("id") Long id,
                                 Authentication authentication, 
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByUsername(authentication.getName());
            forumService.deleteQuestion(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Your question has been deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete question: " + e.getMessage());
        }
        
        return "redirect:/forum";
    }
}
