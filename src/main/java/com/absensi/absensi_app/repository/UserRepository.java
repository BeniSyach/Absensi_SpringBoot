package com.absensi.absensi_app.repository;

import com.absensi.absensi_app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {
            "opd",
            "shift",
            "shift.waktuKerja"
    })
    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByNip(String nip);

    boolean existsByUsername(String username);

    boolean existsByNip(String nip);

    @Query("""
    SELECT u
    FROM User u
    LEFT JOIN FETCH u.opd
    LEFT JOIN FETCH u.shift
    WHERE u.username = :username
    AND u.aktif = true
    """)
    Optional<User> findActiveByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.opd WHERE u.opd.id = :opdId AND u.aktif = :aktif ORDER BY u.namaLengkap")
    org.springframework.data.domain.Page<User> findByOpdIdAndAktif(
            @Param("opdId") Long opdId,
            @Param("aktif") Boolean aktif,
            org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"opd", "shift"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Query("""
    select u
    from User u
    left join fetch u.opd
    left join fetch u.shift
    where u.id = :id
    """)
    Optional<User> findDetailById(@Param("id") Long id);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameOrNip(String username, String nip);

    @Query("""
    SELECT u
    FROM User u
    JOIN FETCH u.opd
    LEFT JOIN FETCH u.shift
    WHERE u.aktif = true
    AND (u.username = :login OR u.nip = :login)
    """)
    Optional<User> findActiveByUsernameOrNip(@Param("login") String login);
}
