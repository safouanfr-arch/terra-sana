package com.example.demo.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private Boolean actif;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRoleSecurityName()));
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isMembre() {
        return "membre".equalsIgnoreCase(role);
    }

    public String getRoleSecurityName() {
        return isAdmin() ? "ADMIN" : "MEMBRE";
    }
}
