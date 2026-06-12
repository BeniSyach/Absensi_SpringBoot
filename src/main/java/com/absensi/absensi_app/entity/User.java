package com.absensi.absensi_app.entity;

import com.absensi.absensi_app.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_nip", columnList = "nip"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_opd", columnList = "opd_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nip;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String namaLengkap;

    @Column(length = 20)
    private String telepon;

    @Column(length = 200)
    private String email;

    @Column(name = "foto_profil")
    private String fotoProfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "aktif")
    @Builder.Default
    private Boolean aktif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opd_id", nullable = false)
    private Opd opd;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AbsenMasuk> absenMasukList;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AbsenPulang> absenPulangList;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<WaktuKerja> waktuKerjaList;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // UserDetails implementations
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return aktif; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return aktif; }
}
