package com.music.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "推荐请求参数")
public class RecommendationQueryDTO {

    @Schema(description = "推荐数量")
    @Min(1)
    @Max(50)
    private Integer limit = 10;
}
