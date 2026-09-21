package com.sistek.sos.analysis_dashboard.controllers;

import com.sistek.sos.analysis_dashboard.dto.PageQuery;
import com.sistek.sos.analysis_dashboard.dto.UserForm;
import com.sistek.sos.analysis_dashboard.dto.UserRow;
import com.sistek.sos.analysis_dashboard.exceptions.UserBusinessException;
import com.sistek.sos.analysis_dashboard.exceptions.UserValidationException;
import com.sistek.sos.analysis_dashboard.services.UserAdminService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Set;

/**
 * Kullanıcı yönetimi ekranları denetleyicisi (Yalnızca ADMIN).
 * Formları bağlar ve sonuçları çizer; iş kuralları UserAdminService içindedir.
 */
@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    /**
     * Tüm form görünümleri için izin verilen rolleri modele otomatik sağlar.
     */
    @ModelAttribute("availableRoles")
    public Set<String> availableRoles() {
        return UserAdminService.ALLOWED_ROLES;
    }

    /**
     * Kullanıcıları listeler (sayfalanmış).
     */
    @GetMapping
    public String listUsers(@ModelAttribute("pageQuery") PageQuery pageQuery, Model model) {
        Page<UserRow> users = userAdminService.getUsers(pageQuery);
        model.addAttribute("users", users);
        return "admin/users";
    }

    /**
     * Yeni kullanıcı oluşturma formunu gösterir.
     */
    @GetMapping("/new")
    public String newUserForm(Model model) {
        return renderForm(model, new UserForm("", "", Set.of("USER"), true), null, false);
    }

    /**
     * Yeni kullanıcı oluşturur (POST-Redirect-GET).
     */
    @PostMapping
    public String createUser(@ModelAttribute("form") UserForm form,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            userAdminService.createUser(form);
            redirectAttributes.addFlashAttribute("successMessage", "'" + form.username() + "' kullanıcısı başarıyla oluşturuldu.");
            return "redirect:/admin/users";
        } catch (UserValidationException e) {
            return renderForm(model, form, e.getFieldErrors(), false);
        }
    }

    /**
     * Kullanıcı düzenleme formunu gösterir.
     */
    @GetMapping("/{username}/edit")
    public String editUserForm(@PathVariable String username, Model model) {
        return renderForm(model, userAdminService.getUserForEdit(username), null, true);
    }

    /**
     * Kullanıcıyı günceller (POST-Redirect-GET).
     */
    @PostMapping("/{username}")
    public String updateUser(@PathVariable String username,
                             @ModelAttribute("form") UserForm form,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            userAdminService.updateUser(username, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "'" + username + "' kullanıcısı başarıyla güncellendi.");
            return "redirect:/admin/users";
        } catch (UserValidationException e) {
            return renderForm(model, form, e.getFieldErrors(), true);
        } catch (UserBusinessException e) {
            return renderForm(model, form, Map.of("general", e.getMessage()), true);
        }
    }

    /**
     * Kullanıcıyı siler (POST-Redirect-GET).
     */
    @PostMapping("/{username}/delete")
    public String deleteUser(@PathVariable String username,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            userAdminService.deleteUser(username, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "'" + username + "' kullanıcısı başarıyla silindi.");
        } catch (UserBusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private String renderForm(Model model, UserForm form, Map<String, String> errors, boolean isEdit) {
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        model.addAttribute("isEdit", isEdit);
        return "admin/user-form";
    }
}
