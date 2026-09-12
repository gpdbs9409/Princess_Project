package com.example.princessproject.commontask.repository;

import com.example.princessproject.commontask.model.ReadingBook;
import com.example.princessproject.commontask.model.ReadingBookStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingBookRepository extends JpaRepository<ReadingBook, Long> {

    Optional<ReadingBook> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, ReadingBookStatus status);

    List<ReadingBook> findByUserIdOrderByIdDesc(Long userId);

    Optional<ReadingBook> findFirstByUserIdOrderByIdAsc(Long userId);
}
