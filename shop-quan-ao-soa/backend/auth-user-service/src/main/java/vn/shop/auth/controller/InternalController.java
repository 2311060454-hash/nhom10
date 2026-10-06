package vn.shop.auth.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import vn.shop.auth.service.AuthService;

@RestController @RequestMapping("/internal")
public class InternalController {
    private final AuthService auth;
    public InternalController(AuthService auth) { this.auth=auth; }
    @GetMapping("/sessions/{id}") public Map<String,Boolean> session(@PathVariable String id,@RequestParam long userId) { return Map.of("valid",auth.validSession(id,userId)); }
}
