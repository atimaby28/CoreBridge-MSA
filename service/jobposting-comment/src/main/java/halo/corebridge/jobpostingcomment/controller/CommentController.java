package halo.corebridge.comment.controller;

import halo.corebridge.comment.model.dto.CommentDto;
import halo.corebridge.comment.service.CommentService;
import halo.corebridge.common.web.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.JOB_COMMENTS)
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public CommentDto.CommentResponse read(
            @PathVariable("commentId") Long commentId
    ) {
        return commentService.read(commentId);
    }

    @PostMapping
    public CommentDto.CommentResponse create(@RequestBody CommentDto.CommentCreateRequest request) {
        return commentService.create(request);
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable("commentId") Long commentId) {
        commentService.delete(commentId);
    }

    @GetMapping
    public CommentDto.CommentPageResponse readAll(
            @RequestParam("jobpostingId") Long jobpostingId,
            @RequestParam("page") Long page,
            @RequestParam("pageSize") Long pageSize
    ) {
        return commentService.readAll(jobpostingId, page, pageSize);
    }

}
