package com.tracker.backend.service;

import com.tracker.backend.dto.CommentResponse;
import com.tracker.backend.dto.CreateCommentRequest;
import com.tracker.backend.entity.Comment;
import com.tracker.backend.entity.Task;
import com.tracker.backend.repository.CommentRepository;
import com.tracker.backend.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// * Unit tests for CommentService verifying comment creation, lookup, and deletion.
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CommentService commentService;

    private Task testTask;
    private Comment testComment;
    private CreateCommentRequest request;

    @BeforeEach
    void setUp() {
        testTask = new Task();
        testTask.setId(10L);
        testTask.setTitle("Implement WebSocket");

        testComment = new Comment();
        testComment.setId(50L);
        testComment.setText("Looks great!");
        testComment.setAuthorId(1L);
        testComment.setTask(testTask);

        request = new CreateCommentRequest();
        request.setText("Looks great!");
        request.setTaskId(10L);
        request.setAuthorId(1L);
    }

    @Test
    void createComment_Success() {
        // 1. ARRANGE
        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // 2. ACT
        CommentResponse response = commentService.createComment(request);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("Looks great!", response.getText());
        assertEquals(10L, response.getTaskId());
    }

    @Test
    void createComment_TaskNotFound_ThrowsException() {
        // 1. ARRANGE
        when(taskRepository.findById(10L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(EntityNotFoundException.class, () -> commentService.createComment(request));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getCommentsByTask_ReturnsList() {
        // 1. ARRANGE
        when(commentRepository.findByTaskId(10L)).thenReturn(List.of(testComment));

        // 2. ACT
        List<CommentResponse> comments = commentService.getCommentsByTask(10L);

        // 3. ASSERT
        assertEquals(1, comments.size());
        assertEquals("Looks great!", comments.get(0).getText());
    }

    @Test
    void deleteComment_Success() {
        // 1. ARRANGE
        when(commentRepository.findById(50L)).thenReturn(Optional.of(testComment));

        // 2. ACT
        commentService.deleteComment(50L);

        // 3. ASSERT
        verify(commentRepository, times(1)).delete(testComment);
    }
}
