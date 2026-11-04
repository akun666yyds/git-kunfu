package com.gitkunfu.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "请填写用户名")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{2,38}$",
                    message = "用户名需为 3-39 位字母、数字、下划线或短横线，并以字母或数字开头")
            String username,
            @NotBlank(message = "请填写邮箱")
            @Email(message = "邮箱格式不正确")
            @Size(max = 190, message = "邮箱过长")
            String email,
            @NotBlank(message = "请填写密码")
            @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
            String password,
            @Size(max = 80, message = "昵称过长")
            String displayName
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "请填写用户名或邮箱") String login,
            @NotBlank(message = "请填写密码") String password
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "昵称不能为空")
            @Size(max = 80, message = "昵称最多 80 个字符") String displayName,
            @Size(max = 255, message = "简介最多 255 个字符") String bio
    ) {
    }

    public record AuthResponse(String token, UserView user) {
    }

    public record ProfileResponse(UserView user, long repositoryCount) {
    }
}
