package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.service.dto.LoginRequest;
import at.fhtw.swen3.paperless.service.dto.LoginResponse;
import at.fhtw.swen3.paperless.service.dto.RegisterRequest;
import at.fhtw.swen3.paperless.service.dto.UserDto;

public interface AuthService {

    UserDto register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String token);

    UserDto getUser(Long id);
}
