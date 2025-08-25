// repository/IdeaRepository.java
package com.example.An_Yang.repository;

import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
    List<Idea> findByCreatedBy(User user);
}
