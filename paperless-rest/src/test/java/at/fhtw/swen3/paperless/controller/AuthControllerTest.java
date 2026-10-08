package at.fhtw.swen3.paperless.controller;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.InvalidCredentialsException;
import at.fhtw.swen3.paperless.security.JwtService;
import at.fhtw.swen3.paperless.security.SecurityConfig;
import at.fhtw.swen3.paperless.service.AuthService;
import at.fhtw.swen3.paperless.service.dto.LoginResponse;
import at.fhtw.swen3.paperless.service.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String REGISTER_BODY =
            "{\"username\":\"jarod\",\"email\":\"jarod@example.com\",\"password\":\"secret123\"}";
    private static final String LOGIN_BODY = "{\"username\":\"jarod\",\"password\":\"secret123\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void register_returns201() throws Exception {
        when(authService.register(any())).thenReturn(new UserDto(1L, "jarod", "jarod@example.com"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("jarod"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_invalidInput_returns400() throws Exception {
        String body = "{\"username\":\" \",\"email\":\"not-an-email\",\"password\":\"short\"}";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void register_takenUsername_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("taken"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returnsToken() throws Exception {
        when(authService.login(any())).thenReturn(
                new LoginResponse("abc.def.ghi", "Bearer", 3600, new UserDto(1L, "jarod", "jarod@example.com")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("abc.def.ghi"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logout_revokesToken() throws Exception {
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isNoContent());
        verify(authService).logout("abc.def.ghi");
    }

    @Test
    void logout_notLoggedIn_returns401() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(username = "7")
    void me_returnsCurrentUser() throws Exception {
        when(authService.getUser(7L)).thenReturn(new UserDto(7L, "jarod", "jarod@example.com"));

        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }
}
