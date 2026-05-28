package com.startup.domain.ai.repository;

import com.startup.domain.ai.entity.InterrogationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterrogationLogRepository extends JpaRepository<InterrogationLog, Long> {

    List<InterrogationLog> findTop5ByPlaySessionIdAndSuspectIdOrderByCreatedAtDesc(
            Long playSessionId, Long suspectId);

    int countByPlaySessionIdAndSuspectId(Long playSessionId, Long suspectId);

    List<InterrogationLog> findByPlaySessionIdOrderByCreatedAtAsc(Long playSessionId);

    List<InterrogationLog> findByPlaySessionIdAndSuspectIdOrderByCreatedAtAsc(
            Long playSessionId, Long suspectId);

    //세션 전체 심문 횟수(대시보드)
    int countByPlaySessionId(Long playSessionId);

    interface SuspectInterrogationCount {
        Long getSuspectId();
        Long getCount(); // JPQL의 COUNT()는 기본적으로 Long을 반환합니다.
    }
    @Query("SELECT i.suspectId AS suspectId, COUNT(i) AS count " +
            "FROM InterrogationLog i " +
            "WHERE i.playSessionId = :playSessionId " +
            "GROUP BY i.suspectId")
    List<SuspectInterrogationCount> countInterrogationsPerSuspect(@Param("playSessionId") Long playSessionId);
}
