package com.matteopaciolla.prbe.controller.pages;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.UserDto;
import com.matteopaciolla.prbe.service.UserService;
import com.matteopaciolla.prbe.util.StringManipulationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Slf4j
@Controller("publicEmailConfirmationController")
@RequestMapping(Paths.PUBLIC_PATH)
public class PublicController {

    @Autowired
    private UserService userService;

    @RequestMapping("/confirmEmail")
    public ModelAndView confirmEmail(@RequestParam String token, ModelMap model) {
        log.info("Confirming email with token: {}", token);
        UserDto userDto = userService.confirmEmail(token);
        log.info("Email confirmed successfully");
        ResourceBundle bundle = ResourceBundle.getBundle("labels", Locale.ITALY);
        model.addAttribute("title", bundle.getString("email_confirmed_page.title"));
        String message = bundle.getString("email_confirmed_page.message");
        message = StringManipulationUtils.insertVariables(message, Map.of("username", userDto.getUsername()));
        model.addAttribute("message", message);
        return new ModelAndView("email_confirmed_page");
    }
}
