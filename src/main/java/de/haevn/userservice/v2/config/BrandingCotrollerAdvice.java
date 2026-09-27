package de.haevn.userservice.v2.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class BrandingCotrollerAdvice {
    @Value("${app.branding.company-name}")
    private String companyName;

    @Value("${app.branding.slogan}")
    private String slogan;

    /**
     * Diese Methode wird vor jedem Controller-Aufruf ausgeführt.
     * Die hier gesetzten Attribute stehen automatisch in JEDEM Thymeleaf-Template bereit.
     */
    @ModelAttribute
    public void addGlobalBrandingAttributes(Model model) {
        model.addAttribute("companyName", companyName);
        model.addAttribute("slogan", slogan);
    }
}
