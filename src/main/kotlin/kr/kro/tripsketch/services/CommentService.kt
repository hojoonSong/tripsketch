package kr.kro.tripsketch.services

import kr.kro.tripsketch.domain.Comment
import kr.kro.tripsketch.dto.CommentDto
import kr.kro.tripsketch.dto.CommentUpdateDto
import kr.kro.tripsketch.repositories.CommentRepository
import org.bson.types.ObjectId // ObjectId를 사용하기 위한 import
import org.springframework.stereotype.Service

@Service
class CommentService(private val commentRepository: CommentRepository, private val jwtService: JwtService,) {

    fun getAllComments(): List<CommentDto> {
        return commentRepository.findAll().map { CommentDto.fromComment(it) }
    }

    fun getCommentByTripId(tripId: String): List<CommentDto> {
        val comments = commentRepository.findAllByTripId(tripId)
        return comments.map { CommentDto.fromComment(it) }
    }

    fun createComment(token: String, dto: CommentDto): Comment {
        val actualToken = token.removePrefix("Bearer ").trim() // "Bearer " 제거

        if (!jwtService.validateToken(actualToken)) { // 토큰 유효성 검증구
            throw IllegalArgumentException("토큰이 유효 하지 않습니다.")
        }
        val userEmail = jwtService.getEmailFromToken(token)
        val parentComment: Comment? = dto.parentId?.let {
            commentRepository.findById(it).orElse(null)
        }

        val comment = Comment(
            userEmail = userEmail,
            tripId = dto.tripId,
            parentId = dto.parentId,
            content = dto.content,
            replyTo = dto.replyTo,
            userNickName = dto.userNickName,    
            userProfileUrl = dto.userProfileUrl,  
        )

        if (parentComment == null) {
            // parentId가 없는 경우: 새로운 댓글을 저장하고 반환
            return commentRepository.save(comment)
        } else {
            // parentId가 있는 경우: 새로운 댓글을 부모의 children 리스트에 추가하고 부모 댓글을 저장
            val childComment = Comment(
                id = ObjectId().toString(), // 새로운 ObjectId 생성
                userEmail = userEmail,
                tripId = comment.tripId,
                parentId = comment.parentId,
                content = comment.content,
                replyTo = comment.replyTo,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                likedBy = comment.likedBy,
                userNickName = comment.userNickName,           
                userProfileUrl = comment.userProfileUrl, 
            )
            parentComment.children.add(childComment)
            commentRepository.save(parentComment)
        }
        return commentRepository.save(parentComment)
    }

    fun updateComment(id: String, commentUpdateDto: CommentUpdateDto): CommentDto {
        val comment = commentRepository.findById(id).orElse(null) ?: throw IllegalArgumentException("해당 id 댓글은 존재하지 않습니다.")

        val updatedComment = comment.copy(
            content = commentUpdateDto.content ?: comment.content,
            updatedAt = commentUpdateDto.updatedAt,
        )

        val savedComment = commentRepository.save(updatedComment)

        return CommentDto.fromComment(savedComment)
    }

    fun updateChildrenComment(parentId: String, id: String, commentUpdateDto: CommentUpdateDto): CommentDto {
        val parentComment = commentRepository.findById(parentId).orElse(null)
            ?: throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")

        val childCommentIndex = parentComment.children.indexOfFirst { it.id == id }
        if (childCommentIndex == -1) {
            throw IllegalArgumentException("해당 id에 대응하는 댓글이 children 존재하지 않습니다.")
        }

        val updatedChildComment = parentComment.children[childCommentIndex].copy(
            content = commentUpdateDto.content ?: parentComment.children[childCommentIndex].content,
            updatedAt = commentUpdateDto.updatedAt,
        )

            parentComment.children[childCommentIndex] = updatedChildComment
            val savedParentComment = commentRepository.save(parentComment)

            return CommentDto.fromComment(savedParentComment)
    }

    fun deleteComment(id: String) {
        val comment = commentRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("해당 id 댓글은 존재하지 않습니다.")

        // Soft delete 처리
        val deletedComment = comment.copy(isDeleted = true)
        commentRepository.save(deletedComment)
    }

    fun deleteChildrenComment(parentId: String, id: String) {
        val parentComment = commentRepository.findById(parentId).orElse(null)
            ?: throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")

        val childCommentIndex = parentComment.children.indexOfFirst { it.id == id }
        if (childCommentIndex == -1) {
            throw IllegalArgumentException("해당 id에 대응하는 댓글이 children에 존재하지 않습니다.")
        }

        // Soft delete 처리
        val deletedChildComment = parentComment.children[childCommentIndex].copy(isDeleted = true)
        parentComment.children[childCommentIndex] = deletedChildComment
        commentRepository.save(parentComment)
    }

    fun toggleLikeComment(token:String, id: String): CommentDto {
        val comment = commentRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("해당 id 댓글은 존재하지 않습니다.")
        val userEmail = jwtService.getEmailFromToken(token)
        if (comment.likedBy.contains(userEmail)) {
            comment.likedBy.remove(userEmail) // 이미 좋아요를 누른 경우 좋아요 취소
        } else {
            comment.likedBy.add(userEmail) // 좋아요 추가
        }

        val savedComment = commentRepository.save(comment)
        return CommentDto.fromComment(savedComment)
    }

    fun toggleLikeChildrenComment(token:String, parentId: String, id: String): CommentDto {
        val parentComment = commentRepository.findById(parentId).orElse(null)
            ?: throw IllegalArgumentException("해당 parentId 댓글은 존재하지 않습니다.")

        val childCommentIndex = parentComment.children.indexOfFirst { it.id == id }
        if (childCommentIndex == -1) {
            throw IllegalArgumentException("해당 id에 대응하는 댓글이 children에 존재하지 않습니다.")
        }

        val childComment = parentComment.children[childCommentIndex]
        val userEmail = jwtService.getEmailFromToken(token)
        if (childComment.likedBy.contains(userEmail)) {
            childComment.likedBy.remove(userEmail) // 이미 좋아요를 누른 경우 좋아요 취소
        } else {
            childComment.likedBy.add(userEmail) // 좋아요 추가
        }

        val savedParentComment = commentRepository.save(parentComment)
        return CommentDto.fromComment(savedParentComment)
    }
}
