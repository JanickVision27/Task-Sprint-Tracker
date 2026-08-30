package com.tracker.backend.service;

import com.tracker.backend.dto.CommentResponse;
import com.tracker.backend.dto.CreateCommentRequest;
import com.tracker.backend.entity.Comment;
import com.tracker.backend.entity.Task;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;

    public CommentService(CommentRepository commentRepository, TaskRepository taskRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
    }

    public CommentResponse createComment(CreateCommentRequest request) {
        Task task = findTask(request.getTaskId());

        Comment comment = new Comment();
        applyRequest(comment, request);
        comment.setTask(task);
        return mapToResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getCommentsByTask(Long taskId) {
        return commentRepository.findByTaskId(taskId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CommentResponse getCommentById(Long id) {
        return mapToResponse(findComment(id));
    }

    public CommentResponse updateComment(Long id, CreateCommentRequest request) {
        Comment comment = findComment(id);
        applyRequest(comment, request);
        comment.setTask(findTask(request.getTaskId()));
        return mapToResponse(commentRepository.save(comment));
    }

    public void deleteComment(Long id) {
        commentRepository.delete(findComment(id));
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
    }

    private void applyRequest(Comment comment, CreateCommentRequest request) {
        comment.setText(request.getText());
        comment.setAuthorId(request.getAuthorId());
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setText(comment.getText());
        response.setTaskId(comment.getTask().getId());
        response.setAuthorId(comment.getAuthorId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }
}
