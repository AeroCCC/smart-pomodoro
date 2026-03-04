package com.pomotodo.repository;

import com.pomotodo.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    // 查找用户拥有的团队
    List<Team> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    
    // 通过邀请码查找团队
    Optional<Team> findByInviteCode(String inviteCode);
    
    // 查找用户参与的所有团队（包括作为成员）
    @Query("SELECT t FROM Team t LEFT JOIN t.members m WHERE (t.owner.id = :userId OR m.user.id = :userId) AND t.isActive = true ORDER BY t.createdAt DESC")
    List<Team> findAllByUserId(@Param("userId") Long userId);
    
    // 检查邀请码是否已存在
    boolean existsByInviteCode(String inviteCode);
    
    // 查找用户拥有的活跃团队
    List<Team> findByOwnerIdAndIsActiveTrueOrderByCreatedAtDesc(Long ownerId);
}
