package halo.corebridge.jobpostingcomment.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.jobpostingcomment.model.dto.CommentDto;
import halo.corebridge.jobpostingcomment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public BaseResponse<CommentDto.CommentResponse> read(
            @PathVariable("commentId") Long commentId
    ) {
        return BaseResponse.success(commentService.read(commentId));
    }

    @PostMapping
    public BaseResponse<CommentDto.CommentResponse> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody CommentDto.CommentCreateRequest request
    ) {
        return BaseResponse.success(commentService.create(request, userId));
    }

    @DeleteMapping("/{commentId}")
    public BaseResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable("commentId") Long commentId
    ) {
        commentService.delete(commentId, userId);
        return BaseResponse.success();
    }

    @GetMapping
    public BaseResponse<CommentDto.CommentPageResponse> readAll(
            @RequestParam("jobpostingId") Long jobpostingId,
            @RequestParam("page") Long page,
            @RequestParam("pageSize") Long pageSize
    ) {
        return BaseResponse.success(commentService.readAll(jobpostingId, page, pageSize));
    }
}
