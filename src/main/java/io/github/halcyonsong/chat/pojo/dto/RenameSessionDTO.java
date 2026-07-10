package io.github.halcyonsong.chat.pojo.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameSessionDTO {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "title 不能为空")
    @Size(max = 100, message = "title 长度不能超过 100")
    private String title;
}