package com.pomotodo.repository;

import com.pomotodo.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    
    // 查找团队成员
    List<TeamMember> findByTeamIdAndIsActiveTrue(Long teamId);
    
    // 查找用户的团队成员记录
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);
    
    // 检查用户是否是团队成员
    boolean existsByTeamIdAndUserIdAndIsActiveTrue(Long teamId, Long userId);
    
    // 查找用户加入的所有团队
    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true")
    List<TeamMember> findAllByUserId(@Param("userId") Long userId);
    
    // 统计团队成员数量
    long countByTeamIdAndIsActiveTrue(Long teamId);
    
    // 移除成员
    @Modifying
    @Transactional
    @Query("UPDATE TeamMember tm SET tm.isActive = false WHERE tm.team.id = :teamId AND tm.user.id = :userId")
    void deactivateMember(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    // 更新成员角色
    @Modifying
    @Transactional
    @Query("UPDATE TeamMember tm SET tm.role = :role WHERE tm.team.id = :teamId AND tm.user.id = :userId")
    void updateRole(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("role") TeamMember.Role role);
}
